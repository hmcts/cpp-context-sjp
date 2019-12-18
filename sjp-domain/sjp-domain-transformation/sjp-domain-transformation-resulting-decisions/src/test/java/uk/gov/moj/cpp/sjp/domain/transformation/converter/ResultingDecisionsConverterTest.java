package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.transformation.DecisionsTransformer.SJP_EVENTS_CASE_ADJOURNED_TO_LATER_SJP_HEARING_RECORDED;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TestUtils.readJson;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class ResultingDecisionsConverterTest {

    private ResultingDecisionsConverter resultingDecisionsConverter;

    private static final UUID caseId = UUID.fromString("fd51dc11-dc3c-40c8-90c6-79bb50b5752a");
    private static final UUID metadataId = UUID.fromString("fd51dc11-dc3c-40c0-90c6-79bb50b5752a");

    private ReferForCourtHearingConverter referForCourtHearingConverter = new ReferForCourtHearingConverter(mock(SjpViewStoreService.class));
    private SjpViewStoreService sjpViewStoreService;
    private AdjournConverter adjournConverter;

    @Before
    public void setUp() {
        this.sjpViewStoreService = mock(SjpViewStoreService.class);
        this.adjournConverter = mock(AdjournConverter.class);
        this.resultingDecisionsConverter = new ResultingDecisionsConverter(
                sjpViewStoreService,
                referForCourtHearingConverter,
                adjournConverter);
        when(sjpViewStoreService.getPlea("e76ab6f5-9156-4ecd-81f4-01fb9241ad5f")).thenReturn("GUILTY");
    }

    @Test
    public void shouldConvertDischargeDecision() {
        // given the offence decision
        final JsonObject offenceDecisionJsonObject = readJson(
                "resultingDecisionsConverter/discharge-input.json",
                JsonObject.class);

        when(sjpViewStoreService.getPlea("e76ab6f5-9156-4ecd-81f4-01fb9241ad5f")).thenReturn("GUILTY");

        final JsonEnvelope transformedEventObject = resultingDecisionsConverter.convert(completedEventEnvelope(), offenceDecisionJsonObject);
        final JsonObject expectedJsonObject = getExpectedJsonObject(transformedEventObject,
                "resultingDecisionsConverter/discharge-output.json");

        assertThat(transformedEventObject.asJsonObject(), is(expectedJsonObject));
    }

    @Test
    public void shouldConvertDismissDecision() {
        // given the offence decision
        final JsonObject offenceDecisionJsonObject = readJson(
                "resultingDecisionsConverter/dismiss-input.json",
                JsonObject.class);

        when(sjpViewStoreService.getPlea("e76ab6f5-9156-4ecd-81f4-01fb9241ad5f")).thenReturn("GUILTY");

        final JsonEnvelope transformedEventObject = resultingDecisionsConverter.convert(completedEventEnvelope(), offenceDecisionJsonObject);
        final JsonObject expectedJsonObject = getExpectedJsonObject(transformedEventObject,
                "resultingDecisionsConverter/dismiss-output.json");

        assertThat(transformedEventObject.asJsonObject(), is(expectedJsonObject));
    }


    @Test
    public void shouldConvertFinancialPenaltyDecision() {
        // given the offence decision
        final JsonObject offenceDecisionJsonObject = readJson(
                "resultingDecisionsConverter/financial-penalty-input.json",
                JsonObject.class);

        when(sjpViewStoreService.getPlea("e76ab6f5-9156-4ecd-81f4-01fb9241ad5f")).thenReturn("GUILTY");

        final JsonEnvelope transformedEventObject = resultingDecisionsConverter.convert(completedEventEnvelope(), offenceDecisionJsonObject);
        final JsonObject expectedJsonObject = getExpectedJsonObject(transformedEventObject,
                "resultingDecisionsConverter/financial-penalty-output.json");

        assertThat(transformedEventObject.asJsonObject(), is(expectedJsonObject));
    }

    @Test
    public void shouldConvertReferForCourtHearingDecision() {
        // given the offence decision
        final JsonObject offenceDecisionJsonObject = readJson(
                "resultingDecisionsConverter/refer-for-court-hearing-input.json",
                JsonObject.class);

        when(sjpViewStoreService.getPlea("e76ab6f5-9156-4ecd-81f4-01fb9241ad5f")).thenReturn("GUILTY");

        final JsonEnvelope transformedEventObject = resultingDecisionsConverter.convert(completedEventEnvelope(), offenceDecisionJsonObject);

        final JsonObject expectedJsonObject = getExpectedJsonObject(transformedEventObject,
                "resultingDecisionsConverter/refer-for-court-hearing-output.json");

        assertThat(transformedEventObject.asJsonObject(), is(expectedJsonObject));
    }


    @Test
    public void shouldConvertRSJPDecisionWithoutSessionID() {
        // given the offence decision
        final JsonObject offenceDecisionJsonObject = readJson(
                "resultingDecisionsConverter/rsjp-input-no-session-id.json",
                JsonObject.class);

        final JsonEnvelope transformedEventObject = resultingDecisionsConverter.convert(completedEventEnvelope(), offenceDecisionJsonObject);

        final JsonObject expectedJsonObject = getExpectedJsonObject(transformedEventObject,
                "resultingDecisionsConverter/rsjp-output.json");

        assertThat(transformedEventObject.asJsonObject(), is(expectedJsonObject));
    }

    @Test
    public void shouldConvertRSJPDecisionWithSessionID() {
        // given the offence decision

        final JsonObject offenceDecisionJsonObject = readJson(
                "resultingDecisionsConverter/rsjp-input.json",
                JsonObject.class);

        final JsonEnvelope transformedEventObject = resultingDecisionsConverter.convert(completedEventEnvelope(), offenceDecisionJsonObject);

        final JsonObject expectedJsonObject = getExpectedJsonObject(transformedEventObject,
                "resultingDecisionsConverter/rsjp-output.json");

        assertThat(transformedEventObject.asJsonObject(), is(expectedJsonObject));
    }



    @Test
    public void shouldConvertAdjournDecision() {
        // given the offence decision
        final JsonObject adjournDecisionObject = readJson(
                "resultingDecisionsConverter/adjournsjp-input.json",
                JsonObject.class);

        final JsonObject offenceDecisionObject = readJson(
                "resultingDecisionsConverter/adjournsjp-offencedecision.json",
                JsonObject.class);
        final JsonObject jsonObject = mock(JsonObject.class);
        when(adjournConverter.convert(adjournDecisionObject,
                null,
                "NO_VERDICT"))
                .thenReturn(offenceDecisionObject);

        // when
        final JsonEnvelope transformedEventObject = resultingDecisionsConverter.convert(adjournEventEnvelope(), adjournDecisionObject);

        final JsonObject expectedJsonObject = getExpectedJsonObject(transformedEventObject,
                "resultingDecisionsConverter/adjournsjp-output.json");

        assertThat(transformedEventObject.asJsonObject(), is(expectedJsonObject));
    }


    @Test
    public void shouldConvertReferForOpenCourtDecision() {
        // given the offence decision
        final JsonObject offenceDecisionJsonObject = readJson(
                "resultingDecisionsConverter/opencourt-input.json",
                JsonObject.class);

        final JsonEnvelope transformedEventObject = resultingDecisionsConverter.convert(completedEventEnvelope(), offenceDecisionJsonObject);

        final JsonObject expectedJsonObject = getExpectedJsonObject(transformedEventObject,
                "resultingDecisionsConverter/opencourt-output.json");
        assertThat(transformedEventObject.asJsonObject(), is(expectedJsonObject));
    }

    private JsonEnvelope completedEventEnvelope() {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataOf(metadataId, "sjp.events.case-completed"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build()
        );
    }

    private JsonEnvelope adjournEventEnvelope() {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataOf(metadataId, SJP_EVENTS_CASE_ADJOURNED_TO_LATER_SJP_HEARING_RECORDED),
                createObjectBuilder()
                        .add("caseId", UUID.fromString("05b215d8-1432-4ea5-b9e9-11cb6c9ae8e7").toString())
                        .build()
        );
    }

    private JsonObject getExpectedJsonObject(final JsonEnvelope transformedEventObject, final String filePath) {
        return readJson(
                filePath,
                JsonObject.class,
                transformedEventObject
                        .payloadAsJsonObject()
                        .getJsonArray("offenceDecisions")
                        .getJsonObject(0)
                        .getString(ID));
    }

}