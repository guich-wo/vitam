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
package fr.gouv.vitam.worker.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.google.common.collect.Iterables;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveUnitType;
import fr.gouv.vitam.common.model.unit.AgentTypeModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.model.unit.ArchiveUnitRoot;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * test class for {@link ElementMapper}
 */
public class ArchiveUnitMapperTest {

    private static JAXBContext jaxbContext;

    private ArchiveUnitMapper archiveUnitMapper;

    @BeforeClass
    public static void setUp() throws Exception {
        jaxbContext = JAXBContext.newInstance("fr.gouv.culture.archivesdefrance.seda.v2");
    }

    @Before
    public void init() throws Exception {
        archiveUnitMapper = new ArchiveUnitMapper(new DescriptiveMetadataMapper(), new RuleMapper());
    }


    @Test
    public void should_map_element_to_hashMap() throws Exception {
        // Given
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(getClass().getResourceAsStream(
            "/element_unit_with_agent_type.xml"));

        // When
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");

        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final List<AgentTypeModel> recipients = archiveUnit.getDescriptiveMetadataModel().getRecipient();
        assertThat(recipients).hasSize(1);

        AgentTypeModel recipient = Iterables.getOnlyElement(recipients);
        assertThat(recipient.getFirstName()).isEqualTo("John");
        assertThat(recipient.getBirthName()).isEqualTo("Doe");
        assertThat(recipient.getNationalities()).hasSize(2);
        assertThat(recipient.getNationalities().get(1)).isEqualTo("Algerian");
        assertThat(recipient.getIdentifiers()).hasSize(2);
        assertThat(recipient.getIdentifiers().get(1)).isEqualTo("EFG");
        assertThat(recipient.getGivenName()).isEqualTo("Martin");
        assertThat(recipient.getBirthPlace()).isNotNull();
        assertThat(recipient.getBirthPlace().getGeogname()).isEqualTo("Geogname");
        assertThat(recipient.getDeathPlace()).isNotNull();
        assertThat(recipient.getDeathPlace().getAddress()).isEqualTo("123 my Street");

    }

    @Test
    public void should_transform_startDate_to_valid_date_format_when_sip_is_ingest() throws Exception {
        // Given
        final InputStream resourceAsStream = getClass().getResourceAsStream(
            "/ArchiveUnitDateWithBadFormatInElasticSearch/archive_unit_with_bad_startDate.xml");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(resourceAsStream);
        // When
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");
        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertThat(descriptiveMetadataModel.getStartDate())
            .isEqualTo("2012-11-15T00:00:00+03:00");
    }

    @Test
    public void should_do_nothing_on_startDate_when_date_format_is_good() throws Exception {
        // Given
        final InputStream resourceAsStream = getClass().getResourceAsStream(
            "/ArchiveUnitDateWithBadFormatInElasticSearch/archive_unit_with_good_date_format.xml");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(resourceAsStream);
        // When
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");
        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertThat(descriptiveMetadataModel.getStartDate())
            .isEqualTo("2012-09-26T15:34:08.284+02:00");
    }

    @Test
    public void should_transform_createdDate_to_valid_date_format_when_sip_is_ingest() throws Exception {
        // Given
        final InputStream resourceAsStream = getClass().getResourceAsStream(
            "/ArchiveUnitDateWithBadFormatInElasticSearch/archive_unit_with_bad_createdDate.xml");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(resourceAsStream);
        // When
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");
        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertThat(descriptiveMetadataModel.getCreatedDate())
            .isEqualTo("2012-11-15T00:00:00+03:00");
    }

    @Test
    public void should_transform_transactedDate_to_valid_date_format_when_sip_is_ingest() throws Exception {
        // Given
        final InputStream resourceAsStream = getClass().getResourceAsStream(
            "/ArchiveUnitDateWithBadFormatInElasticSearch/archive_unit_with_bad_transactedDate.xml");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(resourceAsStream);
        // When
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");
        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertThat(descriptiveMetadataModel.getTransactedDate())
            .isEqualTo("2012-11-15T00:00:00+03:00");
    }

    @Test
    public void should_transform_acquiredDate_to_valid_date_format_when_sip_is_ingest() throws Exception {
        // Given
        final InputStream resourceAsStream = getClass().getResourceAsStream(
            "/ArchiveUnitDateWithBadFormatInElasticSearch/archive_unit_with_bad_acquiredDate.xml");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(resourceAsStream);
        // When
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");
        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertThat(descriptiveMetadataModel.getAcquiredDate())
            .isEqualTo("2012-11-15T00:00:00+03:00");
    }

    @Test
    public void should_transform_sentDate_to_valid_date_format_when_sip_is_ingest() throws Exception {
        // test for BUG #3844
        // Given
        final InputStream resourceAsStream = getClass().getResourceAsStream(
            "/ArchiveUnitDateWithBadFormatInElasticSearch/archive_unit_with_bad_sentDate.xml");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(resourceAsStream);
        //when
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");
        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertThat(descriptiveMetadataModel.getSentDate())
            .isEqualTo("2015-11-15T00:00:00+03:00");
    }

    @Test
    public void should_transform_receivedDate_to_valid_date_format_when_sip_is_ingest() throws Exception {
        // test for BUG #3844
        // Given
        final InputStream resourceAsStream = getClass().getResourceAsStream(
            "/ArchiveUnitDateWithBadFormatInElasticSearch/archive_unit_with_bad_receivedDate.xml");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(resourceAsStream);
        //when
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");
        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertThat(descriptiveMetadataModel.getReceivedDate())
            .isEqualTo("2016-11-15T00:00:00+03:00");
    }

    @Test
    public void should_transform_registeredDate_to_valid_date_format_when_sip_is_ingest() throws Exception {
        // test for BUG #3844
        // Given
        final InputStream resourceAsStream = getClass().getResourceAsStream(
            "/ArchiveUnitDateWithBadFormatInElasticSearch/archive_unit_with_bad_registerDate.xml");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ArchiveUnitType archiveUnitType = (ArchiveUnitType) unmarshaller.unmarshal(resourceAsStream);
        //when
        ArchiveUnitRoot archiveUnitRoot = archiveUnitMapper.map(archiveUnitType, "", "");
        // Then
        ArchiveUnitModel archiveUnit = archiveUnitRoot.getArchiveUnit();
        final DescriptiveMetadataModel descriptiveMetadataModel = archiveUnit.getDescriptiveMetadataModel();
        assertThat(descriptiveMetadataModel.getRegisteredDate())
            .isEqualTo("2012-11-15T00:00:00+03:00");
    }
}
