package uk.gov.moj.sjp.it.test;

import static java.time.ZoneId.of;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.producer.DecisionToReferCaseForCourtHearingSavedProducer;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class CourtReferralIT extends BaseIntegrationTest {

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

        final Optional<Envelope<CaseReferredForCourtHearing>> event = new EventListener()
                .subscribe(CaseReferredForCourtHearing.class.getAnnotation(Event.class).value())
                .run(completeCaseProducer::completeCase)
                .run(decisionToReferCaseForCourtHearingSavedProducer::saveDecisionToReferCaseForCourtHearing)
                .popEvent(CaseReferredForCourtHearing.class);

        assertThat(event.isPresent(), is(true));

        assertThat(event.get().payload(), equalTo(caseReferredForCourtHearing()
                .withCaseId(caseId)
                .withSessionId(sjpSessionId)
                .withReferralReasonId(referralReasonId)
                .withHearingTypeId(hearingTypeId)
                .withEstimatedHearingDuration(estimatedHearingDuration)
                .withListingNotes(listingNotes)
                .withReferredAt(resultedOn.withZoneSameLocal(of("UTC")))
                .build()));
    }

}
