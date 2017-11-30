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
import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.helper.UpdateInterpreterHelper;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdateInterpreterIT extends BaseIntegrationTest {

    private UpdateInterpreterHelper updateInterpreterHelper;
    private CaseSjpHelper caseSjpHelper;

    @Before
    public void setUp() {
        updateInterpreterHelper = new UpdateInterpreterHelper();
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.verifyCaseCreatedUsingId();
    }

    @After
    public void tearDown() throws Exception {
        caseSjpHelper.close();
        updateInterpreterHelper.close();
    }

    @Test
    public void shouldUpdateInterpreter() {
        stubGetCaseDecisionsWithNoDecision(caseSjpHelper.getCaseId());
        stubGetEmptyAssignmentsByDomainObjectId(caseSjpHelper.getCaseId());

        final String defendantId = caseSjpHelper.getSingleDefendantId();
        final String caseId = caseSjpHelper.getCaseId();

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("french"));

        updateInterpreterHelper.pollForInterpreter(caseId, defendantId, "french");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("german"));

        updateInterpreterHelper.pollForInterpreter(caseId, defendantId, "german");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload(null));

        updateInterpreterHelper.pollForEmptyInterpreter(caseId, defendantId);
    }

    @Test
    public void shouldRejectInterpreterUpdateIfCaseIsAlreadyCompleted() {
        stubGetCaseDecisionsWithDecision(caseSjpHelper.getCaseId());

        final String defendantId = caseSjpHelper.getSingleDefendantId();
        final String caseId = caseSjpHelper.getCaseId();

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("french"));

        final JsonEnvelope event = updateInterpreterHelper.getEventFromPublicTopic();
        assertThat(event, getCaseUpdateRejectedPublicEventMatcher(caseId, CaseUpdateRejected.RejectReason.CASE_COMPLETED.name()));
    }

    @Test
    public void shouldRejectInterpreterUpdateIfCaseIsAssignedToSomebodyElse() {
        stubGetCaseDecisionsWithNoDecision(caseSjpHelper.getCaseId());
        stubGetAssignmentsByDomainObjectId(caseSjpHelper.getCaseId(), randomUUID());

        final String defendantId = caseSjpHelper.getSingleDefendantId();
        final String caseId = caseSjpHelper.getCaseId();

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("french"));

        final JsonEnvelope event = updateInterpreterHelper.getEventFromPublicTopic();
        assertThat(event, getCaseUpdateRejectedPublicEventMatcher(caseId, CaseUpdateRejected.RejectReason.CASE_ASSIGNED.name()));
    }

    private Matcher getCaseUpdateRejectedPublicEventMatcher(final String caseId, final String reason) {
        final Matcher payloadMatcher = allOf(
                withJsonPath("$.caseId", is(caseId)),
                withJsonPath("$.reason", is(reason)));

        return jsonEnvelope()
                .withMetadataOf(metadata().withName("public.structure.case-update-rejected"))
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
