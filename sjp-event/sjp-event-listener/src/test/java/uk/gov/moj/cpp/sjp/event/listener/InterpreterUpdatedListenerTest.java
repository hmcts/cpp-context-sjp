package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InterpreterUpdatedListenerTest {

    private UUID caseId = randomUUID();
    private UUID defendantId = randomUUID();

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseDetail caseDetail;

    @Mock
    private DefendantDetail defendant;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private OnlinePleaRepository.InterpreterLanguageOnlinePleaRepository onlinePleaRepository;

    @Mock
    private OnlinePlea onlinePlea;

    @InjectMocks
    private InterpreterUpdatedListener listener;

    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaCaptor;

    private Clock clock = new UtcClock();

    @BeforeEach
    public void setUp() {
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant()).thenReturn(defendant);
    }

    private JsonEnvelope commonSetupForInterpreter(final String language, final boolean updatedByOnlinePlea, final ZonedDateTime now) {
        final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant =
                updatedByOnlinePlea
                        ? InterpreterUpdatedForDefendant.createEventForOnlinePlea(caseId, defendantId, language, now)
                        : InterpreterUpdatedForDefendant.createEvent(caseId, defendantId, language);

        final JsonEnvelope envelope = JsonEnvelopeBuilder.envelope()
                .with(metadataWithRandomUUID("sjp.events.interpreter-for-defendant-updated"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId").withPayloadOf(
                        JsonObjects.createObjectBuilder().add("needed", true)
                                .add("language", language).build(), "interpreter")
                .build();

        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class))
                .thenReturn(interpreterUpdatedForDefendant);
        return envelope;
    }

    @Test
    public void shouldUpdateInterpreter() {
        final String language = "French";
        final ZonedDateTime now = clock.now();
        final JsonEnvelope envelope = commonSetupForInterpreter(language, false, now);

        listener.interpreterUpdated(envelope);

        final ArgumentCaptor<InterpreterDetail> captor = ArgumentCaptor.forClass(InterpreterDetail.class);

        verify(defendant).setInterpreter(captor.capture());
        verify(jsonObjectToObjectConverter).convert(envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class);
        verify(onlinePleaRepository, never()).saveOnlinePlea(any());

        assertThat(captor.getValue().getLanguage(), is(language));
        assertThat(captor.getValue().getNeeded(), is(Boolean.TRUE));
    }

    @Test
    public void shouldUpdateInterpreterForOnlinePlea() {
        final String language = "French";
        final ZonedDateTime now = clock.now();
        final JsonEnvelope envelope = commonSetupForInterpreter(language, true, now);

        listener.interpreterUpdated(envelope);

        final ArgumentCaptor<InterpreterDetail> captor = ArgumentCaptor.forClass(InterpreterDetail.class);

        verify(defendant).setInterpreter(captor.capture());
        verify(jsonObjectToObjectConverter).convert(envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class);
        verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());

        assertThat(captor.getValue().getLanguage(), is(language));
        assertThat(captor.getValue().getNeeded(), is(Boolean.TRUE));
        assertThat(onlinePleaCaptor.getValue().getCaseId(), equalTo(caseId));
        assertThat(onlinePleaCaptor.getValue().getPleaDetails().getInterpreterLanguage(), equalTo(language));
        assertThat(onlinePleaCaptor.getValue().getSubmittedOn(), equalTo(now));
    }

    @Test
    public void shouldCancelInterpreter() {

        final JsonEnvelope envelope = JsonEnvelopeBuilder.envelope()
                .with(metadataWithRandomUUID("sjp.events.interpreter-for-defendant-cancelled"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId")
                .build();

        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), InterpreterCancelledForDefendant.class)).thenReturn(
                new InterpreterCancelledForDefendant(caseId, defendantId));

        listener.interpreterCancelled(envelope);

        verify(defendant).setInterpreter(null);
    }

}
