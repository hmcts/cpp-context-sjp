package uk.gov.justice.api.resource;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.api.service.CourtExtractDataService;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultQueryApiCasesCaseIdDocumentsCourtExtractResourceTest {

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Mock
    private CourtExtractDataService courtExtractDataService;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextCaptor;

    @InjectMocks
    private DefaultQueryApiCasesCaseIdDocumentsCourtExtractResource endpointHandler;

    private final UUID caseId = randomUUID();
    private final UUID userId = randomUUID();
    private final byte[] documentBytes = "test".getBytes();

    @Before
    public void init() {
        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(userId));
    }

    @Test
    public void shouldRunAllInterceptorsAndFetchAndStreamDocument() throws Exception {
        when(courtExtractDataService.getCourtExtractData(any(JsonEnvelope.class))).
                thenReturn(this.buildMockCourtData());

        when(courtExtractDataService.generatePdfDocument(any(JsonObject.class))).thenReturn(documentBytes);

        final Response courtExtractResponse = endpointHandler.getCasesByCaseIdDocumentsCourtExtract(caseId, userId);

        verify(courtExtractDataService).getCourtExtractData(any(JsonEnvelope.class));
        verify(courtExtractDataService).generatePdfDocument(any(JsonObject.class));

        assertThat(courtExtractResponse.getStatus(), is(SC_OK));
        assertThat((ByteArrayInputStream) courtExtractResponse.getEntity(), isA(ByteArrayInputStream.class));
        assertThat(courtExtractResponse.getHeaders(), hasEntry(is("Content-Type"), is(singletonList("application/pdf"))));
        assertThat(courtExtractResponse.getHeaders(), hasEntry(is("Content-Disposition"), is(singletonList("attachment; filename=\"court_extract.pdf\""))));

        verifyInterceptorChainExecution();
    }

    @Test
    public void shouldReturnNotFoundStatusWhenCourtExtractDataNotFound(){
        when(courtExtractDataService.getCourtExtractData(any(JsonEnvelope.class))).
                thenReturn(Optional.empty());

        final Response courtExtractResponse = endpointHandler.getCasesByCaseIdDocumentsCourtExtract(caseId, userId);
        verify(courtExtractDataService).getCourtExtractData(any(JsonEnvelope.class));

        assertThat(courtExtractResponse.getStatus(), is(SC_NOT_FOUND));
    }

    @Test
    public void shouldReturnInternalServerErrorIfDocumentServiceFails() throws IOException {
        when(courtExtractDataService.getCourtExtractData(any(JsonEnvelope.class))).
                thenReturn(this.buildMockCourtData());

        when(courtExtractDataService.generatePdfDocument(any(JsonObject.class))).thenThrow(new IOException());

        final Response courtExtractResponse = endpointHandler.getCasesByCaseIdDocumentsCourtExtract(caseId, userId);

        verify(courtExtractDataService).getCourtExtractData(any(JsonEnvelope.class));
        verify(courtExtractDataService).generatePdfDocument(any(JsonObject.class));

        assertThat(courtExtractResponse.getStatus(), is(SC_INTERNAL_SERVER_ERROR));
    }




    private void verifyInterceptorChainExecution() {
        verify(interceptorChainProcessor).process(interceptorContextCaptor.capture());

        MatcherAssert.assertThat(interceptorContextCaptor.getValue().inputEnvelope(),
                jsonEnvelope(metadata().withName(DefaultQueryApiCasesCaseIdDocumentsCourtExtractResource.QUERY_ACTION_NAME).withUserId(userId.toString()),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString()))
                ))
        ));
    }

    private Optional<JsonObject> buildMockCourtData() {
        return Optional.of(Json.createReader(getClass().getClassLoader().
                getResourceAsStream("uk/gov/moj/cpp/sjp/query/court-extract/case-court-extract-data.json")).
                readObject());
    }

}
