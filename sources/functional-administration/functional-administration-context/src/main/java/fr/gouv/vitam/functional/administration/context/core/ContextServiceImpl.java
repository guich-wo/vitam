/**
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
 */
package fr.gouv.vitam.functional.administration.context.core;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.parser.request.adapter.SimpleVarNameAdapter.change;
import static fr.gouv.vitam.common.database.server.mongodb.VitamDocument.TENANT_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.PermissionModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.Context;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.IngestContract;
import fr.gouv.vitam.functional.administration.common.VitamErrorUtils;
import fr.gouv.vitam.functional.administration.common.counter.SequenceType;
import fr.gouv.vitam.functional.administration.common.counter.VitamCounterService;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import fr.gouv.vitam.functional.administration.context.api.ContextService;
import fr.gouv.vitam.functional.administration.context.core.ContextValidator.ContextRejectionCause;
import fr.gouv.vitam.functional.administration.contract.api.ContractService;
import fr.gouv.vitam.functional.administration.contract.core.AccessContractImpl;
import fr.gouv.vitam.functional.administration.contract.core.IngestContractImpl;
import fr.gouv.vitam.functional.administration.security.profile.core.SecurityProfileService;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.VisibleForTesting;
import org.bson.conversions.Bson;

public class ContextServiceImpl implements ContextService {
    private static final String INVALID_IDENTIFIER_OF_THE_ACCESS_CONTRACT =
        "Invalid identifier of the access contract:";

