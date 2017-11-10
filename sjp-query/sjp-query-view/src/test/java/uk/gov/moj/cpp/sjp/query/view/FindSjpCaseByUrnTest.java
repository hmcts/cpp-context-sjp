package uk.gov.moj.cpp.sjp.query.view;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher;
import uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.UUID;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FindSjpCaseByUrnTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseService caseService;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    @Test
    public void findSjpCaseByUrnShouldHaveProperAnnotations() {
        assertThat(SjpQueryView.class,
                HandlerClassMatcher.isHandlerClass(Component.QUERY_VIEW)
                    .with(HandlerMethodMatcher.method("findSjpCaseByUrn")
                    .thatHandles("sjp.query.sjp-case-by-urn")));
    }

    @Test
    public void shouldFindSjpCaseByUrn() {
        String urn = "urn";
        UUID caseId = UUID.randomUUID();

        JsonEnvelope jsonEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.query.sjp-case-by-urn"),
                createObjectBuilder().add("urn", urn).build());

        CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withUrn(urn)
                .withCaseId(caseId)
                .build();
        CaseView caseView = new CaseView(caseDetail);

        when(caseService.findSjpCaseByUrn(urn)).thenReturn(caseView);

        JsonEnvelope actualJsonEnvelope = sjpQueryView.findSjpCaseByUrn(jsonEnvelope);

        verify(caseService).findSjpCaseByUrn(urn);

        assertThat(actualJsonEnvelope, JsonEnvelopeMatcher.jsonEnvelope(
                JsonEnvelopeMetadataMatcher.metadata().withName("sjp.query.case-response"),
                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                        JsonPathMatchers.withJsonPath("$.urn", is(urn)),
                        JsonPathMatchers.withJsonPath("$.id", is(caseId.toString()))
                ))
        ));
    }
}
