package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.ONLINE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.POSTAL;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestReasonChanged;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.VerdictCancelled;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceUpdatedListenerTest {

    private UUID caseId = randomUUID();
    private UUID defendantId = randomUUID();
    private UUID offenceId = randomUUID();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private OffenceRepository offenceRepository;

    @Mock
    private CaseSearchResultRepository searchResultRepository;

    @InjectMocks
    private OffenceUpdatedListener listener;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JsonEnvelope envelope;

    @Mock
    private PleaCancelled pleaCancelled;

    @Mock
    private VerdictCancelled verdictCancelled;

    @Mock
    private OffenceDetail offence;

    @Mock
    private DefendantDetail defendant;

    @Mock
    private JsonObject payload;

    @Mock
    private CaseSearchResult searchResult;

    @Spy
    private Clock clock = new StoppedClock(new UtcClock().now());

    @Mock
    private OnlinePleaRepository.PleaDetailsRepository onlinePleaRepository;

    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaCaptor;

    @Mock
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    @Captor
    private ArgumentCaptor<OnlinePleaDetail> onlinePleaDetailCaptor;

    @Test
    @Deprecated
    public void shouldUpdateGuiltyPleaOnlineLegacy() {
        final PleaUpdated pleaUpdated = givenPleaUpdatedWithPleaType(PleaType.GUILTY, clock.now());
        when(jsonObjectToObjectConverter.convert(payload, PleaUpdated.class)).thenReturn(pleaUpdated);
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        final Metadata metadata1 = metadataBuilder;
        listener.updatePlea(envelope);
        Metadata metadata = metadata1;

        verify(offence).setPlea(pleaUpdated.getPlea());
        verify(offence).setPleaMethod(pleaUpdated.getPleaMethod());
        verify(offence).setPleaDate(Optional.ofNullable(pleaUpdated.getUpdatedDate()).orElseGet(() -> envelope.metadata().createdAt().orElse(null)));

        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());
        assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), is(pleaUpdated.getUpdatedDate()));
        assertThat(onlinePleaCaptor.getValue().getCaseId(), is(caseId));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getComeToCourt(), is(false));

        verify(onlinePleaDetailRepository).save(onlinePleaDetailCaptor.capture());
        assertThat(onlinePleaDetailCaptor.getValue().getPlea(), is(pleaUpdated.getPlea()));
        assertThat(onlinePleaDetailCaptor.getValue().getMitigation(), is(pleaUpdated.getMitigation()));
        assertThat(onlinePleaDetailCaptor.getValue().getNotGuiltyBecause(), is(pleaUpdated.getNotGuiltyBecause()));


    }

    @Test
    public void shouldUpdateGuiltyPleaOnline() {
        final PleadedGuilty pleadedGuilty = new PleadedGuilty(caseId, defendantId, offenceId, ONLINE, "The dog ate my ticket", clock.now());
        final PleaType plea = GUILTY;
        when(jsonObjectToObjectConverter.convert(payload, PleadedGuilty.class)).thenReturn(pleadedGuilty);
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        listener.updateOffenceDetailsWithPleadedGuilty(envelope);

        verify(offence).setPlea(plea);
        verify(offence).setPleaMethod(pleadedGuilty.getMethod());
        verify(offence).setPleaDate(pleadedGuilty.getPleadDate());
        assertOnlinePlea(plea, true, false);
    }

    @Test
    public void shouldUpdateCaseStatusPleaReceivedReadyForDecision() {
        final PleaUpdated pleaUpdated = givenPleaUpdatedWithPleaType(GUILTY_REQUEST_HEARING, clock.now());
        when(jsonObjectToObjectConverter.convert(payload, PleaUpdated.class)).thenReturn(pleaUpdated);
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        final Metadata metadata1 = metadataBuilder;
        listener.updatePlea(envelope);
        Metadata metadata = metadata1;

        verify(offence).setPlea(pleaUpdated.getPlea());
        verify(offence).setPleaMethod(pleaUpdated.getPleaMethod());
        verify(offence).setPleaDate(Optional.ofNullable(pleaUpdated.getUpdatedDate()).orElseGet(() -> envelope.metadata().createdAt().orElse(null)));

        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());
        assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), is(pleaUpdated.getUpdatedDate()));
        assertThat(onlinePleaCaptor.getValue().getCaseId(), is(caseId));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getComeToCourt(), is(true));

        verify(onlinePleaDetailRepository).save(onlinePleaDetailCaptor.capture());
        assertThat(onlinePleaDetailCaptor.getValue().getPlea(), is(pleaUpdated.getPlea()));
        assertThat(onlinePleaDetailCaptor.getValue().getMitigation(), is(pleaUpdated.getMitigation()));
        assertThat(onlinePleaDetailCaptor.getValue().getNotGuiltyBecause(), is(pleaUpdated.getNotGuiltyBecause()));


    }

    @Test
    @Deprecated
    public void shouldUpdateGuiltyRequestHearingPleaOnlineLegacy() {
        final PleaUpdated pleaUpdated = givenPleaUpdatedWithPleaType(GUILTY_REQUEST_HEARING, clock.now());
        when(jsonObjectToObjectConverter.convert(payload, PleaUpdated.class)).thenReturn(pleaUpdated);
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        final Metadata metadata1 = metadataBuilder;
        listener.updatePlea(envelope);
        Metadata metadata = metadata1;

        verify(offence).setPlea(pleaUpdated.getPlea());
        verify(offence).setPleaMethod(pleaUpdated.getPleaMethod());
        verify(offence).setPleaDate(Optional.ofNullable(pleaUpdated.getUpdatedDate()).orElseGet(() -> envelope.metadata().createdAt().orElse(null)));

        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());
        assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), is(pleaUpdated.getUpdatedDate()));
        assertThat(onlinePleaCaptor.getValue().getCaseId(), is(caseId));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getComeToCourt(), is(true));

        verify(onlinePleaDetailRepository).save(onlinePleaDetailCaptor.capture());
        assertThat(onlinePleaDetailCaptor.getValue().getPlea(), is(pleaUpdated.getPlea()));
        assertThat(onlinePleaDetailCaptor.getValue().getMitigation(), is(pleaUpdated.getMitigation()));
        assertThat(onlinePleaDetailCaptor.getValue().getNotGuiltyBecause(), is(pleaUpdated.getNotGuiltyBecause()));


    }

    @Test
    public void shouldUpdateGuiltyRequestHearingPleaOnline() {
        final PleadedGuiltyCourtHearingRequested pleadedGuiltyCourtHearingRequested =
                new PleadedGuiltyCourtHearingRequested(caseId, defendantId, offenceId, ONLINE, "The dog ate my ticket", clock.now());
        assertExpectationsForPleadGuiltyCourtHearingRequested(pleadedGuiltyCourtHearingRequested);
    }

    @Test
    @Deprecated
    public void shouldUpdateNotGuiltyPleaOnlineWithUpdateDateLegacy() {
        final PleaUpdated pleaUpdated = new PleaUpdated(caseId, offenceId, PleaType.NOT_GUILTY,
                null, "I was not there, they are lying", ONLINE, clock.now());
        when(jsonObjectToObjectConverter.convert(payload, PleaUpdated.class)).thenReturn(pleaUpdated);
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        final Metadata metadata1 = metadataBuilder;
        listener.updatePlea(envelope);
        Metadata metadata = metadata1;

        verify(offence).setPlea(pleaUpdated.getPlea());
        verify(offence).setPleaMethod(pleaUpdated.getPleaMethod());
        verify(offence).setPleaDate(Optional.ofNullable(pleaUpdated.getUpdatedDate()).orElseGet(() -> envelope.metadata().createdAt().orElse(null)));

        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());
        assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), is(pleaUpdated.getUpdatedDate()));
        assertThat(onlinePleaCaptor.getValue().getCaseId(), is(caseId));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getComeToCourt(), is(true));

        verify(onlinePleaDetailRepository).save(onlinePleaDetailCaptor.capture());
        assertThat(onlinePleaDetailCaptor.getValue().getPlea(), is(pleaUpdated.getPlea()));
        assertThat(onlinePleaDetailCaptor.getValue().getMitigation(), is(pleaUpdated.getMitigation()));
        assertThat(onlinePleaDetailCaptor.getValue().getNotGuiltyBecause(), is(pleaUpdated.getNotGuiltyBecause()));


    }

    @Test
    public void shouldUpdateNotGuiltyPleaOnlineWithUpdateDate() {
        final PleadedNotGuilty pleadedNotGuilty = new PleadedNotGuilty(caseId, defendantId, offenceId, "I was not there, they are lying", clock.now(), ONLINE);
        assertExpectationsForPleadNotGuilty(pleadedNotGuilty);
    }

    @Test
    @Deprecated
    public void shouldUpdateByPostLegacy() {
        final PleaUpdated pleaUpdated = new PleaUpdated(caseId, offenceId, PleaType.GUILTY,
                null, null, POSTAL, clock.now());
        when(jsonObjectToObjectConverter.convert(payload, PleaUpdated.class)).thenReturn(pleaUpdated);
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        final Metadata metadata1 = metadataBuilder;
        listener.updatePlea(envelope);
        Metadata metadata = metadata1;

        verify(offence).setPlea(pleaUpdated.getPlea());
        verify(offence).setPleaMethod(pleaUpdated.getPleaMethod());
        verify(offence).setPleaDate(Optional.ofNullable(pleaUpdated.getUpdatedDate()).orElseGet(() -> envelope.metadata().createdAt().orElse(null)));

        verify(onlinePleaRepository, never()).saveOnlinePlea(onlinePleaCaptor.capture());
        verify(onlinePleaDetailRepository, never()).save(onlinePleaDetailCaptor.capture());
    }

    @Test
    public void shouldUpdateByPost() {
        final PleadedGuilty pleadedGuilty = new PleadedGuilty(caseId, defendantId, offenceId, POSTAL, "The dog ate my ticket", clock.now());
        final PleaType plea = GUILTY;
        when(jsonObjectToObjectConverter.convert(payload, PleadedGuilty.class)).thenReturn(pleadedGuilty);
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        listener.updateOffenceDetailsWithPleadedGuilty(envelope);

        verify(offence).setPlea(plea);
        verify(offence).setPleaMethod(pleadedGuilty.getMethod());
        verify(offence).setPleaDate(pleadedGuilty.getPleadDate());
        assertOnlinePlea(plea, false, false);
    }

    @Test
    public void shouldCancelPlea() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, PleaCancelled.class)).thenReturn(pleaCancelled);
        when(pleaCancelled.getOffenceId()).thenReturn(offenceId);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);

        listener.cancelPlea(envelope);

        verify(offence).setPlea(null);
        verify(offence).setPleaMethod(null);
        verify(offence).setPleaDate(null);
    }

    @Test
    public void shouldCancelVerdict() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, VerdictCancelled.class)).thenReturn(verdictCancelled);
        when(verdictCancelled.getOffenceId()).thenReturn(offenceId);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);

        listener.cancelVerdict(envelope);

        verify(offence).setConviction(null);
        verify(offence).setConvictionDate(null);
    }

    @Test
    public void requestOffenceWithdrawal() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final OffenceWithdrawalRequested offenceWithdrawalRequested = new OffenceWithdrawalRequested(caseId, offenceId, randomUUID(), randomUUID(), ZonedDateTime.now());
        when(jsonObjectToObjectConverter.convert(payload, OffenceWithdrawalRequested.class)).thenReturn(offenceWithdrawalRequested);
        when(offenceRepository.findBy(offenceWithdrawalRequested.getOffenceId())).thenReturn(offence);
        listener.requestOffenceWithdrawal(envelope);
        verify(offence).setWithdrawalRequestReasonId(offenceWithdrawalRequested.getWithdrawalRequestReasonId());
    }

    @Test
    public void cancelOffenceWithdrawal() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final OffenceWithdrawalRequestCancelled offenceWithdrawalRequestCancelled = new OffenceWithdrawalRequestCancelled(caseId, offenceId, randomUUID(), ZonedDateTime.now());
        when(jsonObjectToObjectConverter.convert(payload, OffenceWithdrawalRequestCancelled.class)).thenReturn(offenceWithdrawalRequestCancelled);
        when(offenceRepository.findBy(offenceWithdrawalRequestCancelled.getOffenceId())).thenReturn(offence);
        listener.cancelOffenceWithdrawal(envelope);
        verify(offence).setWithdrawalRequestReasonId(null);
    }

    @Test
    public void offenceWithdrawalReasonChange() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        final OffenceWithdrawalRequestReasonChanged offenceWithdrawalRequestReasonChanged = new OffenceWithdrawalRequestReasonChanged(caseId, offenceId, randomUUID(),
                ZonedDateTime.now(), randomUUID(), randomUUID());
        when(jsonObjectToObjectConverter.convert(payload, OffenceWithdrawalRequestReasonChanged.class)).thenReturn(offenceWithdrawalRequestReasonChanged);
        when(offenceRepository.findBy(offenceWithdrawalRequestReasonChanged.getOffenceId())).thenReturn(offence);
        listener.offenceWithdrawalReasonChange(envelope);
        verify(offence).setWithdrawalRequestReasonId(offenceWithdrawalRequestReasonChanged.getNewWithdrawalRequestReasonId());
    }

    private PleaUpdated givenPleaUpdatedWithPleaType(PleaType pleaType, ZonedDateTime updatedDate) {
        return new PleaUpdated(caseId, offenceId, pleaType,
                "It was an accident", null, ONLINE, updatedDate);
    }

    private void assertExpectationsForPleadGuiltyCourtHearingRequested(final PleadedGuiltyCourtHearingRequested pleadedGuiltyCourtHearingRequested) {
        final PleaType plea = GUILTY_REQUEST_HEARING;
        when(jsonObjectToObjectConverter.convert(payload, PleadedGuiltyCourtHearingRequested.class)).thenReturn(pleadedGuiltyCourtHearingRequested);
        whenPleadGuiltyCourtHearingRequestedIsInvoked();

        verify(offence).setPlea(plea);
        verify(offence).setPleaMethod(pleadedGuiltyCourtHearingRequested.getMethod());
        verify(offence).setPleaDate(pleadedGuiltyCourtHearingRequested.getPleadDate());
        assertOnlinePlea(plea, true, true);
    }

    private void assertExpectationsForPleadNotGuilty(final PleadedNotGuilty pleadedNotGuilty) {
        final PleaType plea = NOT_GUILTY;
        when(jsonObjectToObjectConverter.convert(payload, PleadedNotGuilty.class)).thenReturn(pleadedNotGuilty);
        whenPleadNotGuiltyIsInvoked();

        verify(offence).setPlea(plea);
        verify(offence).setPleaMethod(pleadedNotGuilty.getMethod());
        assertOnlinePlea(plea, true, true);
    }

    private void assertOnlinePlea(final PleaType plea, final boolean onlinePlea, final boolean comeToCourt) {
        if (onlinePlea) {
            verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());
            verify(onlinePleaDetailRepository).save(onlinePleaDetailCaptor.capture());
            assertThat(onlinePleaCaptor.getValue().getCaseId(), is(caseId));
            assertThat(onlinePleaCaptor.getValue().getPleaDetails().getComeToCourt(), is(comeToCourt));
            assertThat(onlinePleaDetailCaptor.getValue().getPlea(), is(plea));
        } else {
            verify(onlinePleaRepository, never()).saveOnlinePlea(onlinePleaCaptor.capture());
            verify(onlinePleaDetailRepository, never()).save(onlinePleaDetailCaptor.capture());
        }
    }

    private void whenPleadGuiltyCourtHearingRequestedIsInvoked() {
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        listener.updateOffenceDetailsWithPleadedGuiltyCourtHearingRequested(envelope);
    }

    private void whenPleadNotGuiltyIsInvoked() {
        Metadata metadataBuilder = MetadataBuilderFactory.metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(envelope.metadata()).thenReturn(metadataBuilder);
        listener.updateOffenceDetailsWithPleadedNotGuilty(envelope);
    }

}
