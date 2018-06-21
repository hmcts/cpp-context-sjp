package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.query.view.matcher.ZonedDateTimeMatcher.isSameMoment;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionQueryViewTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SessionQueryView sessionQueryView;

    @Test
    public void shouldFindSession() {

        final UUID sessionId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();
        final String courtHouseName = "Wimbledon Magistrates' Court";
        final String localJusticeAreaNationalCourtCode = "2577";
        final String magistrate = "John Smith";
        final ZonedDateTime startedAt = ZonedDateTime.now(UTC);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.session"))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .build();

        final Session session = new Session(sessionId, userId, courtHouseName, localJusticeAreaNationalCourtCode, magistrate, startedAt);

        when(sessionRepository.findBy(sessionId)).thenReturn(session);

        final JsonEnvelope result = sessionQueryView.findSession(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.session"),
                payload().isJson(allOf(
                        withJsonPath("$.sessionId", is(sessionId.toString())),
                        withJsonPath("$.userId", is(userId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", is(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", is(MAGISTRATE.name())),
                        withJsonPath("$.magistrate", is(magistrate)),
                        withJsonPath("$.startedAt", isSameMoment(startedAt))
                ))
        ));
    }

    @Test
    public void shouldHandlesQuery() {
        Assert.assertThat(SessionQueryView.class, isHandlerClass(Component.QUERY_VIEW)
                .with(method("findSession").thatHandles("sjp.query.session")));
    }

}
