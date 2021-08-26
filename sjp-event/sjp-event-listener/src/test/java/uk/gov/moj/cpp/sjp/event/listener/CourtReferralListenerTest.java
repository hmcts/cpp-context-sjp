package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
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
import static uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded.caseReferralForCourtHearingRejectionRecorded;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseCourtReferralStatusRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtReferralListenerTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String REJECTION_REASON = "Business validation failed";
    private static final ZonedDateTime RECEIVED_AT = now(UTC).minusDays(1);
    private static final ZonedDateTime REJECTED_AT = now(UTC);
    private static final String URN = "22C22222222";

    @Mock
    private CaseCourtReferralStatusRepository caseCourtReferralStatusRepository;

    @InjectMocks
    private CourtReferralListener courtReferralListener;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseDetail caseDetail;

    @Test
    public void shouldMarkReferralStatusAsRejectedWhenCaseReferralRejected() {
        final CaseReferralForCourtHearingRejectionRecorded caseReferralForCourtHearingRejected =
                caseReferralForCourtHearingRejectionRecorded()
                        .withCaseId(CASE_ID)
                        .withRejectionReason(REJECTION_REASON)
                        .withRejectedAt(REJECTED_AT)
                        .build();
        Envelope<CaseReferralForCourtHearingRejectionRecorded> eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-referral-for-court-hearing-rejection-recorded"),
                caseReferralForCourtHearingRejected);

        final CaseCourtReferralStatus caseCourtReferralStatus = new CaseCourtReferralStatus(
                CASE_ID,
                URN,
                RECEIVED_AT);

        when(caseCourtReferralStatusRepository.findBy(CASE_ID)).thenReturn(caseCourtReferralStatus);
        when(caseRepository.findBy(CASE_ID)).thenReturn(caseDetail);
        courtReferralListener.handleCaseReferredForCourtHearingRejectionRecorded(eventEnvelope);
        assertThat(caseCourtReferralStatus.getRejectedAt(), Matchers.is(REJECTED_AT));
        assertThat(caseCourtReferralStatus.getRejectionReason(), Matchers.is(REJECTION_REASON));
    }

    @Test
    public void shouldCreateCaseCourtReferralStatusWhenCaseReferredForHearing() {
        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withReferredAt(RECEIVED_AT)
                .build();
        Envelope<CaseReferredForCourtHearing> eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-referred-for-court-hearing"),
                caseReferredForCourtHearing);

        when(caseRepository.findBy(CASE_ID)).thenReturn(caseDetail);
        courtReferralListener.handleCaseReferredForCourtHearing(eventEnvelope);
        caseCourtReferralStatusRepository.save(new CaseCourtReferralStatus(CASE_ID, URN, RECEIVED_AT));

        verify(caseCourtReferralStatusRepository).save(new CaseCourtReferralStatus(CASE_ID, URN, RECEIVED_AT));

        verify(caseDetail).setReferredForCourtHearing(true);
    }

    @Test
    public void shouldHandleCourtReferralRelatedEvents() {
        assertThat(CourtReferralListener.class, isHandlerClass(EVENT_LISTENER)
                .with(allOf(
                        method("handleCaseReferredForCourtHearing").thatHandles("sjp.events.case-referred-for-court-hearing"),
                        method("handleCaseReferredForCourtHearingRejectionRecorded").thatHandles("sjp.events.case-referral-for-court-hearing-rejection-recorded")
                )));
    }

    @Test
    public void shouldMarkManagedByATCMAsFalseStatusWhenCaseReferredForHearing() {
        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withCaseId(CASE_ID)
                .withReferredAt(RECEIVED_AT)
                .build();
        final Envelope<CaseReferredForCourtHearing> eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-referred-for-court-hearing"),
                caseReferredForCourtHearing);

        when(caseRepository.findBy(CASE_ID)).thenReturn(caseDetail);

        courtReferralListener.handleCaseReferredForCourtHearing(eventEnvelope);
        caseCourtReferralStatusRepository.save(new CaseCourtReferralStatus(CASE_ID, URN, RECEIVED_AT));

        verify(caseCourtReferralStatusRepository).save(new CaseCourtReferralStatus(CASE_ID, URN, RECEIVED_AT));
        verify(caseDetail).setReferredForCourtHearing(true);
        verify(caseDetail).setManagedByAtcm(false);

    }

    @Test
    public void shouldMarkManagedByATCMStatusAsTrueWhenCaseReferralRejected() {
        final CaseReferralForCourtHearingRejectionRecorded caseReferralForCourtHearingRejected =
                caseReferralForCourtHearingRejectionRecorded()
                        .withCaseId(CASE_ID)
                        .withRejectionReason(REJECTION_REASON)
                        .withRejectedAt(REJECTED_AT)
                        .build();
        final Envelope<CaseReferralForCourtHearingRejectionRecorded> eventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-referral-for-court-hearing-rejection-recorded"),
                caseReferralForCourtHearingRejected);

        final CaseCourtReferralStatus caseCourtReferralStatus = new CaseCourtReferralStatus(
                CASE_ID,
                URN,
                RECEIVED_AT);

        when(caseCourtReferralStatusRepository.findBy(CASE_ID)).thenReturn(caseCourtReferralStatus);
        when(caseRepository.findBy(CASE_ID)).thenReturn(caseDetail);

        courtReferralListener.handleCaseReferredForCourtHearingRejectionRecorded(eventEnvelope);

        assertThat(caseCourtReferralStatus.getRejectedAt(), Matchers.is(REJECTED_AT));
        assertThat(caseCourtReferralStatus.getRejectionReason(), Matchers.is(REJECTION_REASON));
        verify(caseDetail).setManagedByAtcm(true);
    }
}
