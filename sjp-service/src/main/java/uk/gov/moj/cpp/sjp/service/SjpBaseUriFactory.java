package uk.gov.moj.cpp.sjp.service;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Specializes;
import javax.inject.Inject;

import uk.gov.justice.services.clients.core.EndpointDefinition;
import uk.gov.justice.services.clients.core.webclient.BaseUriFactory;
import uk.gov.justice.services.clients.core.webclient.MockServerPortProvider;

/** Routes legacy access-control clients to the Users and Groups service. */
@ApplicationScoped
@Specializes
public class SjpBaseUriFactory extends BaseUriFactory {

    private static final String LEGACY_QUERY_PREFIX = "http://localhost:8080/usersgroups-query-api/";
    private static final String DEFAULT_BASE_URI = "http://usersgroups-service-wildfly-app/usersgroups-service/";

    @Inject
    MockServerPortProvider usersGroupsMockServerPortProvider;

    @Override
    public String createBaseUri(final EndpointDefinition definition) {
        final String baseUri = definition.getBaseUri();
        if (baseUri.startsWith(LEGACY_QUERY_PREFIX)
                && !usersGroupsMockServerPortProvider.getMockServerPort(definition).isPresent()) {
            final String configured = System.getProperty("usersgroups.query.baseUri", DEFAULT_BASE_URI);
            final String root = configured.endsWith("/") ? configured : configured + "/";
            return root + baseUri.substring(LEGACY_QUERY_PREFIX.length());
        }
        return super.createBaseUri(definition);
    }
}
