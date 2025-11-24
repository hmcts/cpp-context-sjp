package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.math.BigDecimal.ROUND_DOWN;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDocumentBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class CaseDocumentsRestrictionTests {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    @InjectMocks
    private SjpQueryView queryView;
    @Mock
    private CaseService caseService;

    @Test
    public void findCaseFiltersOtherAndFinancialMeansDocuments() {

        //given
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.query.case-filter-other-and-financial-means-documents").build(),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "Transport for London")
                .add("policeFlag", false)
                .build();

        final CaseView caseView = new CaseView(CaseDetailBuilder.aCase()
                .withCaseId(caseId)
                .withDefendantDetail(
                        DefendantDetailBuilder
                                .aDefendantDetail()
                                .build())
                .build(),
                prosecutorPayload);
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
    public void findCaseFiltersOtherAndFinancialMeansDocumentsWithAocpEligible() {

        //given
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.query.case-filter-other-and-financial-means-documents").build(),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "Transport for London")
                .add("policeFlag", false)
                .build();

        CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withCaseId(caseId)
                .withVictimSurcharge(BigDecimal.ONE)
                .withCosts(BigDecimal.TEN)
                .withDefendantDetail(
                        DefendantDetailBuilder
                                .aDefendantDetail()
                                .build())
                .build();
        caseDetail.setAocpEligible(true);
        caseDetail.setAocpTotalCost(BigDecimal.TEN.add(BigDecimal.ONE));
        final CaseView caseView = new CaseView(caseDetail,
                prosecutorPayload);
        when(caseService.findCaseAndFilterOtherAndFinancialMeansDocuments(caseId.toString()))
                .thenReturn(caseView);

        //when
        final JsonEnvelope response = queryView.findCaseAndFilterOtherAndFinancialMeansDocuments(envelope);

        //then
        //This is only simple test checking if appropriate methods were invoked
        assertThat(response, jsonEnvelope(withMetadataEnvelopedFrom(envelope).withName("sjp.query.case-response"),
                payloadIsJson(withJsonPath("$.id", is(caseId.toString())))
        ));

        assertThat(caseView.getAocpTotalCost(), is(BigDecimal.valueOf(11.00).setScale(2, ROUND_DOWN)));
    }

    @Test
    public void findCaseDocumentsFiltersOtherAndFinancialMeans() {
        //given
        final UUID caseId = UUID.randomUUID();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("sjp.query.case-filter-other-and-financial-means-documents").build(),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        final UUID materialId = UUID.randomUUID();
        final CaseDocument caseDocument = CaseDocumentBuilder.aCaseDocument().withMaterialId(materialId).build();
        final CaseDocumentsView caseDocumentsView = new CaseDocumentsView(Collections.singletonList(new CaseDocumentView(caseDocument)));

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
