package uk.gov.moj.sjp.it.framework.util;

import static uk.gov.justice.services.jmx.system.command.client.connection.JmxParametersBuilder.jmxParameters;
import static uk.gov.justice.services.test.utils.common.host.TestHostProvider.getHost;

import uk.gov.justice.services.jmx.api.command.CatchupCommand;
import uk.gov.justice.services.jmx.api.command.IndexerCatchupCommand;
import uk.gov.justice.services.jmx.api.command.RebuildCommand;
import uk.gov.justice.services.jmx.api.command.ShutterSystemCommand;
import uk.gov.justice.services.jmx.api.command.SystemCommand;
import uk.gov.justice.services.jmx.api.command.UnshutterSystemCommand;
import uk.gov.justice.services.jmx.system.command.client.SystemCommanderClient;
import uk.gov.justice.services.jmx.system.command.client.TestSystemCommanderClientFactory;
import uk.gov.justice.services.jmx.system.command.client.connection.JmxParameters;

public class SystemCommandInvoker {

    private static final String HOST = getHost();
    private static final int JMX_PORT = 9990;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String CONTEXT_NAME = "sjp-service";

    private final TestSystemCommanderClientFactory testSystemCommanderClientFactory = new TestSystemCommanderClientFactory();

    public void invokeRebuild() {
        invokeSystemCommand(new RebuildCommand());
    }

    public void invokeCatchup() {
        invokeSystemCommand(new CatchupCommand());
    }


    public void invokeIndexerCatchup() {
        invokeSystemCommand(new IndexerCatchupCommand());
    }

    public void invokeShutter() {
        invokeSystemCommand(new ShutterSystemCommand());
    }

    public void invokeUnshutter() {
        invokeSystemCommand(new UnshutterSystemCommand());
    }

    private JmxParameters jmxParameter() {
        return jmxParameters()
                .withContextName(CONTEXT_NAME)
                .withHost(HOST)
                .withPort(JMX_PORT)
                .withUsername(USERNAME)
                .withPassword(PASSWORD)
                .build();
    }

    private void invokeSystemCommand(final SystemCommand systemCommand) {
        try (final SystemCommanderClient systemCommanderClient = testSystemCommanderClientFactory.create(jmxParameter())) {
            systemCommanderClient.getRemote(CONTEXT_NAME).call(systemCommand);
        }
    }
}
