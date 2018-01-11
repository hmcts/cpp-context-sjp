package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import javax.json.Json;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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

    @InjectMocks
    private InterpreterUpdatedListener listener;

    @Before
    public void setUp() {
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant()).thenReturn(defendant);
    }

    @Test
    public void shouldUpdateInterpreter() {

        final String language = "French";

        final JsonEnvelope envelope = DefaultJsonEnvelope.envelope()
                .with(metadataWithRandomUUID("sjp.events.interpreter-for-defendant-updated"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId").withPayloadOf(
                        Json.createObjectBuilder().add("needed", true)
                                .add("language", language).build(), "interpreter")
                .build();

        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class)).thenReturn(
                new InterpreterUpdatedForDefendant(caseId, defendantId, new Interpreter(language)));

        listener.interpreterUpdated(envelope);

        final ArgumentCaptor<InterpreterDetail> captor =
                        ArgumentCaptor.forClass(InterpreterDetail.class);

        verify(defendant).setInterpreter(captor.capture());

        assertThat(captor.getValue().getLanguage(), is(language));
        assertThat(captor.getValue().getNeeded(), is(Boolean.TRUE));
    }

    @Test
    public void shouldCancelInterpreter() {

        final JsonEnvelope envelope = DefaultJsonEnvelope.envelope()
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
