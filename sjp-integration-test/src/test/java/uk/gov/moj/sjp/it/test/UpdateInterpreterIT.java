package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithDecision;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.UpdateInterpreterHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdateInterpreterIT extends BaseIntegrationTest {

    private UpdateInterpreterHelper updateInterpreterHelper;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        updateInterpreterHelper = new UpdateInterpreterHelper();
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
    }

    @After
    public void tearDown() throws Exception {
        updateInterpreterHelper.close();
    }

    @Test
    public void shouldUpdateInterpreter() {
        stubGetCaseDecisionsWithNoDecision(createCasePayloadBuilder.getId());
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("french"));

        updateInterpreterHelper.pollForInterpreter(caseId, defendantId, "french");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("german"));

        updateInterpreterHelper.pollForInterpreter(caseId, defendantId, "german");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload(null));

        updateInterpreterHelper.pollForEmptyInterpreter(caseId, defendantId);
    }

    @Test
    public void shouldRejectInterpreterUpdateIfCaseIsAlreadyCompleted() {
        stubGetCaseDecisionsWithDecision(createCasePayloadBuilder.getId());

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("french"));

        final JsonEnvelope event = updateInterpreterHelper.getEventFromPublicTopic();
        assertThat(event, getCaseUpdateRejectedPublicEventMatcher(caseId, CaseUpdateRejected.RejectReason.CASE_COMPLETED.name()));
    }

    @Test
    public void shouldRejectInterpreterUpdateIfCaseIsAssignedToSomebodyElse() {
        stubGetCaseDecisionsWithNoDecision(createCasePayloadBuilder.getId());
        stubGetAssignmentsByDomainObjectId(createCasePayloadBuilder.getId(), randomUUID());

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("french"));

        final JsonEnvelope event = updateInterpreterHelper.getEventFromPublicTopic();
        assertThat(event, getCaseUpdateRejectedPublicEventMatcher(caseId, CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name()));
    }

    private Matcher getCaseUpdateRejectedPublicEventMatcher(final UUID caseId, final String reason) {
        final Matcher payloadMatcher = allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.reason", is(reason)));

        return jsonEnvelope()
                .withMetadataOf(metadata().withName("public.sjp.case-update-rejected"))
                .withPayloadOf(payloadIsJson(payloadMatcher));
    }

    private JsonObject updateInterpreterPayload(final String language) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        if (!StringUtils.isEmpty(language)) {
            objectBuilder.add("language", language);
        }
        return objectBuilder.build();
    }
}
