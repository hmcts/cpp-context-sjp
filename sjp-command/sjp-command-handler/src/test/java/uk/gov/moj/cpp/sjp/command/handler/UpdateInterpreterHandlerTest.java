package uk.gov.moj.cpp.sjp.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateInterpreterHandlerTest extends CaseCommandHandlerTest {

    @InjectMocks
    private UpdateInterpreterHandler updateInterpreterHandler;

    @Test
    public void shouldUpdateInterpreterWithLanguage() throws EventStreamException {
        final UUID defendantId = UUID.randomUUID();
        final String language = "fr";

        when(jsonObject.getString("defendantId")).thenReturn(defendantId.toString());
        when(jsonObject.getString("language", null)).thenReturn(language);

        when(caseAggregate.updateInterpreter(userId, defendantId, language)).thenReturn(events);

        updateInterpreterHandler.updateInterpreter(jsonEnvelope);

        verify(jsonObject).getString("defendantId");
        verify(jsonObject).getString("language", null);

        verify(caseAggregate).updateInterpreter(userId, defendantId, language);
    }

    @Test
    public void shouldUpdateInterpreterWithNullLanguageIfLanguageNotPresent() throws EventStreamException {
        final UUID defendantId = UUID.randomUUID();

        when(jsonObject.getString("defendantId")).thenReturn(defendantId.toString());
        when(jsonObject.getString("language", null)).thenReturn(null);

        when(caseAggregate.updateInterpreter(userId, defendantId, null)).thenReturn(events);

        updateInterpreterHandler.updateInterpreter(jsonEnvelope);

        verify(jsonObject).getString("defendantId");
        verify(jsonObject).getString("language", null);

        verify(caseAggregate).updateInterpreter(userId, defendantId, null);
    }
}
