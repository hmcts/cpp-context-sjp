package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
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

import java.time.temporal.ChronoUnit;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SessionQueryViewTest {

    private static final String TFL = "TFL";
    private static final String DVL = "DVL";
    public static final List<String> PROSECUTORS = Arrays.asList(TFL, DVL);
    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private CaseDecisionRepository caseDecisionRepository;

    @InjectMocks
    private SessionQueryView sessionQueryView;

    @Test
    public void shouldFindSessionForBackwardCompatibility() {

        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();
        final String courtHouseCode = "B01LY";
        final String courtHouseName = "Wimbledon Magistrates' Court";
        final String localJusticeAreaNationalCourtCode = "2577";
        final String magistrate = "John Smith";
        final ZonedDateTime startedAt = ZonedDateTime.now(UTC);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.session"))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .build();

        final Session session = new Session(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, magistrate, startedAt, null);

        when(sessionRepository.findBy(sessionId)).thenReturn(session);

        final JsonEnvelope result = sessionQueryView.findSession(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.session"),
                payload().isJson(allOf(
                        withJsonPath("$.sessionId", is(sessionId.toString())),
                        withJsonPath("$.userId", is(userId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", is(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", is(MAGISTRATE.name())),
                        withJsonPath("$.magistrate", is(magistrate)),
                        withJsonPath("$.startedAt", isSameMoment(startedAt.truncatedTo(ChronoUnit.MILLIS)))
                ))
        ));
    }

    @Test
    public void shouldFindSession() {

        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();
        final String courtHouseCode = "B01LY";
        final String courtHouseName = "Wimbledon Magistrates' Court";
        final String localJusticeAreaNationalCourtCode = "2577";
        final String magistrate = "John Smith";
        final ZonedDateTime startedAt = ZonedDateTime.now(UTC);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.session"))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .build();

        final Session session = new Session(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, magistrate, startedAt, PROSECUTORS);

        when(sessionRepository.findBy(sessionId)).thenReturn(session);

        final JsonEnvelope result = sessionQueryView.findSession(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.session"),
                payload().isJson(allOf(
                        withJsonPath("$.sessionId", is(sessionId.toString())),
                        withJsonPath("$.userId", is(userId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", is(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", is(MAGISTRATE.name())),
                        withJsonPath("$.magistrate", is(magistrate)),
                        withJsonPath("$.startedAt", isSameMoment(startedAt.truncatedTo(ChronoUnit.MILLIS))),
                        withJsonPath("$.prosecutors[0]", is(TFL)),
                        withJsonPath("$.prosecutors[1]", is(DVL))
                ))
        ));
    }

    @Test
    public void shouldHandlesQuery() {
        assertThat(SessionQueryView.class, isHandlerClass(Component.QUERY_VIEW)
                .with(method("findSession").thatHandles("sjp.query.session")));
    }

    @Test
    public void shouldFindConvictingCourtSessionForBackwardCompatibility() {
        final UUID offenceId = randomUUID();
        final UUID sessionIdToday = randomUUID();
        final UUID sessionId2DaysAgo = randomUUID();
        final UUID userId = randomUUID();
        final String courtHouseCode = "B01LY";
        final String courtHouseName = "Wimbledon Magistrates' Court";
        final String localJusticeAreaNationalCourtCode = "2577";
        final String magistrate = "John Smith";
        final ZonedDateTime startedAt = ZonedDateTime.now(UTC);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.convicting-court-session"))
                .withPayloadOf(offenceId.toString(), "offenceId")
                .build();

        final CaseDecision caseDecisionToday = new CaseDecision();
        final Session sessionToday = new Session(sessionIdToday, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, magistrate, startedAt, null);
        caseDecisionToday.setId(randomUUID());
        caseDecisionToday.setSession(sessionToday);
        caseDecisionToday.setSavedAt(ZonedDateTime.now());

        final CaseDecision caseDecision2DaysAgo = new CaseDecision();
        final Session session2DaysAgo = new Session(sessionId2DaysAgo, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, magistrate, startedAt, null);
        caseDecision2DaysAgo.setId(randomUUID());
        caseDecision2DaysAgo.setSession(session2DaysAgo);
        caseDecision2DaysAgo.setSavedAt(ZonedDateTime.now().minusDays(2));
        final List<CaseDecision> caseDecisions = Arrays.asList(caseDecisionToday, caseDecision2DaysAgo);

        when(caseDecisionRepository.findCaseDecisionsForConvictingCourtSessions(offenceId)).thenReturn(caseDecisions);

        final JsonEnvelope queryResponseEnvelope = sessionQueryView.findConvictingCourtSession(queryEnvelope);

        assertThat(queryResponseEnvelope, jsonEnvelope(metadata().withName("sjp.query.convicting-court-session"),
                payload().isJson(allOf(
                        withJsonPath("$.sessionId", is(sessionIdToday.toString())),
                        withJsonPath("$.userId", is(userId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(sessionToday.getCourtHouseCode())),
                        withJsonPath("$.courtHouseName", equalTo(sessionToday.getCourtHouseName())),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", is(sessionToday.getLocalJusticeAreaNationalCourtCode())),
                        withJsonPath("$.type", is(MAGISTRATE.name())),
                        withJsonPath("$.magistrate", is(sessionToday.getMagistrate().get())),
                        withJsonPath("$.startedAt", isSameMoment(sessionToday.getStartedAt().truncatedTo(ChronoUnit.MILLIS)))
                ))
        ));
    }

    @Test
    public void shouldFindConvictingCourtSession() {
        final UUID offenceId = randomUUID();
        final UUID sessionIdToday = randomUUID();
        final UUID sessionId2DaysAgo = randomUUID();
        final UUID userId = randomUUID();
        final String courtHouseCode = "B01LY";
        final String courtHouseName = "Wimbledon Magistrates' Court";
        final String localJusticeAreaNationalCourtCode = "2577";
        final String magistrate = "John Smith";
        final ZonedDateTime startedAt = ZonedDateTime.now(UTC);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.convicting-court-session"))
                .withPayloadOf(offenceId.toString(), "offenceId")
                .build();

        final CaseDecision caseDecisionToday = new CaseDecision();
        final Session sessionToday = new Session(sessionIdToday, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, magistrate, startedAt, PROSECUTORS);
        caseDecisionToday.setId(randomUUID());
        caseDecisionToday.setSession(sessionToday);
        caseDecisionToday.setSavedAt(ZonedDateTime.now());

        final CaseDecision caseDecision2DaysAgo = new CaseDecision();
        final Session session2DaysAgo = new Session(sessionId2DaysAgo, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, magistrate, startedAt, PROSECUTORS);
        caseDecision2DaysAgo.setId(randomUUID());
        caseDecision2DaysAgo.setSession(session2DaysAgo);
        caseDecision2DaysAgo.setSavedAt(ZonedDateTime.now().minusDays(2));
        final List<CaseDecision> caseDecisions = Arrays.asList(caseDecisionToday, caseDecision2DaysAgo);

        when(caseDecisionRepository.findCaseDecisionsForConvictingCourtSessions(offenceId)).thenReturn(caseDecisions);

        final JsonEnvelope queryResponseEnvelope = sessionQueryView.findConvictingCourtSession(queryEnvelope);

        assertThat(queryResponseEnvelope, jsonEnvelope(metadata().withName("sjp.query.convicting-court-session"),
                payload().isJson(allOf(
                        withJsonPath("$.sessionId", is(sessionIdToday.toString())),
                        withJsonPath("$.userId", is(userId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(sessionToday.getCourtHouseCode())),
                        withJsonPath("$.courtHouseName", equalTo(sessionToday.getCourtHouseName())),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", is(sessionToday.getLocalJusticeAreaNationalCourtCode())),
                        withJsonPath("$.type", is(MAGISTRATE.name())),
                        withJsonPath("$.magistrate", is(sessionToday.getMagistrate().get())),
                        withJsonPath("$.startedAt", isSameMoment(sessionToday.getStartedAt().truncatedTo(ChronoUnit.MILLIS))),
                        withJsonPath("$.prosecutors[0]", is(TFL)),
                        withJsonPath("$.prosecutors[1]", is(DVL))
                ))
        ));
    }

    @Test
    public void shouldFindLatestAocpSessionForBackwardCompatibility() {

        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();
        final String courtHouseCode = "B52CM00";
        final String courtHouseName = "Bristol Magistrates' Court";
        final String localJusticeAreaNationalCourtCode = "1450";
        final ZonedDateTime startedAt = ZonedDateTime.now(UTC);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.latest-aocp-session"))
                .build();

        final Session session = new Session(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, null, startedAt, null);

        when(sessionRepository.findLatestAocpSession()).thenReturn(asList(session));

        final JsonEnvelope result = sessionQueryView.getLatestAocpSession(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.latest-aocp-session"),
                payload().isJson(allOf(
                        withJsonPath("$.sessionId", is(sessionId.toString())),
                        withJsonPath("$.userId", is(userId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", is(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.startedAt", isSameMoment(startedAt.truncatedTo(ChronoUnit.MILLIS)))
                ))
        ));
    }

    @Test
    public void shouldFindLatestAocpSession() {

        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();
        final String courtHouseCode = "B52CM00";
        final String courtHouseName = "Bristol Magistrates' Court";
        final String localJusticeAreaNationalCourtCode = "1450";
        final ZonedDateTime startedAt = ZonedDateTime.now(UTC);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.latest-aocp-session"))
                .build();

        final Session session = new Session(sessionId, userId, courtHouseCode, courtHouseName, localJusticeAreaNationalCourtCode, null, startedAt, PROSECUTORS);

        when(sessionRepository.findLatestAocpSession()).thenReturn(asList(session));

        final JsonEnvelope result = sessionQueryView.getLatestAocpSession(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.latest-aocp-session"),
                payload().isJson(allOf(
                        withJsonPath("$.sessionId", is(sessionId.toString())),
                        withJsonPath("$.userId", is(userId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", is(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.startedAt", isSameMoment(startedAt.truncatedTo(ChronoUnit.MILLIS))),
                        withJsonPath("$.prosecutors[0]", is(TFL)),
                        withJsonPath("$.prosecutors[1]", is(DVL))
                ))
        ));
    }
}
