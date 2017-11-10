package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CourtReferral;
import uk.gov.moj.cpp.sjp.persistence.repository.CourtReferralRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)

public class CourtReferralListenerTest {

    @Mock
    private CourtReferralRepository repository;

    @InjectMocks
    private CourtReferralListener courtReferralListener;

    @Captor
    private ArgumentCaptor<CourtReferral> captor;

    private UUID caseId;

    @Before
    public void setup() {
       this.caseId = UUID.randomUUID();
    }

    @Test
    public void shouldCreateCourtReferral() {

        final LocalDate hearingDate = LocalDate.now().plusWeeks(1);

        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(hearingDate.toString(), "hearingDate")
                .build();

        courtReferralListener.courtReferralCreated(event);

        verify(repository).save(captor.capture());
        final CourtReferral courtReferral = captor.getValue();
        assertThat(courtReferral.getCaseId().toString(), is(caseId.toString()));
        assertThat(courtReferral.getHearingDate(), is(hearingDate));
    }

    @Test
    public void shouldActionCourtReferral() {

        when(repository.findBy(caseId)).thenReturn(new CourtReferral(caseId, LocalDate.now().plusWeeks(1)));

        final ZonedDateTime actioned = ZonedDateTime.now(UTC);

        final JsonEnvelope event = envelope()
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(actioned.toString(), "actioned")
                .build();

        courtReferralListener.courtReferralActioned(event);

        verify(repository).save(captor.capture());
        final CourtReferral courtReferral = captor.getValue();
        assertThat(courtReferral.getCaseId().toString(), is(caseId.toString()));
        assertThat(courtReferral.getActioned(), is(actioned));
    }

}