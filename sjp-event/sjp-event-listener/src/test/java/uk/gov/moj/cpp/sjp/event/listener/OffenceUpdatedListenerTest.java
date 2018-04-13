package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithDefaults;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffenceUpdatedListenerTest {

    private UUID offenceId = UUID.randomUUID();
    private UUID caseId = UUID.randomUUID();

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
    private OffenceDetail offence;

    @Mock
    private DefendantDetail defendant;

    @Mock
    private JsonObject payload;

    @Mock
    private CaseSearchResult searchResult;

    @Spy
    @InjectMocks
    private CaseSearchResultService caseSearchResultService = new CaseSearchResultService();

    @Spy
    private Clock clock = new StoppedClock(new UtcClock().now());

    @Mock
    private OnlinePleaRepository.PleaDetailsRepository onlinePleaRepository;

    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaCaptor;

    @Test
    public void shouldUpdateGuiltyPleaOnline() {
        final PleaUpdated pleaUpdated = new PleaUpdated(caseId.toString(), offenceId.toString(), Plea.Type.GUILTY.toString(),
                "It was an accident", null, PleaMethod.ONLINE, clock.now());
        assertExpectationsForPleaUpdate(true, true, pleaUpdated, false);
    }

    @Test
    public void shouldUpdateGuiltyRequestHearingPleaOnline() {
        final PleaUpdated pleaUpdated = new PleaUpdated(caseId.toString(), offenceId.toString(), Plea.Type.GUILTY_REQUEST_HEARING.toString(),
                "It was an accident", null, PleaMethod.ONLINE, clock.now());
        assertExpectationsForPleaUpdate(true, true, pleaUpdated, true);
    }

    @Test
    public void shouldUpdateNotGuiltyPleaOnlineWithUpdateDate() {
        final PleaUpdated pleaUpdated = new PleaUpdated(caseId.toString(), offenceId.toString(), Plea.Type.NOT_GUILTY.toString(),
                null, "I was not there, they are lying", PleaMethod.ONLINE, clock.now());
        assertExpectationsForPleaUpdate(true, true, pleaUpdated, true);
    }

    @Test
    public void shouldUpdateNotGuiltyPleaOnlineWithoutUpdateDate() {
        final PleaUpdated pleaUpdated = new PleaUpdated(caseId.toString(), offenceId.toString(), Plea.Type.NOT_GUILTY.toString(),
                null, "I was not there, they are lying", PleaMethod.ONLINE, null);
        assertExpectationsForPleaUpdate(true, false, pleaUpdated, true);
    }

    @Test
    public void shouldUpdateByPost() {
        final PleaUpdated pleaUpdated = new PleaUpdated(caseId.toString(), offenceId.toString(), Plea.Type.GUILTY.toString(),
                null, null, PleaMethod.POSTAL, clock.now());
        assertExpectationsForPleaUpdate(false, false, pleaUpdated, null);
    }

    private void assertExpectationsForPleaUpdate(boolean onlinePlea, boolean pleaUpdatedEventHasUpdatedDate, PleaUpdated pleaUpdated, Boolean comeToCourt) {
        Metadata metadataBuilder = metadataWithDefaults().build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(envelope.metadata().createdAt()).thenReturn(Optional.empty());
        when(jsonObjectToObjectConverter.convert(payload, PleaUpdated.class)).thenReturn(pleaUpdated);
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(searchResultRepository.findByCaseId(caseId)).thenReturn(asList(searchResult));
        when(envelope.metadata()).thenReturn(metadataBuilder);

        listener.updatePlea(envelope);

        verify(offence).setPlea(pleaUpdated.getPlea());
        verify(offence).setPleaMethod(pleaUpdated.getPleaMethod());
        //TODO: should use a fixed clock for 100% test reliability
        verify(searchResult).setPleaDate(now());

        if (onlinePlea) {
            verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());
            if (pleaUpdatedEventHasUpdatedDate) {
                assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), is(pleaUpdated.getUpdatedDate()));
            } else {
                assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), is(metadataBuilder.createdAt().get()));
            }
            assertThat(onlinePleaCaptor.getValue().getCaseId(), is(caseId));
            assertThat(onlinePleaCaptor.getValue().getPleaDetails().getPlea(), is(pleaUpdated.getPlea()));
            assertThat(onlinePleaCaptor.getValue().getPleaDetails().getComeToCourt(), is(comeToCourt));
            assertThat(onlinePleaCaptor.getValue().getPleaDetails().getMitigation(), is(pleaUpdated.getMitigation()));
            assertThat(onlinePleaCaptor.getValue().getPleaDetails().getNotGuiltyBecause(), is(pleaUpdated.getNotGuiltyBecause()));
        } else {
            verify(onlinePleaRepository, never()).saveOnlinePlea(onlinePleaCaptor.capture());
        }
    }

    @Test
    public void shouldCancelPlea() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, PleaCancelled.class)).thenReturn(pleaCancelled);
        when(pleaCancelled.getOffenceId()).thenReturn(offenceId.toString());
        when(offenceRepository.findBy(offenceId)).thenReturn(offence);
        when(pleaCancelled.getCaseId()).thenReturn(caseId.toString());
        when(offence.getDefendantDetail()).thenReturn(defendant);
        when(searchResultRepository.findByCaseId(caseId)).thenReturn(asList(searchResult));

        listener.cancelPlea(envelope);

        verify(offence).setPlea(null);
        verify(offence).setPleaMethod(null);
        verify(searchResult).setPleaDate(null);
    }
}
