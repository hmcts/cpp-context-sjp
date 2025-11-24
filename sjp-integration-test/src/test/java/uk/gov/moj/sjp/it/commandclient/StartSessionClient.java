package uk.gov.moj.sjp.it.commandclient;

import uk.gov.moj.cpp.sjp.event.processor.SessionProcessor;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.sjp.it.util.commandclient.CommandClient;
import uk.gov.moj.sjp.it.util.commandclient.CommandExecutor;
import uk.gov.moj.sjp.it.util.commandclient.EventHandler;

import java.util.UUID;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@CommandClient(
        URI = "/sessions/{sessionId}",
        contentType = "application/vnd.sjp.start-session+json"
)
public class StartSessionClient {

    @JsonIgnore
    public UUID sessionId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String magistrate;

    public String courtHouseOUCode;

    @EventHandler(SessionProcessor.PUBLIC_SJP_SESSION_STARTED)
    public Consumer startedPublicHandler;

    @EventHandler(MagistrateSessionStarted.EVENT_NAME)
    public Consumer magistrateStartedHandler;

    @EventHandler(DelegatedPowersSessionStarted.EVENT_NAME)
    public Consumer delegatedPowersStartedHandler;

    public CommandExecutor getExecutor() {
        return new CommandExecutor(this);
    }

}
