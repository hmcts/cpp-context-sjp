package uk.gov.moj.cpp.sjp.service;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Optional;

import javax.enterprise.inject.Produces;

import com.sun.net.httpserver.HttpServer;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.clients.core.EndpointDefinition;
import uk.gov.justice.services.clients.core.webclient.BaseUriFactory;
import uk.gov.justice.services.clients.core.webclient.MockServerPortProvider;
import uk.gov.justice.services.common.rest.ServerPortProvider;

class SjpBaseUriFactoryTest {

    private static final String LEGACY_QUERY = "http://localhost:8080/usersgroups-query-api/query/api/rest/usersgroups";
    private String originalBaseUri;
    private String originalDefaultPort;

    @BeforeEach
    void saveProperties() {
        originalBaseUri = System.getProperty("usersgroups.query.baseUri");
        originalDefaultPort = System.getProperty("DEFAULT_PORT");
        System.clearProperty("usersgroups.query.baseUri");
        System.clearProperty("DEFAULT_PORT");
    }

    @AfterEach
    void restoreProperties() {
        restore("usersgroups.query.baseUri", originalBaseUri);
        restore("DEFAULT_PORT", originalDefaultPort);
    }

    @Test
    void shouldSelectSpecializedFactoryAndReachCombinedWarRoute() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/usersgroups-service/query/api/rest/usersgroups/users/test/groups", exchange -> {
            assertEquals("test-user", exchange.getRequestHeaders().getFirst("CJSCPPUID"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();
        System.setProperty("usersgroups.query.baseUri", "http://localhost:" + server.getAddress().getPort() + "/usersgroups-service");
        try (WeldContainer container = container(true)) {
            final BaseUriFactory factory = container.select(BaseUriFactory.class).get();
            assertTrue(factory instanceof SjpBaseUriFactory);
            final HttpURLConnection connection = (HttpURLConnection) new URL(
                    factory.createBaseUri(endpoint(LEGACY_QUERY)) + "/users/test/groups").openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            connection.setRequestProperty("CJSCPPUID", "test-user");
            try {
                assertEquals(204, connection.getResponseCode());
            } finally {
                connection.disconnect();
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void shouldReproduceLegacyRouteWithoutSpecialization() {
        System.setProperty("usersgroups.query.baseUri", "http://usersgroups-service-wildfly-app/usersgroups-service/");
        try (WeldContainer container = container(false)) {
            assertEquals(LEGACY_QUERY, container.select(BaseUriFactory.class).get().createBaseUri(endpoint(LEGACY_QUERY)));
        }
    }

    @Test
    void shouldUseExistingUsersGroupsServiceInTheSameNamespace() {
        try (WeldContainer container = container(true)) {
            assertEquals("http://usersgroups-service-wildfly-app/usersgroups-service/query/api/rest/usersgroups",
                    container.select(BaseUriFactory.class).get().createBaseUri(endpoint(LEGACY_QUERY)));
        }
    }

    @Test
    void shouldRetainFrameworkMockRouting() {
        try (WeldContainer container = container(true)) {
            final EndpointDefinition definition = endpoint(LEGACY_QUERY);
            when(container.select(MockServerPortProvider.class).get().getMockServerPort(definition))
                    .thenReturn(Optional.of("9191"));
            assertEquals("http://localhost:9191/usersgroups-query-api/query/api/rest/usersgroups",
                    container.select(BaseUriFactory.class).get().createBaseUri(definition));
        }
    }

    @Test
    void shouldDelegateOtherContextsToFrameworkPortSelection() {
        System.setProperty("DEFAULT_PORT", "8180");
        try (WeldContainer container = container(true)) {
            assertEquals("http://localhost:8180/hearing-query-api/query/api/rest/hearing",
                    container.select(BaseUriFactory.class).get().createBaseUri(
                            endpoint("http://localhost:8080/hearing-query-api/query/api/rest/hearing")));
        }
    }

    @Test
    void shouldPreserveExplicitRemoteAndCombinedWarUrls() {
        try (WeldContainer container = container(true)) {
            final BaseUriFactory factory = container.select(BaseUriFactory.class).get();
            final String remote = "https://example.invalid/usersgroups-query-api/query/api/rest/usersgroups";
            final String combined = "http://localhost:9080/usersgroups-service/query/api/rest/usersgroups";
            assertEquals(remote, factory.createBaseUri(endpoint(remote)));
            assertEquals(combined, factory.createBaseUri(endpoint(combined)));
        }
    }

    private static EndpointDefinition endpoint(final String baseUri) {
        return new EndpointDefinition(baseUri, "", emptySet(), emptySet(), "application/json");
    }

    private static WeldContainer container(final boolean specialize) {
        final Weld weld = new Weld().disableDiscovery().addBeanClasses(BaseUriFactory.class, FrameworkDependencies.class);
        if (specialize) {
            weld.addBeanClass(SjpBaseUriFactory.class);
        }
        return weld.initialize();
    }

    private static void restore(final String name, final String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    public static class FrameworkDependencies {
        @Produces
        ServerPortProvider serverPortProvider() {
            return () -> System.getProperty("DEFAULT_PORT", "8080");
        }

        @Produces
        @javax.inject.Singleton
        MockServerPortProvider mockServerPortProvider() {
            final MockServerPortProvider provider = mock(MockServerPortProvider.class);
            when(provider.getMockServerPort(any())).thenReturn(Optional.empty());
            return provider;
        }
    }
}
