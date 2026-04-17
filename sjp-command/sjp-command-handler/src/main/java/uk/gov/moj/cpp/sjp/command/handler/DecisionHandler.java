package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.json.schemas.domains.sjp.commands.SaveApplicationDecision;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.ApplicationOffencesResults;
import uk.gov.moj.cpp.sjp.domain.CaseCompleteBdf;
import uk.gov.moj.cpp.sjp.domain.GrantedApplicationResults;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class DecisionHandler extends CaseCommandHandler {
    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Handles("sjp.command.save-decision")
    public void saveDecision(final Envelope<Decision> command) throws EventStreamException {
        final UUID sessionId = command.payload().getSessionId();
        final EventStream sessionEventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(sessionEventStream, Session.class);
        applyToCaseAggregate(command.payload().getCaseId(), command, aCase -> aCase.saveDecision(command.payload(), session));
    }

    @Handles("sjp.command.handler.save-application-decision")
    public void saveApplicationDecision(final Envelope<SaveApplicationDecision> command) throws EventStreamException {
        final UUID sessionId = command.payload().getSessionId();
        final EventStream sessionEventStream = eventSource.getStreamById(sessionId);
        final Session session = aggregateService.get(sessionEventStream, Session.class);
        applyToCaseAggregate(command.payload().getCaseId(), command, aCase -> aCase.saveApplicationDecision(command.payload(), session));
    }

    @Handles("sjp.command.record-granted-application-results")
    public void recordGrantedApplicationResults(final Envelope<GrantedApplicationResults> command) throws EventStreamException {
        final GrantedApplicationResults payload = command.payload();
        final Optional<UUID> caseId = ofNullable(payload.getHearing().getCourtApplications())
                        .map(applications -> applications.get(0))
                            .filter(courtApplication -> nonNull(courtApplication.getCourtApplicationCases()))
                                .map(courtApplication -> courtApplication.getCourtApplicationCases().get(0))
                                        .map(CourtApplicationCase::getProsecutionCaseId);
        if(caseId.isPresent()) {
            applyToCaseAggregate(caseId.get(), command, aCase -> aCase.recordGrantedApplicationResults(payload));
        }
    }

    @Handles("sjp.command.save-application-offences-results")
    public void saveApplicationOffencesResults(final Envelope<ApplicationOffencesResults> command) throws EventStreamException {
        final ApplicationOffencesResults payload = command.payload();
        final Optional<UUID> caseId = ofNullable(payload.getHearing().getProsecutionCases())
                .map(prosecutionCases -> prosecutionCases.get(0))
                .map(ProsecutionCase::getId);
        if(caseId.isPresent()) {
            applyToCaseAggregate(caseId.get(), command, aCase -> aCase.saveApplicationOffencesResults(payload));
        }
    }


    @Handles("sjp.command.case-complete-bdf")
    public void caseCompleteBdf(final Envelope<CaseCompleteBdf> command) throws EventStreamException {
        final CaseCompleteBdf payload = command.payload();
        applyToCaseAggregate(payload.getCaseId(), command, CaseAggregate::caseCompletedBdf);
    }

}
