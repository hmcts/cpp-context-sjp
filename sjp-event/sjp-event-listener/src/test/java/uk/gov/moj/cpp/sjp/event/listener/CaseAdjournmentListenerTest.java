package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded.caseAdjournedToLaterSjpHearingRecorded;
import static uk.gov.moj.cpp.sjp.event.CaseAdjournmentToLaterSjpHearingElapsed.caseAdjournmentToLaterSjpHearingElapsed;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseAdjournmentToLaterSjpHearingElapsed;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAdjournmentListenerTest {

    private final UUID caseId = randomUUID();

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseDetail caseDetail;

    @InjectMocks
    private CaseAdjournmentListener caseAdjournmentListener;

    @Test
    public void shouldUpdateCaseDetailsWithAdjournmentToDate() {
        final LocalDate adjournedTo = now(UTC);

        final CaseAdjournedToLaterSjpHearingRecorded caseAdjournedToLaterSjpHearingRecorded = caseAdjournedToLaterSjpHearingRecorded()
                .withCaseId(caseId)
                .withAdjournedTo(adjournedTo)
                .build();

        final Envelope<CaseAdjournedToLaterSjpHearingRecorded> event = envelopeFrom(metadataWithRandomUUID("sjp.events.case-adjourned-to-later-sjp-hearing-recorded"), caseAdjournedToLaterSjpHearingRecorded);

        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        caseAdjournmentListener.handleCaseAdjournedToLaterSjpHearingRecorded(event);

        verify(caseDetail).setAdjournedTo(adjournedTo);
    }

    @Test
    public void shouldRemoveAdjournmentToDateFormCaseDetails() {
        final ZonedDateTime elapsedAt = ZonedDateTime.now();

        final CaseAdjournmentToLaterSjpHearingElapsed caseAdjournmentToLaterSjpHearingElapsed = caseAdjournmentToLaterSjpHearingElapsed()
                .withCaseId(caseId)
                .withElapsedAt(elapsedAt)
                .build();

        final Envelope<CaseAdjournmentToLaterSjpHearingElapsed> event = envelopeFrom(metadataWithRandomUUID("sjp.events.case-adjournment-to-later-sjp-hearing-elapsed"), caseAdjournmentToLaterSjpHearingElapsed);

        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        caseAdjournmentListener.handleCaseAdjournmentToLaterSjpHearingElapsed(event);

        verify(caseDetail).setAdjournedTo(null);
    }

    @Test
    public void shouldHandleCaseAdjournmentEvents() {
        assertThat(CaseAdjournmentListener.class, isHandlerClass(EVENT_LISTENER)
                .with(allOf(
                        method("handleCaseAdjournedToLaterSjpHearingRecorded").thatHandles("sjp.events.case-adjourned-to-later-sjp-hearing-recorded"),
                        method("handleCaseAdjournmentToLaterSjpHearingElapsed").thatHandles("sjp.events.case-adjournment-to-later-sjp-hearing-elapsed")
                )));
    }
}
