package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

@ServiceComponent(Component.COMMAND_HANDLER)
public class ResolveCaseStatusHandler extends CaseCommandHandler {

    private static final String COMMAND_NAME = "sjp.command.resolve-case-status";

    @Handles(COMMAND_NAME)
    public void resolveCaseStatus(final JsonEnvelope jsonEnvelope) throws EventStreamException {
        applyToCaseAggregate(jsonEnvelope, CaseAggregate::resolveCaseStatus);
    }
}