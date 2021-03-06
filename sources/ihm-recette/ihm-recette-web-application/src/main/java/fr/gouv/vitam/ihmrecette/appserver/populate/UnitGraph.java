/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/


package fr.gouv.vitam.ihmrecette.appserver.populate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.objectgroup.FileInfoModel;
import fr.gouv.vitam.common.model.objectgroup.FormatIdentificationModel;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import fr.gouv.vitam.common.model.unit.ManagementModel;
import fr.gouv.vitam.common.model.unit.RuleCategoryModel;
import fr.gouv.vitam.common.model.unit.RuleModel;

/**
 * Unit Graph class
 */
public class UnitGraph {


    private final LoadingCache<String, UnitModel> cache;

    /**
     * Constructor
     * 
     * @param metadataRepository metadata repository
     */
    public UnitGraph(MetadataRepository metadataRepository) {

        cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, UnitModel>() {
                @Override
                public UnitModel load(String key) {
                    Optional<UnitModel> unitById = metadataRepository.findUnitById(key);
                    return unitById.orElseThrow(() -> new RuntimeException("rootId not present in database: " + key));
                }
            });
    }

    /**
     * Create a graph
     *
     * @param i used to generate dynamic metadata
     * @param populateModel model of populate service
     * @return new UnitGotModel (unitModel, gotModel)
     */
    public UnitGotModel createGraph(int i, PopulateModel populateModel) {

        final int tenantId = populateModel.getTenant();
        final boolean withGot = populateModel.isWithGots();
        final boolean withLFCUnits = populateModel.isWithLFCUnits();
        final boolean withLFCGots = populateModel.isWithLFCGots();
        // id of the unit.
        String uuid = GUIDFactory.newUnitGUID(tenantId).toString();

        // create unitModel
        UnitModel unitModel = createUnitModel(uuid, DescriptiveMetadataGenerator.generateDescriptiveMetadataModel(i),
            populateModel, i);

        UnitGotModel unitGotModel = new UnitGotModel(unitModel);

        // Create a LogbookLifecycleUnit
        if (withLFCUnits) {
            String id = GUIDFactory.newWriteLogbookGUID(tenantId).toString();
            LogbookLifecycle logbookLifecycle = this.createLogbookLifecycle(id, tenantId,
                unitGotModel.getUnit().getId(), populateModel.getLFCUnitsEventsSize());
            unitGotModel.setLogbookLifecycleUnit(logbookLifecycle);
        }

        if (withGot) {
            // id of the got.
            String guid = GUIDFactory.newObjectGroupGUID(tenantId).toString();

            // update unitModel
            unitModel.setOg(guid);

            // create gotModel
            ObjectGroupModel gotModel = createObjectGroupModel(guid, tenantId,
                DescriptiveMetadataGenerator.generateFileInfoModel(i), unitModel, populateModel.getObjectSize());
            unitGotModel.setGot(gotModel);
            // set object size
            unitGotModel.setObjectSize(populateModel.getObjectSize());

            // Create a LogbookLifeCycleObjectGroup
            if (withLFCGots) {
                String id = GUIDFactory.newWriteLogbookGUID(tenantId).toString();
                LogbookLifecycle logbookLifecycle = this.createLogbookLifecycle(id, tenantId,
                    unitGotModel.getGot().getId(), populateModel.getLFCGotsEventsSize());
                unitGotModel.setLogbookLifeCycleObjectGroup(logbookLifecycle);
            }

        }
        return unitGotModel;
    }

    /**
     * Create a unitModel
     *
     * @param uuid Guid
     * @param descriptiveMetadataModel MetadataModel
     * @param populateModel populate Model
     * @return a UnitModel
     */
    private UnitModel createUnitModel(String uuid, DescriptiveMetadataModel descriptiveMetadataModel,
        PopulateModel populateModel, int unitNumber) {

        final int tenantId = populateModel.getTenant();
        final String rootId = populateModel.getRootId();
        final String originatingAgency = populateModel.getSp();

        // get rootUnit if any
        UnitModel rootUnit = null;
        if (rootId != null) {
            rootUnit = cache.getUnchecked(rootId);
        }

        UnitModel unitModel = new UnitModel();

        unitModel.setId(uuid);
        unitModel.setSp(originatingAgency);
        unitModel.getSps().add(originatingAgency);

        unitModel.getUp().add(rootId);
        unitModel.setTenant(tenantId);
        unitModel.setDescriptiveMetadataModel(descriptiveMetadataModel);

        if (populateModel.isWithRules()) {
            ManagementModel management = new ManagementModel();
            Map<String, Integer> ruleMap = populateModel.getRuleTemplatePercent();
            for (String rule : ruleMap.keySet()) {
                int numberUnitWithRUle = Math.round(ruleMap.get(rule) * populateModel.getNumberOfUnit() / 100);
                if (unitNumber < numberUnitWithRUle) {
                    RuleModel ruleModel = new RuleModel(rule, "2017-01-01");
                    // Set default rule duration 50 years
                    // TODO search rule duration and calculate end date ??
                    ruleModel.setEndDate("2067-01-01");
                    List<RuleModel> rules = new ArrayList<>();
                    rules.add(ruleModel);
                    RuleCategoryModel ruleCategory = new RuleCategoryModel();
                    ruleCategory.setRules(rules);
                    management.setRuleCategoryModel(ruleCategory, MasterdataRepository.getRuleCategoryByRuleId(rule));
                }
            }
            unitModel.setManagementModel(management);
        }

        // setup graph info
        if (rootUnit != null) {
            unitModel.getSps().addAll(rootUnit.getSps());

            unitModel.getUs().addAll(rootUnit.getUs());
            unitModel.getUs().add(rootId);

            // calculate uds
            for (String s : rootUnit.getUds().keySet()) {
                unitModel.getUds().merge(s, rootUnit.getUds().get(s) + 1, Math::min);
            }

            unitModel.getUds().put(rootId, 1);
        }

        return unitModel;
    }

    /**
     * Create a GotModel
     *
     * @param guid GUID
     * @param tenantId tenant identifier
     * @param fileInfoModel fileInfo
     * @param parentUnit unitModel of the parent AU
     * @return a ObjectGroupModel
     */
    private ObjectGroupModel createObjectGroupModel(String guid, int tenantId, FileInfoModel fileInfoModel,
        UnitModel parentUnit, int objectSize) {
        ObjectGroupModel gotModel = new ObjectGroupModel();

        gotModel.setId(guid);
        gotModel.setTenant(tenantId);
        gotModel.getUp().add(parentUnit.getId());
        gotModel.setFileInfoModel(fileInfoModel);
        List<ObjectGroupQualifiersModel> qualifiers = new ArrayList<>();
        if (objectSize != 0) {
            ObjectGroupQualifiersModel qualifier = new ObjectGroupQualifiersModel();
            qualifier.setNbc(1);
            qualifier.setQualifier("BinaryMaster");
            List<ObjectGroupVersionsModel> versions = new ArrayList<>();
            ObjectGroupVersionsModel version = new ObjectGroupVersionsModel();
            String uuid = GUIDFactory.newObjectGUID(tenantId).toString();
            version.setId(uuid);
            version.setAlgorithm("SHA-512");
            version.setMessageDigest(PopulateService.getPopulateFileDigest());
            version.setDataObjectVersion("BinaryMaster_1");
            version.setDataObjectGroupId(guid);
            version.setFileInfoModel(fileInfoModel);
            version.setUri("Content/" + fileInfoModel.getFilename());

            FormatIdentificationModel formatIdentificationModel = new FormatIdentificationModel();
            formatIdentificationModel.setFormatLitteral("Plain Text File");
            formatIdentificationModel.setFormatId("x-fmt/111");
            formatIdentificationModel.setMimeType("text/plain");
            version.setFormatIdentification(formatIdentificationModel);
            version.setSize(objectSize);
            ObjectStorageJson objectStorageJson = new ObjectStorageJson();
            objectStorageJson.setStrategyId("default");
            objectStorageJson.setNbc(StoragePopulateImpl.getNbc());
            objectStorageJson.setOfferIds(StoragePopulateImpl.getOfferIds());

            version.setStorage(objectStorageJson);

            versions.add(version);
            qualifier.setVersions(versions);
            qualifiers.add(qualifier);
        }

        gotModel.setQualifiers(qualifiers);
        gotModel.setSp(parentUnit.getSp());
        gotModel.getSps().addAll(parentUnit.getSps());

        return gotModel;
    }

    /**
     * Create a LogbookLifecycle with the specified number of events
     *
     * @param evId
     * @param tenantId
     * @param obId
     * @param eventsSize the number of events to generate per LogbookLifecycle
     * @return
     */
    private LogbookLifecycle createLogbookLifecycle(String evId, int tenantId, String obId, int eventsSize) {
        LogbookLifecycle logbookLifecycle = new LogbookLifecycle();
        logbookLifecycle.setId(obId);
        logbookLifecycle.setObId(obId);
        logbookLifecycle.setTenant(tenantId);
        logbookLifecycle.setVersion(1);
        logbookLifecycle.setEvIdProc(evId);
        logbookLifecycle.setEvId(evId);
        logbookLifecycle.setEvType("LFC.LFC_CREATION");
        logbookLifecycle.setOutDetail("LFC.LFC_CREATION.OK");
        logbookLifecycle.setOutMessg("Succès");
        logbookLifecycle.setOutcome("OK");
        logbookLifecycle.setEvTypeProc("INGEST");
        logbookLifecycle.setEvDateTime(getDate());

        List<LogbookEvent> events = new ArrayList();
        for (int i = 0; i < eventsSize; i++) {
            LogbookEvent event = new LogbookEvent();
            event.setEvIdProc(evId);
            event.setObId(obId);
            event.setEvId(evId);
            event.setEvType("LFC." + i);
            event.setOutMessg("" + i);
            event.setOutcome("OK");
            event.setEvDetData("");
            event.setEvParentId("");
            event.setEvTypeProc("INGEST");
            event.setEvDateTime(getDate());
            events.add(event);
        }
        logbookLifecycle.setEvents(events);
        return logbookLifecycle;
    }

    private String getDate() {
        return LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
    }

}
