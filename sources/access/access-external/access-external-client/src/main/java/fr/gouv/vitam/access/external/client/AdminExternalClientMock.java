package fr.gouv.vitam.access.external.client;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.AbstractMockClient;
import fr.gouv.vitam.common.external.client.ClientMockResultHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.FileFormatModel;
import fr.gouv.vitam.common.model.administration.FileRulesModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.stream.StreamUtils;

/**
 * Mock client implementation for Admin External
 */
public class AdminExternalClientMock extends AbstractMockClient implements AdminExternalClient {


    public static final String ID = "identifier1";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminExternalClientMock.class);

    @Override
    public RequestResponse getAccessionRegisterDetail(VitamContext vitamContext, String id,
        JsonNode query)
        throws InvalidParseOperationException, AccessExternalClientServerException,
        AccessExternalClientNotFoundException {
        return ClientMockResultHelper.getAccessionRegisterDetail();
    }

    @Override
    public RequestResponse updateAccessContract(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getAccessContracts().toJsonNode());
    }

    @Override
    public RequestResponse updateIngestContract(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getIngestContracts().toJsonNode());
    }

    @Override
    public RequestResponse createProfiles(VitamContext vitamContext, InputStream profiles)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper
            .createReponse(ClientMockResultHelper.getProfiles(Status.CREATED.getStatusCode()).toJsonNode())
            .setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse createProfileFile(VitamContext vitamContext,
        String profileMetadataId, InputStream profile)
        throws InvalidParseOperationException, AccessExternalClientException {
        return new RequestResponseOK().setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public Response downloadProfileFile(VitamContext vitamContext, String profileMetadataId)
        throws AccessExternalClientException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, StreamUtils.toInputStream("Vitam Test"),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse createContexts(VitamContext vitamContext, InputStream contexts)
        throws InvalidParseOperationException {
        return ClientMockResultHelper
            .createReponse(ClientMockResultHelper.getContexts(Status.CREATED.getStatusCode()).toJsonNode())
            .setHttpCode(Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse updateContext(VitamContext vitamContext, String id,
        JsonNode queryDsl)
        throws AccessExternalClientException, InvalidParseOperationException {
        return ClientMockResultHelper.createReponse(
            ClientMockResultHelper.getContexts(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(
                Status.CREATED.getStatusCode());
    }

    @Override
    public RequestResponse checkTraceabilityOperation(VitamContext vitamContext,
        JsonNode query)
        throws AccessExternalClientServerException, InvalidParseOperationException {
        return ClientMockResultHelper.checkOperationTraceability();
    }

    @Override
    public Response downloadTraceabilityOperationFile(VitamContext vitamContext,
        String operationId)
        throws AccessExternalClientServerException {
        return new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
            MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
    }

    @Override
    public RequestResponse launchAudit(VitamContext vitamContext, JsonNode auditOption) {
        return new RequestResponseOK().setHttpCode(Status.ACCEPTED.getStatusCode());
    }

    @Override
    public RequestResponse<FileFormatModel> findFormats(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getFormat();
    }

    @Override
    public RequestResponse<FileRulesModel> findRules(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getRuleList();
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContracts(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getIngestContracts();
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContracts(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getAccessContracts();
    }

    @Override
    public RequestResponse<ContextModel> findContexts(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getContexts(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ProfileModel> findProfiles(VitamContext vitamContext,
        JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getProfiles(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegister(
        VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return ClientMockResultHelper.getAccessionRegisterSummary();
    }

    @Override
    public RequestResponse<FileFormatModel> findFormatById(VitamContext vitamContext,
        String formatId)
        throws VitamClientException {
        return ClientMockResultHelper.getFormat();
    }

    @Override
    public RequestResponse<FileRulesModel> findRuleById(VitamContext vitamContext,
        String ruleId)
        throws VitamClientException {
        return ClientMockResultHelper.getRule();
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContractById(
        VitamContext vitamContext, String contractId) throws VitamClientException {
        return ClientMockResultHelper.getIngestContracts();
    }

    @Override
    public RequestResponse<AccessContractModel> findAccessContractById(
        VitamContext vitamContext, String contractId) throws VitamClientException {
        return ClientMockResultHelper.getAccessContracts();
    }

    @Override
    public RequestResponse<ContextModel> findContextById(VitamContext vitamContext,
        String contextId)
        throws VitamClientException {
        return ClientMockResultHelper.getContexts(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ProfileModel> findProfileById(VitamContext vitamContext,
        String profileId)
        throws VitamClientException {
        return ClientMockResultHelper.getProfiles(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<AccessionRegisterSummaryModel> findAccessionRegisterById(
        VitamContext vitamContext, String accessionRegisterId) throws VitamClientException {
        return ClientMockResultHelper.getAccessionRegisterSummary();
    }

    @Override
    public RequestResponse<AgenciesModel> findAgencies(VitamContext vitamContext, JsonNode query) {
        return ClientMockResultHelper.getAgencies();
    }

    @Override
    public RequestResponse<AgenciesModel> findAgencyByID(VitamContext vitamContext, String agencyById)
        throws VitamClientException {
        return ClientMockResultHelper.getAgencies();
    }

    @Override
    public RequestResponse updateSecurityProfile(VitamContext vitamContext, String identifier, JsonNode queryDsl)
        throws VitamClientException {
        return ClientMockResultHelper.getSecurityProfiles();
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfiles(VitamContext vitamContext, JsonNode select)
        throws VitamClientException {
        return (RequestResponse<SecurityProfileModel>) ClientMockResultHelper.getSecurityProfiles();
    }

    @Override
    public RequestResponse<SecurityProfileModel> findSecurityProfileById(VitamContext vitamContext, String identifier)
        throws VitamClientException {
        return (RequestResponse<SecurityProfileModel>) ClientMockResultHelper.getSecurityProfiles();
    }

    @Override
    public RequestResponse updateProfile(VitamContext vitamContext, String profileMetadataId, JsonNode queryDsl)
        throws AccessExternalClientException {
        return ClientMockResultHelper.getProfiles(200);
    }

    @Override
    public RequestResponse<ProcessDetail> listOperationsDetails(VitamContext vitamContext,
        ProcessQuery query)
        throws VitamClientException {
        return new RequestResponseOK<ProcessDetail>().addResult(new ProcessDetail())
            .setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessStatus(VitamContext vitamContext,
        String id)
        throws VitamClientException {
        ItemStatus pwork = null;
        try {
            pwork = ClientMockResultHelper.getItemStatus(id);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new VitamClientException(e.getMessage(), e);
        }
        return new RequestResponseOK<ItemStatus>().addResult(pwork).setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ItemStatus> getOperationProcessExecutionDetails(
        VitamContext vitamContext, String id)
        throws VitamClientException {
        return new RequestResponseOK<ItemStatus>().addResult(new ItemStatus(ID)).setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ItemStatus> cancelOperationProcessExecution(
        VitamContext vitamContext, String id)
        throws VitamClientException {
        return new RequestResponseOK<ItemStatus>().addResult(new ItemStatus(ID)).setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(
        VitamContext vitamContext, String actionId, String id)
        throws VitamClientException {
        return new RequestResponseOK<ItemStatus>().addResult(new ItemStatus(ID)).setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse<WorkFlow> getWorkflowDefinitions(VitamContext vitamContext) throws VitamClientException {
        return new RequestResponseOK<WorkFlow>().addResult(new WorkFlow()).setHttpCode(Status.OK.getStatusCode());
    }



    @Override
    public RequestResponse createAgencies(VitamContext vitamContext, InputStream agencies, String filename)
        throws AccessExternalClientException, InvalidParseOperationException {
        StreamUtils.closeSilently(agencies);
        return ClientMockResultHelper.createReponse(
            ClientMockResultHelper.getAgencies(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(
                Status.CREATED.getStatusCode());
    }



    @Override
    public RequestResponse createFormats(VitamContext vitamContext, InputStream formats, String filename)
        throws AccessExternalClientException, InvalidParseOperationException {
        StreamUtils.closeSilently(formats);
        return ClientMockResultHelper.createReponse(
            ClientMockResultHelper.getFormat(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(
                Status.CREATED.getStatusCode());
    }



    @Override
    public RequestResponse createRules(VitamContext vitamContext, InputStream rules, String filename)
        throws AccessExternalClientException, InvalidParseOperationException {
        StreamUtils.closeSilently(rules);
        return ClientMockResultHelper.createReponse(
            ClientMockResultHelper.getRule(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(
                Status.CREATED.getStatusCode());
    }



    @Override
    public RequestResponse createSecurityProfiles(VitamContext vitamContext, InputStream securityProfiles,
        String filename) throws AccessExternalClientException, InvalidParseOperationException, VitamClientException {
        StreamUtils.closeSilently(securityProfiles);
        return ClientMockResultHelper.createReponse(
            ClientMockResultHelper.getSecurityProfiles(Status.CREATED.getStatusCode()).toJsonNode()).setHttpCode(
                Status.CREATED.getStatusCode());
    }

    private Response checkInternalDocuments(VitamContext vitamContext, AdminCollections documentType,
        InputStream stream)
        throws VitamClientException {
        StreamUtils.closeSilently(stream);

        if (AdminCollections.RULES.equals(documentType) || AdminCollections.FORMATS.equals(documentType) ||
            AdminCollections.AGENCIES.equals(documentType)) {
            return new AbstractMockClient.FakeInboundResponse(Status.OK, StreamUtils.toInputStream("Vitam Test"),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
        } else {
            try {
                return new AbstractMockClient.FakeInboundResponse(Status.INTERNAL_SERVER_ERROR,
                    JsonHandler.writeToInpustream(VitamCodeHelper
                        .toVitamError(VitamCode.ADMIN_EXTERNAL_CHECK_DOCUMENT_ERROR, "Collection not found")),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
            } catch (InvalidParseOperationException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                return new AbstractMockClient.FakeInboundResponse(Status.INTERNAL_SERVER_ERROR,
                    new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes()),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
            }
        }
    }

    @Override
    public Response checkRules(VitamContext vitamContext, InputStream rules) throws VitamClientException {
        return checkInternalDocuments(vitamContext, AdminCollections.RULES, rules);
    }

    @Override
    public Response checkFormats(VitamContext vitamContext, InputStream formats) throws VitamClientException {
        return checkInternalDocuments(vitamContext, AdminCollections.FORMATS, formats);
    }

    @Override
    public Response checkAgencies(VitamContext vitamContext, InputStream agencies) throws VitamClientException {
        return checkInternalDocuments(vitamContext, AdminCollections.AGENCIES, agencies);
    }

    @Override
    public RequestResponse createIngestContracts(VitamContext vitamContext, InputStream ingestContracts)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getIngestContracts().toJsonNode());
    }



    @Override
    public RequestResponse createAccessContracts(VitamContext vitamContext, InputStream accessContracts)
        throws InvalidParseOperationException, AccessExternalClientException {
        return ClientMockResultHelper.createReponse(ClientMockResultHelper.getAccessContracts().toJsonNode());
    }

    @Override
    public Response downloadRulesReport(VitamContext vitamContext, String opId)
        throws VitamClientException {
        return ClientMockResultHelper.getObjectStream();
    }

    @Override
    public RequestResponse unitEvidenceAudit(VitamContext vitamContext, String unitId) {
        return ClientMockResultHelper.getUnitEvidenceAudit(Status.OK.getStatusCode());
    }

    @Override
    public RequestResponse objectGroupEvidenceAudit(VitamContext vitamContext, String objectGroupId) {
        return ClientMockResultHelper.getObjectGroupEvidenceAudit(Status.OK.getStatusCode());
    }
}
