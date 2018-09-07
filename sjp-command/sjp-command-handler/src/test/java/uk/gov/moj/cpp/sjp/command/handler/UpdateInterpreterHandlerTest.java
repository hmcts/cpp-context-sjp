package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.util.Collection;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;

@RunWith(Parameterized.class)
public class UpdateInterpreterHandlerTest extends CaseCommandHandlerTest {

    private static final String FRENCH = "French";

    private UUID defendantId;

    @InjectMocks
    private UpdateInterpreterHandler updateInterpreterHandler;

    @Parameter
    public String interpreterLanguage;

    @Parameters(name = "updateHearingRequirements() handles interpreterLanguage={0}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {FRENCH},
                {""},
                {null}
        });
    }

    @Before
    public void prepareJsonEnvelope() {
        // GIVEN
        defendantId = UUID.randomUUID();

        when(jsonObject.getString("defendantId")).thenReturn(defendantId.toString());
        when(jsonObject.getString("language", null)).thenReturn(interpreterLanguage);

        when(caseAggregate.updateInterpreter(userId, defendantId, interpreterLanguage)).thenReturn(events);
    }

    @Test
    public void whenUpdateInterpreter() throws EventStreamException {
        // WHEN
        updateInterpreterHandler.updateInterpreter(jsonEnvelope);

        // THEN updateInterpreter called with expected Parameters
        verify(caseAggregate).updateInterpreter(userId, defendantId, interpreterLanguage);
    }

    @After
    public void thenVerifyMockCalls() {
        // THEN
        verify(jsonObject).getString("defendantId");
        verify(jsonObject).getString("language", null);
    }

}
