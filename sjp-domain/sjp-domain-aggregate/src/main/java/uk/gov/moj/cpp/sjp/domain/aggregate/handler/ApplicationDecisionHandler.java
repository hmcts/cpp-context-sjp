package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.time.ZonedDateTime.now;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationDecision.applicationDecision;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.REOPENING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionRejected.applicationDecisionRejected;
import static uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved.applicationDecisionSaved;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.commands.SaveApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.VerdictCancelled;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class ApplicationDecisionHandler {

    public static final ApplicationDecisionHandler INSTANCE = new ApplicationDecisionHandler();

    private ApplicationDecisionHandler() {
    }

    public Stream<Object> saveApplicationDecision(final SaveApplicationDecision applicationDecisionCommand,
                                                  final CaseAggregateState state,
                                                  final Session session) {

        final List<String> validationErrors = validateApplicationDecision(applicationDecisionCommand, state);
        if(!validationErrors.isEmpty()) {
            return Stream.of(applicationDecisionRejected()
                    .withCaseId(state.getCaseId())
                    .withSessionId(session.getId())
                    .withApplicationId(applicationDecisionCommand.getApplicationId())
                    .withApplicationDecision(toApplicationDecision(applicationDecisionCommand))
                    .withRejectionReasons(validationErrors)
                    .build()
            );
        } else {
            final Stream.Builder streamBuilder = Stream.builder();
            addApplicationDecisionSavedEvent(applicationDecisionCommand, session, streamBuilder);
            addApplicationStatusChangedEvent(applicationDecisionCommand.getGranted(), state, streamBuilder);
            if(applicationDecisionCommand.getGranted()) {
                addPleaCancelledEvents(state, streamBuilder);
                addVerdictCancelledEvents(state, streamBuilder);
                addApplicationDecisionSetAside(state.getCaseId(), applicationDecisionCommand.getApplicationId(), streamBuilder);
            } else {
                addCaseUnassignedEvent(state, streamBuilder);
                addCaseCompletedEvent(state, streamBuilder);
            }
            return streamBuilder.build();
        }
    }

    private void addCaseUnassignedEvent(final CaseAggregateState state, final Stream.Builder streamBuilder) {
        streamBuilder.add(new CaseUnassigned(state.getCaseId()));
    }

    private void addCaseCompletedEvent(final CaseAggregateState state, final Stream.Builder streamBuilder) {
        streamBuilder.add(new CaseCompleted(state.getCaseId(), state.getSessionIds()));
    }

    private void addApplicationDecisionSavedEvent(final SaveApplicationDecision applicationDecisionCommand, final Session session, final Stream.Builder streamBuilder) {
        streamBuilder.add(buildApplicationDecisionSavedEvent(applicationDecisionCommand, session));
    }

    private void addApplicationDecisionSetAside(final UUID caseId, final UUID applicationId, final Stream.Builder streamBuilder) {
        streamBuilder.add(new ApplicationDecisionSetAside(applicationId, caseId));
    }

    private void addApplicationStatusChangedEvent(final boolean granted, final CaseAggregateState state, final Stream.Builder streamBuilder) {
        final ApplicationType applicationType = state.getCurrentApplication().getType();
        ApplicationStatus applicationStatus = null;
        if(applicationType.equals(STAT_DEC)) {
            applicationStatus = granted ? STATUTORY_DECLARATION_GRANTED : STATUTORY_DECLARATION_REFUSED;
        } else if(applicationType.equals(REOPENING)) {
            applicationStatus = granted ? REOPENING_GRANTED : REOPENING_REFUSED;
        }

        ofNullable(applicationStatus).ifPresent(status ->
                streamBuilder.add(new ApplicationStatusChanged(
                        state.getCurrentApplication().getApplicationId(),
                        status
                )));
    }

    private void addVerdictCancelledEvents(final CaseAggregateState state, final Stream.Builder streamBuilder) {
        state.getOffencesWithConviction()
                .forEach(offenceId -> streamBuilder.add(new VerdictCancelled(offenceId)));
    }

    private void addPleaCancelledEvents(final CaseAggregateState state, final Stream.Builder streamBuilder) {
        state.getPleas().forEach(
                plea -> streamBuilder.add(new PleaCancelled(state.getCaseId(), plea.getOffenceId(), plea.getDefendantId()))
        );
    }

    private ApplicationDecisionSaved buildApplicationDecisionSavedEvent(final SaveApplicationDecision applicationDecisionCommand, final Session session) {
        return applicationDecisionSaved()
                .withApplicationId(applicationDecisionCommand.getApplicationId())
                .withCaseId(applicationDecisionCommand.getCaseId())
                .withDecisionId(randomUUID())
                .withSessionId(session.getId())
                .withApplicationDecision(toApplicationDecision(applicationDecisionCommand))
                .withSavedAt(now())
                .withSavedBy(applicationDecisionCommand.getSavedBy())
                .build();
    }

    private ApplicationDecision toApplicationDecision(final SaveApplicationDecision applicationDecisionCommand) {
        return applicationDecision()
                .withGranted(applicationDecisionCommand.getGranted())
                .withOutOfTime(applicationDecisionCommand.getOutOfTime())
                .withOutOfTimeReason(applicationDecisionCommand.getOutOfTimeReason())
                .withRejectionReason(applicationDecisionCommand.getRejectionReason())
                .build();
    }


    private List<String> validateApplicationDecision(final SaveApplicationDecision applicationDecision, final CaseAggregateState state) {
        final List<String> rejectionReasons = new LinkedList<>();
        validatePendingApplication(state, rejectionReasons);
        validateDecisionIsForCurrentApplication(applicationDecision, state, rejectionReasons);
        validateOutOfTime(applicationDecision, rejectionReasons);
        validateRejectionReason(applicationDecision, rejectionReasons);
        return rejectionReasons;
    }

    private void validateRejectionReason(final SaveApplicationDecision applicationDecision, final List<String> rejectionReasons) {
        final Boolean granted = applicationDecision.getGranted();
        final String rejectionReason = applicationDecision.getRejectionReason();
        if(!granted && isBlank(rejectionReason)) {
            rejectionReasons.add("Rejected application must have rejection reason");
        }
    }

    private void validateOutOfTime(final SaveApplicationDecision applicationDecision, final List<String> rejectionReasons) {
        final Boolean granted = applicationDecision.getGranted();
        final Boolean outOfTime = applicationDecision.getOutOfTime();
        final String outOfTimeReason = applicationDecision.getOutOfTimeReason();
        if(granted && outOfTime !=null && outOfTime && isBlank(outOfTimeReason)) {
            rejectionReasons.add("Application decision out of time must have reason");
        }
    }

    private void validateDecisionIsForCurrentApplication(final SaveApplicationDecision applicationDecision,
                                                         final CaseAggregateState state,
                                                         final List<String> rejectionReasons) {
        ofNullable(state.getCurrentApplication())
                .map(Application::getApplicationId)
                .ifPresent(currentApplicationId -> {
                    if(!applicationDecision.getApplicationId().equals(currentApplicationId)) {
                        rejectionReasons.add("The current application is not the same as the one in the decision");
                    }
                });
    }

    private void validatePendingApplication(final CaseAggregateState state, final List<String> rejectionReasons) {
        if(!state.hasPendingApplication()){
            rejectionReasons.add("The case doesn't have any pending application");
        }
    }
}
