package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.helper.CaseDocumentHelper.getCaseDocumentContent;
import static uk.gov.moj.sjp.it.helper.CaseDocumentHelper.getCaseDocumentMetadata;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.stub.MaterialStub;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

public class GetCaseDocumentIT extends BaseIntegrationTest {

    private final UUID documentId = randomUUID();
    private final UUID materialId = randomUUID();
    private final UUID tflCaseId = randomUUID();
    private final UUID tflUser = randomUUID();
    private final UUID tvlUser = randomUUID();
    private final UUID legalAdviserUser = randomUUID();
    private final String fileName = randomAlphanumeric(10);
    private final String mimeType = "application/pdf";
    private final String documentType = "OTHER-Test";
    private final String documentContent = "Test document content";
    private final ZonedDateTime addedAt = ZonedDateTime.now();

    @Before
    public void init() {
        UsersGroupsStub.stubForUserDetails(tflUser, TFL);
        UsersGroupsStub.stubForUserDetails(tvlUser, ProsecutingAuthority.TVL);
        UsersGroupsStub.stubForUserDetails(legalAdviserUser, "ALL");

        createCase(tflCaseId, TFL);
        addDocument(tflCaseId, documentId, materialId, documentType, legalAdviserUser);
    }

    @Test
    public void shouldGetDocumentMetadata() {
        MaterialStub.stubMaterialMetadata(materialId, fileName, mimeType, addedAt);

        final Response documentMetadataResponse = getCaseDocumentMetadata(tflCaseId, documentId, tflUser);

        assertThat(documentMetadataResponse.getHeaderString(CONTENT_TYPE), equalTo("application/vnd.sjp.query.case-document-metadata+json"));

        assertThat(documentMetadataResponse.readEntity(String.class), isJson(allOf(
                withJsonPath("caseDocumentMetadata.fileName", equalTo(fileName)),
                withJsonPath("caseDocumentMetadata.mimeType", equalTo(mimeType)),
                withJsonPath("caseDocumentMetadata.addedAt", equalTo(addedAt.toString()))
        )));
    }

    @Test
    public void shouldRestrictDocumentMetadataAccess() {
        MaterialStub.stubMaterialMetadata(materialId, fileName, mimeType, addedAt);

        assertThat(getCaseDocumentMetadata(tflCaseId, documentId, tflUser).getStatus(), is(SC_OK));
        assertThat(getCaseDocumentMetadata(tflCaseId, documentId, tvlUser).getStatus(), is(SC_FORBIDDEN));
        assertThat(getCaseDocumentMetadata(tflCaseId, documentId, legalAdviserUser).getStatus(), is(SC_OK));
    }

    @Test
    public void shouldGetDocumentContent() {
        MaterialStub.stubMaterialContent(materialId, documentContent.getBytes(), mimeType);
        final Response documentContentResponse = getCaseDocumentContent(tflCaseId, documentId, tflUser);

        assertThat(documentContentResponse.getHeaderString(CONTENT_TYPE), equalTo(mimeType));
        assertThat(documentContentResponse.readEntity(String.class), equalTo(documentContent));
    }

    @Test
    public void shouldRestrictDocumentContentAccess() {
        MaterialStub.stubMaterialContent(materialId, documentContent.getBytes(), mimeType);

        assertThat(getCaseDocumentContent(tflCaseId, documentId, tflUser).getStatus(), is(SC_OK));
        assertThat(getCaseDocumentContent(tflCaseId, documentId, tvlUser).getStatus(), is(SC_FORBIDDEN));
        assertThat(getCaseDocumentContent(tflCaseId, documentId, legalAdviserUser).getStatus(), is(SC_OK));
    }

    @Test
    public void shouldPassThroughNotFoundFromMaterialContext() {
        MaterialStub.stubMaterialMetadataWithResponseStatusCode(materialId, SC_NOT_FOUND);
        MaterialStub.stubMaterialContentWithResponseStatusCode(materialId, SC_NOT_FOUND);

        assertThat(getCaseDocumentMetadata(tflCaseId, documentId, tflUser).getStatus(), is(SC_NOT_FOUND));
        assertThat(getCaseDocumentContent(tflCaseId, documentId, tflUser).getStatus(), is(SC_NOT_FOUND));
    }

    @Test
    public void shouldReturnNotFoundIfCaseOrDocumentDoesNotExist() {
        assertThat(getCaseDocumentMetadata(tflCaseId, randomUUID(), tflUser).getStatus(), is(SC_NOT_FOUND));
        assertThat(getCaseDocumentMetadata(randomUUID(), randomUUID(), tflUser).getStatus(), is(SC_NOT_FOUND));
    }

    @Test
    public void shouldPassThroughForbiddenFromMaterialContext() {
        MaterialStub.stubMaterialMetadataWithResponseStatusCode(materialId, SC_FORBIDDEN);
        MaterialStub.stubMaterialContentWithResponseStatusCode(materialId, SC_FORBIDDEN);

        assertThat(getCaseDocumentMetadata(tflCaseId, documentId, tflUser).getStatus(), is(SC_FORBIDDEN));
        assertThat(getCaseDocumentContent(tflCaseId, documentId, tflUser).getStatus(), is(SC_FORBIDDEN));
    }

    private static void createCase(final UUID caseId, final ProsecutingAuthority prosecutingAuthority) {
        final CreateCase.CreateCasePayloadBuilder createTflCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults().withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority);

        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createTflCasePayloadBuilder));
    }

    private static JsonObject addDocument(final UUID caseId, final UUID documentId, final UUID materialId, final String documentType, final UUID userId) {
        try (final CaseDocumentHelper caseDocumentHelper = new CaseDocumentHelper(caseId)) {
            caseDocumentHelper.addCaseDocument(userId, documentId, materialId, documentType);
            return caseDocumentHelper.findDocument(userId, 0, documentType, 1);
        }
    }
}
