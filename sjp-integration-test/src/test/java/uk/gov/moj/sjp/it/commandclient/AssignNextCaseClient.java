package uk.gov.moj.sjp.it.commandclient;

import static uk.gov.moj.sjp.it.util.commandclient.ListeningStrategy.UNTIL_RECEIVAL;
import static uk.gov.moj.sjp.it.util.commandclient.ListeningStrategy.UNTIL_TIMEOUT;

import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.cpp.sjp.event.session.CaseAssignmentRejected;
import uk.gov.moj.sjp.it.util.commandclient.CommandClient;
import uk.gov.moj.sjp.it.util.commandclient.CommandExecutor;
import uk.gov.moj.sjp.it.util.commandclient.EventHandler;
import uk.gov.moj.sjp.it.util.commandclient.ListenerConfig;

import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;

@Builder()
@CommandClient(
        URI = "/sessions/{sessionId}",
        contentType = "application/vnd.sjp.assign-next-case+json",
        listenerConfigs = {
                @ListenerConfig(key = CaseAssigned.EVENT_NAME, until = UNTIL_RECEIVAL, timeout = 8000),
                @ListenerConfig(key = CaseAssignmentRejected.EVENT_NAME, until = UNTIL_TIMEOUT, timeout = 11000)
        }
)
public class AssignNextCaseClient {

    @JsonIgnore
    public UUID sessionId;

    @EventHandler(AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNED)
    public Consumer assignedPublicHandler;

    @EventHandler(CaseAssigned.EVENT_NAME)
    public Consumer assignedPrivateHandler;

    @EventHandler(AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNMENT_REJECTED)
    public Consumer assignmentRejectedPublicHandler;

    @EventHandler(CaseAssignmentRejected.EVENT_NAME)
    public Consumer assignmentRejectedPrivateHandler;

    @EventHandler(AssignmentProcessor.PUBLIC_SJP_CASE_NOT_ASSIGNED)
    public Consumer notAssignedHandler;

    public CommandExecutor getExecutor() {
        return new CommandExecutor(this);
    }

}
