package uk.gov.moj.sjp.it.test;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseReferralHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.producer.DecisionToReferCaseForCourtHearingSavedProducer;
import uk.gov.moj.sjp.it.producer.ReferToCourtHearingProducer;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class CourtReferralIT extends BaseIntegrationTest {

    public static final String SJP_EVENTS_CASE_REFERRED_FOR_COURT_HEARING = "sjp.events.case-referred-for-court-hearing";
    private UUID caseId = randomUUID();

    @Before
    public void setUp() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults().withId(caseId);

        final EventListener eventListener = new EventListener();
        eventListener
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));

        final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(CaseMarkedReadyForDecision.EVENT_NAME);

        assertThat(jsonEnvelope.isPresent(), equalTo(true));
    }

    @Test
    public void shouldReferCaseForCourtHearing() {

        final UUID sjpSessionId = randomUUID();
        final ZonedDateTime resultedOn = now(UTC);
        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final Integer estimatedHearingDuration = nextInt(1, 999);
        final String listingNotes = randomAlphanumeric(100);

        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        final DecisionToReferCaseForCourtHearingSavedProducer decisionToReferCaseForCourtHearingSavedProducer = new DecisionToReferCaseForCourtHearingSavedProducer(caseId,
                sjpSessionId, referralReasonId, hearingTypeId, estimatedHearingDuration, listingNotes, resultedOn);

        final Optional<JsonEnvelope> caseReferredForCourtHearingEnvelope = new EventListener()
                .subscribe(CaseReferredForCourtHearing.class.getAnnotation(Event.class).value())
                .run(completeCaseProducer::completeCase)
                .run(decisionToReferCaseForCourtHearingSavedProducer::saveDecisionToReferCaseForCourtHearing)
                .popEvent(SJP_EVENTS_CASE_REFERRED_FOR_COURT_HEARING);

        assertThat(caseReferredForCourtHearingEnvelope.isPresent(), is(true));

        assertThat(caseReferredForCourtHearingEnvelope.get(),
                jsonEnvelope(
                        metadata().withName(SJP_EVENTS_CASE_REFERRED_FOR_COURT_HEARING),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", CoreMatchers.equalTo(caseId.toString())),
                                withJsonPath("$.sessionId", CoreMatchers.equalTo(sjpSessionId.toString())),
                                withJsonPath("$.referralReasonId", CoreMatchers.equalTo(referralReasonId.toString())),
                                withJsonPath("$.hearingTypeId", CoreMatchers.equalTo(hearingTypeId.toString())),
                                withJsonPath("$.estimatedHearingDuration", CoreMatchers.equalTo(estimatedHearingDuration)),
                                withJsonPath("$.listingNotes", CoreMatchers.equalTo(listingNotes)),
                                withJsonPath("$.referredAt", CoreMatchers.equalTo(resultedOn.toString()))
                        ))));

        final CaseCourtReferralStatus referralStatus = await()
                .atMost(10, TimeUnit.SECONDS)
                .until(
                        () -> CaseReferralHelper.findReferralStatusForCase(caseId),
                        notNullValue());

        assertThat(referralStatus.getRequestedAt(), notNullValue());
        assertThat(referralStatus.getRejectedAt(), nullValue());
        assertThat(referralStatus.getRejectionReason(), nullValue());
    }

    @Test
    public void shouldRecordCaseReferralRejection() {
        final UUID sjpSessionId = randomUUID();
        final ZonedDateTime resultedOn = now(UTC);
        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final Integer estimatedHearingDuration = nextInt(1, 999);
        final String listingNotes = randomAlphanumeric(100);

        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        final DecisionToReferCaseForCourtHearingSavedProducer decisionToReferCaseForCourtHearingSavedProducer = new DecisionToReferCaseForCourtHearingSavedProducer(caseId,
                sjpSessionId, referralReasonId, hearingTypeId, estimatedHearingDuration, listingNotes, resultedOn);

        final String referralRejectionReason = "Test referral rejection reason";

        String rejectionRecordedEventName = CaseReferralForCourtHearingRejectionRecorded.class.getAnnotation(Event.class).value();
        final Optional<JsonEnvelope> hearingRejectionRecordedEvent = new EventListener()
                .subscribe(rejectionRecordedEventName)
                .run(completeCaseProducer::completeCase)
                .run(decisionToReferCaseForCourtHearingSavedProducer::saveDecisionToReferCaseForCourtHearing)
                .run(() -> ReferToCourtHearingProducer.rejectCaseReferral(caseId, referralRejectionReason))
                .popEvent(rejectionRecordedEventName);

        assertThat(hearingRejectionRecordedEvent.isPresent(), is(true));

        final CaseCourtReferralStatus referralStatus = await()
                .atMost(10, TimeUnit.SECONDS)
                .until(
                        () -> CaseReferralHelper.findReferralStatusForCase(caseId),
                        hasProperty("rejectedAt", notNullValue()));

        assertThat(referralStatus.getRequestedAt(), notNullValue());
        assertThat(referralStatus.getRejectedAt(), notNullValue());
        assertThat(referralStatus.getRejectionReason(), is(referralRejectionReason));
    }

}
