package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved.decisionToReferCaseForCourtHearingSaved;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.resulting.event.DecisionToReferCaseForCourtHearingSaved;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtReferralProcessorTest {

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private CourtReferralProcessor courtReferralProcessor;

    @Test
    public void shouldSentReferCaseForCourtHearingCommandWhenDecisionToReferCaseForCourtHearingSaved() {

        final DecisionToReferCaseForCourtHearingSaved decisionToReferCaseForCourtHearingSaved = decisionToReferCaseForCourtHearingSaved()
                .withCaseId(randomUUID())
                .withSessionId(randomUUID())
                .withReferralReasonId(randomUUID())
                .withHearingTypeId(randomUUID())
                .withEstimatedHearingDuration(nextInt(0, 999))
                .withListingNotes(randomAlphanumeric(100))
                .withDecisionSavedAt(now(UTC))
                .build();

        final Envelope<DecisionToReferCaseForCourtHearingSaved> event = envelopeFrom(metadataWithRandomUUID("public.resulting.decision-to-refer-case-for-court-hearing-saved"), decisionToReferCaseForCourtHearingSaved);

        courtReferralProcessor.decisionToReferCaseForCourtHearingSaved(event);

        verify(sender).send(argThat(
                jsonEnvelope(
                        metadata().envelopedWith(event.metadata()).withName("sjp.command.refer-case-for-court-hearing"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(decisionToReferCaseForCourtHearingSaved.getCaseId().toString())),
                                withJsonPath("$.sessionId", equalTo(decisionToReferCaseForCourtHearingSaved.getSessionId().toString())),
                                withJsonPath("$.referralReasonId", equalTo(decisionToReferCaseForCourtHearingSaved.getReferralReasonId().toString())),
                                withJsonPath("$.hearingTypeId", equalTo(decisionToReferCaseForCourtHearingSaved.getHearingTypeId().toString())),
                                withJsonPath("$.estimatedHearingDuration", equalTo(decisionToReferCaseForCourtHearingSaved.getEstimatedHearingDuration())),
                                withJsonPath("$.listingNotes", equalTo(decisionToReferCaseForCourtHearingSaved.getListingNotes())),
                                withJsonPath("$.requestedAt", equalTo(decisionToReferCaseForCourtHearingSaved.getDecisionSavedAt().toString()))
                        )))));
    }

    @Test
    public void shouldHandleCourtReferralRelatedEvents() {
        assertThat(CourtReferralProcessor.class, isHandlerClass(EVENT_PROCESSOR)
                .with(allOf(
                        method("decisionToReferCaseForCourtHearingSaved").thatHandles("public.resulting.decision-to-refer-case-for-court-hearing-saved"),
                        method("caseReferredForCourtHearing").thatHandles("sjp.events.case-referred-for-court-hearing")
                )));
    }

}