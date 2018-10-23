package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;

import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class CaseLanguageHandler {

    public static final CaseLanguageHandler INSTANCE = new CaseLanguageHandler();

    private CaseLanguageHandler() {
    }

    public Stream<Object> updateHearingRequirements(final UUID userId,
                                                    final UUID defendantId,
                                                    final String interpreterLanguage,
                                                    final Boolean speakWelsh,
                                                    final CaseAggregateState state) {

        return createRejectionEvents(
                userId,
                "Update hearing requirements",
                defendantId,
                state
        ).orElse(updateHearingRequirementsForPostalPlea(defendantId, interpreterLanguage, speakWelsh, state));
    }


    private Stream<Object> updateHearingRequirementsForPostalPlea(final UUID defendantId,
                                                                  final String interpreterLanguage,
                                                                  final Boolean speakWelsh,
                                                                  final CaseAggregateState state) {

        return updateHearingRequirements(false, null, defendantId, interpreterLanguage, speakWelsh, state);
    }

    public Stream<Object> updateHearingRequirements(final boolean updatedByOnlinePlea,
                                                    final ZonedDateTime createdOn,
                                                    final UUID defendantId,
                                                    final String interpreterLanguage,
                                                    final Boolean speakWelsh,
                                                    final CaseAggregateState state) {

        return Stream.of(
                updateInterpreterLanguage(interpreterLanguage, defendantId, updatedByOnlinePlea, createdOn, state),
                updateSpeakWelsh(speakWelsh, defendantId, updatedByOnlinePlea, createdOn, state))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Optional<Object> updateInterpreterLanguage(final String newInterpreterLanguage,
                                                       final UUID defendantId,
                                                       final boolean updatedByOnlinePlea,
                                                       final ZonedDateTime createdOn,
                                                       final CaseAggregateState state) {

        final Object event;
        final Interpreter interpreter = Interpreter.of(newInterpreterLanguage);

        if (!Objects.equals(state.getDefendantInterpreterLanguage(defendantId), interpreter.getLanguage())) {
            if (interpreter.isNeeded() && updatedByOnlinePlea) {
                event = InterpreterUpdatedForDefendant.createEventForOnlinePlea(state.getCaseId(), defendantId, newInterpreterLanguage, createdOn);
            } else if (interpreter.isNeeded()) {
                event = InterpreterUpdatedForDefendant.createEvent(state.getCaseId(), defendantId, newInterpreterLanguage);
            } else {
                event = new InterpreterCancelledForDefendant(state.getCaseId(), defendantId);
            }
        } else {
            event = null;
        }

        return Optional.ofNullable(event);
    }

    private Optional<Object> updateSpeakWelsh(final Boolean newSpeakWelsh,
                                              final UUID defendantId,
                                              final boolean updatedByOnlinePlea,
                                              final ZonedDateTime createdOn,
                                              final CaseAggregateState state) {

        Object event = null;

        if (!Objects.equals(state.defendantSpeakWelsh(defendantId), newSpeakWelsh)) {
            if (newSpeakWelsh != null && updatedByOnlinePlea) {
                event = HearingLanguagePreferenceUpdatedForDefendant.createEventForOnlinePlea(state.getCaseId(), defendantId, newSpeakWelsh, createdOn);
            } else if (newSpeakWelsh != null) {
                event = HearingLanguagePreferenceUpdatedForDefendant.createEvent(state.getCaseId(), defendantId, newSpeakWelsh);
            } else {
                event = new HearingLanguagePreferenceCancelledForDefendant(state.getCaseId(), defendantId);
            }
        }

        return Optional.ofNullable(event);
    }

}
