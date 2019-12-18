package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.query.service.UsersGroupsService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionSessionDecoratorTest {

    private static final String CASE_ID = randomUUID().toString();
    private static final String DECISION_ID = randomUUID().toString();

    private static final UUID LEGAL_ADVISER_USER_ID = randomUUID();
    private static final String LEGAL_ADVISER_FIRST_NAME = "Erica";
    private static final String LEGAL_ADVISER_LAST_NAME = "Smith";

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private UsersGroupsService usersGroupsService;

    @InjectMocks
    private DecisionSessionDecorator decisionSessionDecorator;

    @Test
    public void shouldDecorateWithLegalAdviserName() {

        when(usersGroupsService.getUserDetails(LEGAL_ADVISER_USER_ID, jsonEnvelope)).thenReturn(buildLegalAdviserDetails());

        final JsonObject originalSession = buildOriginalSession();
        final JsonObject expectedDecoratedSession = buildExpectedSession(LEGAL_ADVISER_FIRST_NAME, LEGAL_ADVISER_LAST_NAME);

        final JsonObject expectedDecoratedCase = buildCaseWithDecisionSession(expectedDecoratedSession);

        final JsonObject originalCase = buildCaseWithDecisionSession(originalSession);

        final JsonObject actualDecoratedCase = decisionSessionDecorator.decorateWithLegalAdviserName(originalCase, jsonEnvelope);

        assertThat(actualDecoratedCase, equalTo(expectedDecoratedCase));
    }

    @Test
    public void shouldNotDecorateWithLegalAdviserNameIfNoDecisions() {
        final JsonObject expectedDecoratedCase = buildCaseWithNoDecisions();

        final JsonObject originalCase = buildCaseWithNoDecisions();

        final JsonObject actualDecoratedCase = decisionSessionDecorator.decorateWithLegalAdviserName(originalCase, jsonEnvelope);

        assertThat(actualDecoratedCase, equalTo(expectedDecoratedCase));
    }

    @Test
    public void shouldNotDecorateWithLegalAdviserNameIfNoSession() {
        final JsonObject expectedDecoratedCase = buildCaseWithDecisionNoSession();

        final JsonObject originalCase = buildCaseWithDecisionNoSession();

        final JsonObject actualDecoratedCase = decisionSessionDecorator.decorateWithLegalAdviserName(originalCase, jsonEnvelope);

        assertThat(actualDecoratedCase, equalTo(expectedDecoratedCase));
    }

    private JsonObject buildLegalAdviserDetails() {

        return createObjectBuilder()
                .add("firstName", LEGAL_ADVISER_FIRST_NAME)
                .add("lastName", LEGAL_ADVISER_LAST_NAME)
                .build();
    }

    private JsonObject buildCaseWithDecisionSession(final JsonObject session) {
        return createObjectBuilder()
                .add("id", CASE_ID)
                .add("caseDecisions", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", DECISION_ID)
                                .add("session", session)))
                .build();
    }

    private JsonObject buildCaseWithNoDecisions() {
        return createObjectBuilder()
                .add("id", CASE_ID)
                .build();
    }

    private JsonObject buildCaseWithDecisionNoSession() {
        return createObjectBuilder()
                .add("id", CASE_ID)
                .add("caseDecisions", Json.createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", DECISION_ID)))
                .build();
    }

    private JsonObject buildOriginalSession() {
        return createObjectBuilder()
                .add("legalAdviserUserId", LEGAL_ADVISER_USER_ID.toString())
                .build();
    }

    private JsonObject buildExpectedSession(final String legalAdviserFirstName, final String legalAdviserLastName) {
        return JsonObjects.createObjectBuilder(buildOriginalSession())
                .add("legalAdviser", Json.createObjectBuilder()
                        .add("id", LEGAL_ADVISER_USER_ID.toString())
                        .add("firstName", legalAdviserFirstName)
                        .add("lastName", legalAdviserLastName))
                .build();
    }
}