    private static final String INVALID_IDENTIFIER_OF_THE_INGEST_CONTRACT =
        "Invalid identifier of the ingest contract:";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ContextServiceImpl.class);

    private static final String CONTEXT_IS_MANDATORY_PARAMETER = "contexts parameter is mandatory";
    private static final String CONTEXTS_IMPORT_EVENT = "STP_IMPORT_CONTEXT";
    private static final String CONTEXTS_UPDATE_EVENT = "STP_UPDATE_CONTEXT";
    public static final String CONTEXTS_BACKUP_EVENT = "STP_BACKUP_CONTEXT";
    private static final String UPDATE_CONTEXT_MANDATORY_PARAMETER = "context is mandatory";

    private final MongoDbAccessAdminImpl mongoAccess;
    private final LogbookOperationsClient logbookClient;
    private final VitamCounterService vitamCounterService;
    private final FunctionalBackupService functionalBackupService;
    private final ContractService<IngestContractModel> ingestContract;
    private final ContractService<AccessContractModel> accessContract;
    private final SecurityProfileService securityProfileService;

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    public ContextServiceImpl(
        MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService,
        SecurityProfileService securityProfileService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        ContractService<IngestContractModel> ingestContract = new IngestContractImpl(mongoAccess, vitamCounterService);
        ContractService<AccessContractModel> accessContract = new AccessContractImpl(mongoAccess, vitamCounterService);
        this.ingestContract = ingestContract;
        this.accessContract = accessContract;
        this.securityProfileService = securityProfileService;
        this.functionalBackupService = new FunctionalBackupService(vitamCounterService);
    }

    /**
     * Constructor
     *
     * @param mongoAccess MongoDB client
     */
    @VisibleForTesting
    public ContextServiceImpl(
        MongoDbAccessAdminImpl mongoAccess, VitamCounterService vitamCounterService,
        ContractService<IngestContractModel> ingestContract,
        ContractService<AccessContractModel> accessContract,
        SecurityProfileService securityProfileService, FunctionalBackupService functionalBackupService) {
        this.mongoAccess = mongoAccess;
        this.vitamCounterService = vitamCounterService;
        logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
        this.ingestContract = ingestContract;
        this.accessContract = accessContract;
        this.securityProfileService = securityProfileService;
        this.functionalBackupService = functionalBackupService;
    }

    @Override
    public RequestResponse<ContextModel> createContexts(List<ContextModel> contextModelList) throws VitamException {
        ParametersChecker.checkParameter(CONTEXT_IS_MANDATORY_PARAMETER, contextModelList);

        if (contextModelList.isEmpty()) {
            return new RequestResponseOK<>();
        }
        boolean slaveMode = vitamCounterService
            .isSlaveFunctionnalCollectionOnTenant(SequenceType.CONTEXT_SEQUENCE.getCollection(),
                ParameterHelper.getTenantParameter());

        GUID eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        ContextManager manager = new ContextManager(logbookClient, accessContract, ingestContract,
            securityProfileService, eip);

        manager.logStarted();

        final List<ContextModel> contextsListToPersist = new ArrayList<>();
        final VitamError error = new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
            .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        ArrayNode contextsToPersist = JsonHandler.createArrayNode();

        try {
            for (final ContextModel cm : contextModelList) {

                if (!slaveMode) {
                    final String code = vitamCounterService
                        .getNextSequenceAsString(ParameterHelper.getTenantParameter(),
                            SequenceType.CONTEXT_SEQUENCE);
                    cm.setIdentifier(code);
                }
                // if a contract have an id
                if (cm.getId() != null) {
                    error.addToErrors(new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem()).setMessage(
                        ContextRejectionCause.rejectIdNotAllowedInCreate(cm.getName()).getReason()));
                    continue;
                }

                // validate context
                if (manager.validateContext(cm, error)) {

                    cm.setId(GUIDFactory.newContextGUID().getId());
                    cm.setCreationdate(LocalDateUtil.getString(LocalDateUtil.now()));
                    cm.setLastupdate(LocalDateUtil.getString(LocalDateUtil.now()));

                    final ObjectNode contextNode = (ObjectNode) JsonHandler.toJsonNode(cm);
                    JsonNode jsonNode = contextNode.remove(VitamFieldsHelper.id());

                    if (jsonNode != null) {
                        contextNode.set("_id", jsonNode);
                    }

                    // change field permission.#tenantId by permission._tenantId
                    change(contextNode, VitamFieldsHelper.tenant(), TENANT_ID);

                    contextsToPersist.add(contextNode);
                    final ContextModel ctxt = JsonHandler.getFromJsonNode(contextNode, ContextModel.class);
                    contextsListToPersist.add(ctxt);
                }
                if (slaveMode) {
                    Optional<ContextRejectionCause> result =
                        manager.checkDuplicateInIdentifierSlaveModeValidator().validate(cm);
                    result.ifPresent(t -> error.addToErrors(
                        new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem()).setMessage(t.getReason())));
                }
            }

            if (null != error.getErrors() && !error.getErrors().isEmpty()) {
                // log book + application log
                // stop
                final String errorsDetails =
                    error.getErrors().stream().map(VitamError::getMessage).collect(Collectors.joining(","));
                manager.logValidationError(errorsDetails, CONTEXTS_IMPORT_EVENT);
                return error;
            }

            mongoAccess.insertDocuments(contextsToPersist, FunctionalAdminCollections.CONTEXT).close();

            functionalBackupService.saveCollectionAndSequence(
                eip,
                CONTEXTS_BACKUP_EVENT,
                FunctionalAdminCollections.CONTEXT,
                eip.toString()
            );

        } catch (final Exception exp) {
            final String err = "Import contexts error > " + exp.getMessage();
            manager.logFatalError(err);
            return error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem()).setDescription(err).setHttpCode(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }

        manager.logSuccess();

        return new RequestResponseOK<ContextModel>().addAllResults(contextsListToPersist)
            .setHttpCode(Response.Status.CREATED.getStatusCode());
    }

    @Override
    public DbRequestResult findContexts(JsonNode queryDsl)
        throws ReferentialException {
        return mongoAccess.findDocuments(queryDsl, FunctionalAdminCollections.CONTEXT);
    }

    @Override
    public ContextModel findOneContextById(String id)
        throws ReferentialException, InvalidParseOperationException {
        SanityChecker.checkParameter(id);
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        try {
            parser.addCondition(QueryHelper.eq("Identifier", id));
        } catch (InvalidCreateOperationException e) {
            throw new ReferentialException(e);
        }
        try (DbRequestResult result =
            mongoAccess.findDocuments(parser.getRequest().getFinalSelect(), FunctionalAdminCollections.CONTEXT)) {
            final List<ContextModel> list = result.getDocuments(Context.class, ContextModel.class);
            if (list.isEmpty()) {
                throw new ReferentialNotFoundException("Context not found");
            }
            return list.get(0);
        }
    }

    @Override
    public RequestResponse<ContextModel> updateContext(String id, JsonNode queryDsl)
        throws VitamException {
        ParametersChecker.checkParameter(UPDATE_CONTEXT_MANDATORY_PARAMETER, queryDsl);
        SanityChecker.checkJsonAll(queryDsl);
        final VitamError error =
            getVitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem(), "Context update error", StatusCode.KO)
                .setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());

        final ContextModel contextModel = findOneContextById(id);

        GUID eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        ContextManager manager = new ContextManager(logbookClient, accessContract, ingestContract,
            securityProfileService, eip);

        manager.logUpdateStarted(contextModel.getId());
        final JsonNode permissionsNode = queryDsl.findValue(ContextModel.TAG_PERMISSIONS);
        if (permissionsNode != null && permissionsNode.isArray()) {
            for (JsonNode permission : permissionsNode) {
                PermissionModel permissionModel = JsonHandler.getFromJsonNode(permission, PermissionModel.class);
                final int tenantId = permissionModel.getTenant();
                for (String accessContractId : permissionModel.getAccessContract()) {
                    if (!manager.checkIdentifierOfAccessContract(accessContractId, tenantId)) {
                        error.addToErrors(
                            new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                                .setMessage(INVALID_IDENTIFIER_OF_THE_INGEST_CONTRACT + accessContractId));
                    }
                }

                for (String ingestContractId : permissionModel.getIngestContract()) {
                    if (!manager.checkIdentifierOfIngestContract(ingestContractId, tenantId)) {
                        error.addToErrors(
                            new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                                .setMessage(INVALID_IDENTIFIER_OF_THE_ACCESS_CONTRACT + ingestContractId));
                    }
                }
            }

        }

        if (error.getErrors() != null && error.getErrors().size() > 0) {
            final String errorsDetails =
                error.getErrors().stream().map(VitamError::getMessage).collect(Collectors.joining(","));
            manager.logValidationError(errorsDetails, CONTEXTS_UPDATE_EVENT);

            return error.setState(StatusCode.KO.name());
        }

        String diff = null;
        try {
            DbRequestResult result = mongoAccess.updateData(queryDsl, FunctionalAdminCollections.CONTEXT);

            List<String> updates = null;
            // if at least one change was applied
            if (result.getCount() > 0) {
                // get first list of changes as we updated only one context
                updates = result.getDiffs().values().stream().findFirst().get();
            }

            // close result
            result.close();

            // create diff for evDetData
            if (updates != null && updates.size() > 0) {
                // concat changes
                String modifs = updates.stream().map(i -> i.toString()).collect(Collectors.joining("\n"));

                // create diff as json string
                final ObjectNode diffObject = JsonHandler.createObjectNode();
                diffObject.put("diff", modifs);
                diff = SanityChecker.sanitizeJson(diffObject);
            }

            functionalBackupService.saveCollectionAndSequence(eip,
                CONTEXTS_BACKUP_EVENT,
                FunctionalAdminCollections.CONTEXT,
                contextModel.getId()
            );

        } catch (SchemaValidationException e) {
            LOGGER.error(e);
            final String err = "Update context error > " + e.getMessage();

            // logbook error event 
            manager.logValidationError(err, CONTEXTS_UPDATE_EVENT);

            return getVitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem(), e.getMessage(),
                StatusCode.KO).setHttpCode(Response.Status.BAD_REQUEST.getStatusCode());
        } catch (final Exception e) {
            LOGGER.error(e);
            final String err = "Update context error > " + e.getMessage();
            error.setCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getItem())
                .setDescription(err)
                .setHttpCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

            // logbook error event 
            manager.logFatalError(err);
            return error;
        }

        // logbook success event
        manager.logUpdateSuccess(contextModel.getId(), diff);

        return new RequestResponseOK<>();
    }

    private VitamError getVitamError(String vitamCode, String error, StatusCode statusCode) {
        return VitamErrorUtils.getVitamError(vitamCode, error, "Context", statusCode);
    }

    /**
     * Context validator and logBook manager
     */
    private final static class ContextManager {
        private final GUID eip;
        private final LogbookOperationsClient logbookClient;
        private ContractService<AccessContractModel> accessContract;
        private ContractService<IngestContractModel> ingestContract;
        private SecurityProfileService securityProfileService;
        private List<ContextValidator> validators;

        public ContextManager(LogbookOperationsClient logbookClient,
            ContractService<AccessContractModel> accessContract,
            ContractService<IngestContractModel> ingestContract,
            SecurityProfileService securityProfileService, GUID eip) {
            this.eip = eip;
            this.logbookClient = logbookClient;
            this.accessContract = accessContract;
            this.ingestContract = ingestContract;
            this.securityProfileService = securityProfileService;
            // Init validator
            validators = Arrays.asList(
                createMandatoryParamsValidator(),
                securityProfileIdentifierValidator(),
                createCheckDuplicateInDatabaseValidator(),
                checkContract());
        }

        public boolean validateContext(ContextModel context, VitamError error)
            throws ReferentialException, InvalidParseOperationException {
            for (final ContextValidator validator : validators) {
                final Optional<ContextRejectionCause> result = validator.validate(context);
                if (result.isPresent()) {
                    // there is a validation error on this context
                    /* context is valid, add it to the list to persist */
                    error.addToErrors(new VitamError(VitamCode.CONTEXT_VALIDATION_ERROR.getItem())
                        .setMessage(result.get().getReason()));
                    // once a validation error is detected on a context, jump to next context
                    return false;
                }
            }
            return true;
        }

        /**
         * log start process
         *
         * @throws VitamException
         */
        private void logStarted() throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTEXTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.STARTED), eip);

            logbookClient.create(logbookParameters);
        }

        /**
         * log end success process
         *
         * @throws VitamException
         */
        private void logSuccess() throws VitamException {
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipUsage, CONTEXTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.OK,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.OK), eip);
            logbookClient.update(logbookParameters);
        }

        /**
         * log update start process
         *
         * @throws VitamException
         */
        private void logUpdateStarted(String id) throws VitamException {
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, CONTEXTS_UPDATE_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_UPDATE_EVENT, StatusCode.STARTED), eip);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTEXTS_UPDATE_EVENT +
                "." + StatusCode.STARTED);
            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookClient.create(logbookParameters);
        }

        /**
         * log update success process
         *
         * @throws VitamException
         */
        private void logUpdateSuccess(String id, String evDetData) throws VitamException {
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters =
                LogbookParametersFactory
                    .newLogbookOperationParameters(
                        eipUsage,
                        CONTEXTS_UPDATE_EVENT,
                        eip,
                        LogbookTypeProcess.MASTERDATA,
                        StatusCode.OK,
                        VitamLogbookMessages.getCodeOp(CONTEXTS_UPDATE_EVENT, StatusCode.OK),
                        eip);

            if (null != id && !id.isEmpty()) {
                logbookParameters.putParameterValue(LogbookParameterName.objectIdentifier, id);
            }
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                evDetData);
            logbookParameters.putParameterValue(LogbookParameterName.outcomeDetail, CONTEXTS_UPDATE_EVENT +
                "." + StatusCode.OK);
            logbookClient.update(logbookParameters);
        }

        /**
         * log fatal error (system or technical error)
         *
         * @param errorsDetails
         * @throws VitamException
         */
        private void logFatalError(String errorsDetails) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipUsage, CONTEXTS_IMPORT_EVENT, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.FATAL,
                    VitamLogbookMessages.getCodeOp(CONTEXTS_IMPORT_EVENT, StatusCode.FATAL), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            logbookClient.update(logbookParameters);
        }

        private void logValidationError(String errorsDetails, String action) throws VitamException {
            LOGGER.error("There validation errors on the input file {}", errorsDetails);
            final GUID eipUsage = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());
            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipUsage, action, eip, LogbookTypeProcess.MASTERDATA,
                    StatusCode.KO,
                    VitamLogbookMessages.getCodeOp(action, StatusCode.KO), eip);
            logbookMessageError(errorsDetails, logbookParameters);
            logbookClient.update(logbookParameters);
        }

        private void logbookMessageError(String errorsDetails, LogbookOperationParameters logbookParameters) {
            if (null != errorsDetails && !errorsDetails.isEmpty()) {
                try {
                    final ObjectNode object = JsonHandler.createObjectNode();
                    object.put("contextCheck", errorsDetails);

                    final String wellFormedJson = SanityChecker.sanitizeJson(object);
                    logbookParameters.putParameterValue(LogbookParameterName.eventDetailData, wellFormedJson);
                } catch (final InvalidParseOperationException e) {
                    // Do nothing
                }
            }
        }

        /**
         * Validate that context have not a missing mandatory parameter
         *
         * @return
         */
        private ContextValidator createMandatoryParamsValidator() {
            return (context) -> {

                if (StringUtils.isBlank(context.getName())) {
                    return Optional.of(ContextValidator.ContextRejectionCause.rejectMandatoryMissing(Context.NAME));
                }

                if (StringUtils.isBlank(context.getSecurityProfileIdentifier())) {
                    return Optional
                        .of(ContextValidator.ContextRejectionCause.rejectMandatoryMissing(Context.SECURITY_PROFILE));
                }

                return Optional.empty();
            };
        }

        /**
         * Validate that context have not a missing mandatory parameter
         *
         * @return
         */
        private ContextValidator securityProfileIdentifierValidator() {
            return (context) -> {

                Optional<SecurityProfileModel> securityProfileModel = Optional.empty();
                try {
                    securityProfileModel =
                        securityProfileService.findOneByIdentifier(context.getSecurityProfileIdentifier());

                } catch (ReferentialException | InvalidParseOperationException e) {
                    LOGGER.warn("An error occurred during security profile validation", e);
                }

                if (!securityProfileModel.isPresent()) {
                    return Optional.of(ContextValidator.ContextRejectionCause
                        .invalidSecurityProfile(context.getSecurityProfileIdentifier()));
                } else {
                    // OK
                    return Optional.empty();
                }
            };
        }


        /**
         * Check if the context the same name already exists in database
         *
         * @return
         */
        private ContextValidator createCheckDuplicateInDatabaseValidator() {
            return (context) -> {
                if (ParametersChecker.isNotEmpty(context.getIdentifier())) {
                    final Bson clause = eq(IngestContract.IDENTIFIER, context.getIdentifier());
                    final boolean exist = FunctionalAdminCollections.CONTEXT.getCollection().count(clause) > 0;
                    if (exist) {
                        return Optional
                            .of(ContextValidator.ContextRejectionCause.rejectDuplicatedInDatabase(context.getName()));
                    }
                }
                return Optional.empty();
            };
        }

        /**
         * Check if the ingest contract and access contract exist
         *
         * @return
         */
        private ContextValidator checkContract() {
            return (context) -> {
                ContextValidator.ContextRejectionCause rejection = null;

                final List<PermissionModel> pmList = context.getPermissions();
                for (final PermissionModel pm : pmList) {
                    final int tenant = pm.getTenant();

                    final Set<String> icList = pm.getIngestContract();
                    for (final String ic : icList) {
                        if (!checkIdentifierOfIngestContract(ic, tenant)) {
                            rejection = ContextValidator.ContextRejectionCause.rejectNoExistanceOfIngestContract(ic);
                            return Optional.of(rejection);
                        }
                    }

                    final Set<String> acList = pm.getAccessContract();
                    for (final String ac : acList) {
                        if (!checkIdentifierOfAccessContract(ac, tenant)) {
                            rejection = ContextValidator.ContextRejectionCause.rejectNoExistanceOfAccessContract(ac);
                            return Optional.of(rejection);
                        }
                    }
                }

                return Optional.empty();
            };
        }


        public boolean checkIdentifierOfIngestContract(String ic, int tenant)
            throws ReferentialException, InvalidParseOperationException {

            int initialTenant = VitamThreadUtils.getVitamSession().getTenantId();
            try {
                VitamThreadUtils.getVitamSession().setTenantId(tenant);
                return (null != ingestContract.findByIdentifier(ic));
            } finally {
                VitamThreadUtils.getVitamSession().setTenantId(initialTenant);
            }
        }

        public boolean checkIdentifierOfAccessContract(String ac, int tenant)
            throws ReferentialException, InvalidParseOperationException {

            int initialTenant = VitamThreadUtils.getVitamSession().getTenantId();
            try {
                VitamThreadUtils.getVitamSession().setTenantId(tenant);
                return (null != accessContract.findByIdentifier(ac));
            } finally {
                VitamThreadUtils.getVitamSession().setTenantId(initialTenant);
            }
        }

        /**
         * Check if the Id of the context already exists in database
         *
         * @return
         */
        private ContextValidator checkDuplicateInIdentifierSlaveModeValidator() {
            return (context) -> {
                if (context.getIdentifier() == null || context.getIdentifier().isEmpty()) {
                    return Optional.of(ContextValidator.ContextRejectionCause.rejectMandatoryMissing(
                        AccessContract.IDENTIFIER));
                }
                ContextValidator.ContextRejectionCause rejection = null;
                final int tenant = ParameterHelper.getTenantParameter();
                final Bson clause =
                    and(eq(TENANT_ID, tenant), eq(AccessContract.IDENTIFIER, context.getIdentifier()));
                final boolean exist = FunctionalAdminCollections.CONTEXT.getCollection().count(clause) > 0;
                if (exist) {
                    rejection =
                        ContextValidator.ContextRejectionCause.rejectDuplicatedInDatabase(context.getIdentifier());
                }
                return rejection == null ? Optional.empty() : Optional.of(rejection);
            };
        }
    }

    @Override
    public void close() {
        logbookClient.close();
    }
}
