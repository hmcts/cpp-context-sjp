package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.mockito.InjectMocks;
import org.mockito.verification.VerificationMode;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

public class UpdateHearingRequirementsHandlerTest extends CaseCommandHandlerTest {

    private static final String FRENCH = "French";

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @InjectMocks
    private UpdateHearingRequirementsHandler updateHearingRequirementsHandler;

    @BeforeEach
    void setUp() {
        super.setupMocks();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
    }

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(FRENCH, true),
                Arguments.of(FRENCH, false),
                Arguments.of(FRENCH, null),
                Arguments.of("", true),
                Arguments.of("", false),
                Arguments.of("", null),
                Arguments.of(null, true),
                Arguments.of(null, false),
                Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void whenUpdateHearingRequirements(String interpreterLanguage, Boolean speakWelsh) throws EventStreamException {
        var defendantId = UUID.randomUUID();
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(userId.toString()));
        when(metadata.name()).thenReturn(ACTION_NAME);

        when(jsonObject.getString("defendantId")).thenReturn(defendantId.toString());
        when(jsonObject.getString("interpreterLanguage", null)).thenReturn(interpreterLanguage);
        when(jsonObject.containsKey("speakWelsh")).thenReturn(speakWelsh != null);

        Optional.ofNullable(speakWelsh)
                .ifPresent(nonNullSpeakWelsh -> when(jsonObject.getBoolean("speakWelsh")).thenReturn(nonNullSpeakWelsh));

        when(caseAggregate.updateHearingRequirements(userId, defendantId, interpreterLanguage, speakWelsh)).thenReturn(events);

        // WHEN
        updateHearingRequirementsHandler.updateHearingRequirements(jsonEnvelope);

        // THEN updateHearingRequirements called with expected Parameters
        verify(caseAggregate).updateHearingRequirements(userId, defendantId, interpreterLanguage, speakWelsh);

        verify(jsonObject).getString("defendantId");
        verify(jsonObject).getString("interpreterLanguage", null);
        verify(jsonObject).containsKey("speakWelsh");

        final VerificationMode getSpeakWelshBoolean = speakWelsh == null ? never() : times(1);
        verify(jsonObject, getSpeakWelshBoolean).getBoolean("speakWelsh");
    }
}
