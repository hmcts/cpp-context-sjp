package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.json.schemas.domains.sjp.commands.CreateCaseApplication;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateCaseApplicationHandler extends CaseCommandHandler {

    private static final String COMMAND_NAME = "sjp.command.handler.create-case-application";


    @Handles(COMMAND_NAME)
    public void createCaseApplication(final Envelope<CreateCaseApplication> createCaseApplicationCommand) throws EventStreamException {

        final CreateCaseApplication createCaseApplicationRequest = createCaseApplicationCommand.payload();
        applyToCaseAggregate(createCaseApplicationRequest.getCaseId(), createCaseApplicationCommand, caseAggregate ->
                caseAggregate.createCaseApplication(
                        createCaseApplicationRequest
                ));
    }
}

