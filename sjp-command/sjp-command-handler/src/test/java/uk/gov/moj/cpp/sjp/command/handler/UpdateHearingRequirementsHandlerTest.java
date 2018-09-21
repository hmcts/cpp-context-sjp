package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.verification.VerificationMode;

@RunWith(Parameterized.class)
public class UpdateHearingRequirementsHandlerTest extends CaseCommandHandlerTest {

    private static final String FRENCH = "French";

    private UUID defendantId;

    @InjectMocks
    private UpdateHearingRequirementsHandler updateHearingRequirementsHandler;

    @Parameter(0)
    public String interpreterLanguage;

    @Parameter(1)
    public Boolean speakWelsh;

    @Parameters(name = "updateHearingRequirements() handles interpreterLanguage={0} and speakWelsh={1}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {FRENCH, true},
                {FRENCH, false},
                {FRENCH, null},
                {"", true},
                {"", false},
                {"", null},
                {null, true},
                {null, false},
                {null, null}
        });
    }

    @Before
    public void prepareJsonEnvelope() {
        // GIVEN
        defendantId = UUID.randomUUID();

        when(jsonObject.getString("defendantId")).thenReturn(defendantId.toString());
        when(jsonObject.getString("interpreterLanguage", null)).thenReturn(interpreterLanguage);
        when(jsonObject.containsKey("speakWelsh")).thenReturn(speakWelsh != null);

        Optional.ofNullable(speakWelsh)
                .ifPresent(nonNullSpeakWelsh -> when(jsonObject.getBoolean("speakWelsh")).thenReturn(nonNullSpeakWelsh));

        when(caseAggregate.updateHearingRequirements(userId, defendantId, interpreterLanguage, speakWelsh)).thenReturn(events);
    }

    @Test
    public void whenUpdateHearingRequirements() throws EventStreamException {
        // WHEN
        updateHearingRequirementsHandler.updateHearingRequirements(jsonEnvelope);

        // THEN updateHearingRequirements called with expected Parameters
        verify(caseAggregate).updateHearingRequirements(userId, defendantId, interpreterLanguage, speakWelsh);
    }

    @After
    public void thenVerifyMockCalls() {
        // THEN
        verify(jsonObject).getString("defendantId");
        verify(jsonObject).getString("interpreterLanguage", null);
        verify(jsonObject).containsKey("speakWelsh");

        final VerificationMode getSpeakWelshBoolean = speakWelsh == null ? never() : times(1);
        verify(jsonObject, getSpeakWelshBoolean).getBoolean("speakWelsh");
    }

}
