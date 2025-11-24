package uk.gov.moj.sjp.it.commandclient;

import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.util.commandclient.CommandClient;
import uk.gov.moj.sjp.it.util.commandclient.CommandExecutor;
import uk.gov.moj.sjp.it.util.commandclient.EventHandler;

import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;

@CommandClient(
        URI = "/cases/{caseId}/unassign",
        contentType = "application/vnd.sjp.unassign-case+json"
)
public class UnassignCaseClient {

    @JsonIgnore
    public UUID caseId;

    @EventHandler(CaseUnassigned.EVENT_NAME)
    public Consumer caseUnassignedHandler;

    public CommandExecutor getExecutor() {
        return new CommandExecutor(this);
    }

}
