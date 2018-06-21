package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDocumentBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.Collections;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseDocumentsRestrictionTests {

    @InjectMocks
    private SjpQueryView queryView;

    @Mock
    private CaseService caseService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Test
    public void findCaseFiltersOtherAndFinancialMeansDocuments() {

        //given
        UUID caseId = UUID.randomUUID();
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.query.case-filter-other-and-financial-means-documents").build(),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());


        CaseView caseView = new CaseView(CaseDetailBuilder.aCase().withCaseId(caseId).build());
        when(caseService.findCaseAndFilterOtherAndFinancialMeansDocuments(caseId.toString()))
                .thenReturn(caseView);

        //when
        final JsonEnvelope response = queryView.findCaseAndFilterOtherAndFinancialMeansDocuments(envelope);

        //then
        //This is only simple test checking if appropriate methods were invoked
        assertThat(response, jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("sjp.query.case-response"),
                                        payloadIsJson(withJsonPath("$.id", is(caseId.toString())))
                ));
    }

    @Test
    public void findCaseDocumentsFiltersOtherAndFinancialMeans() {
        //given
        UUID caseId = UUID.randomUUID();
        JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.query.case-filter-other-and-financial-means-documents").build(),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        UUID materialId = UUID.randomUUID();
        final CaseDocument caseDocument = CaseDocumentBuilder.aCaseDocument().withMaterialId(materialId).build();
        CaseDocumentsView caseDocumentsView = new CaseDocumentsView(Collections.singletonList(new CaseDocumentView(caseDocument)));

        when(caseService.findCaseDocumentsFilterOtherAndFinancialMeans(caseId))
                .thenReturn(caseDocumentsView);

        //when
        final JsonEnvelope response = queryView.findCaseDocumentsFilterOtherAndFinancialMeans(envelope);

        //then
        //This is only simple test checking if appropriate methods were invoked
        assertThat(response, jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("sjp.query.case-documents-response"),
                payloadIsJson(withJsonPath("$.caseDocuments[0].materialId", is(materialId.toString())))
        ));
    }

    @Test
    public void findCaseWithFilteringHandlesProperAction() {
        assertThat(SjpQueryView.class,
                isHandlerClass(QUERY_VIEW)
                        .with(method("findCaseAndFilterOtherAndFinancialMeansDocuments")
                                .thatHandles("sjp.query.case-filter-other-and-financial-means-documents")
                        )
        );
    }

    @Test
    public void findCaseDocumentsWithFilteringHandlesProperAction() {
        assertThat(SjpQueryView.class,
                isHandlerClass(QUERY_VIEW)
                        .with(method("findCaseDocumentsFilterOtherAndFinancialMeans")
                                .thatHandles("sjp.query.case-documents-filter-other-and-financial-means")
                        )
        );
    }

}
