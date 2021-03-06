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
package fr.gouv.vitam.worker.core.plugin.dip;

import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.BINARIES_RANK;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.GUID_TO_PATH_RANK;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.MANIFEST_XML_RANK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.xmlunit.builder.Input;

public class CreateManifestTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    private CreateManifest createManifest;

    static Map<String, String> prefix2Uri = new HashMap<>();

    static {
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.0");
    }

    @Before
    public void setUp() throws Exception {
        createManifest = new CreateManifest(metaDataClientFactory);
    }

    @Test
    public void should_create_manifest() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);

        JsonNode queryUnit =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/query.json"));

        JsonNode queryUnitWithTree =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryWithTreeProjection.json"));

        JsonNode queryObjectGroup =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryObjectGroup.json"));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadata.json")));

        given(metaDataClient.selectUnits(queryUnitWithTree)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadataTree.json")));

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultObjectGroup.json")));

        File manifestFile = tempFolder.newFile();
        given(handlerIO.getOutput(MANIFEST_XML_RANK)).willReturn(new ProcessingUri(UriPrefix.WORKSPACE, manifestFile.getPath()));
        given(handlerIO.getNewLocalFile(manifestFile.getPath())).willReturn(manifestFile);

        File guidToPathFile = tempFolder.newFile();
        given(handlerIO.getOutput(GUID_TO_PATH_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, guidToPathFile.getPath()));
        given(handlerIO.getNewLocalFile(guidToPathFile.getPath())).willReturn(guidToPathFile);

        File binaryFile = tempFolder.newFile();
        given(handlerIO.getOutput(BINARIES_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, binaryFile.getPath()));
        given(handlerIO.getNewLocalFile(binaryFile.getPath())).willReturn(binaryFile);

        // When
        ItemStatus itemStatus = createManifest.execute(WorkerParametersFactory.newWorkerParameters(), handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, Object> linkBetweenBinaryIdAndFileName =
            JsonHandler.getMapFromInputStream(new FileInputStream(guidToPathFile));

        assertThat(linkBetweenBinaryIdAndFileName)
            .containsEntry("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq", "Content/aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq")
            .containsEntry("aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq", "Content/aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq")
            .containsEntry("aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba", "Content/aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba");

        ArrayNode fromFile = (ArrayNode) JsonHandler.getFromFile(binaryFile);

        assertThat(fromFile).hasSize(3).extracting(JsonNode::asText)
            .containsExactlyInAnyOrder("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq", "aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq",
                "aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba");

        Assert.assertThat(Input.fromFile(manifestFile), hasXPath("//vitam:ArchiveRestitutionRequest/vitam:DataObjectPackage/vitam:BinaryDataObject/vitam:Uri",
            equalTo("Content/aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq"))
            .withNamespaceContext(prefix2Uri));
        Assert.assertThat(Input.fromFile(manifestFile), hasXPath("//vitam:ArchiveRestitutionRequest/vitam:DataObjectPackage/vitam:ManagementMetadata/vitam:OriginatingAgencyIdentifier",
            equalTo("FRAN_NP_005568"))
            .withNamespaceContext(prefix2Uri));
    }

}