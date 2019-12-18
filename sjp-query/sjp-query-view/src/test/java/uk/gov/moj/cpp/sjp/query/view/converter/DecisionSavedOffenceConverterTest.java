package uk.gov.moj.cpp.sjp.query.view.converter;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;

import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionSavedOffenceConverterTest {

    private final UUID OFFENCE_ID = randomUUID();
    private final UUID WITHDRAWAL_REASON_ID = randomUUID();
    private final UUID CASE_DECISION_ID = randomUUID();

    @InjectMocks
    private DecisionSavedOffenceConverter decisionSavedOffenceConverter;

    @Test
    public void shouldConvertToDecisionSavedEvent() {

        OffenceDecision offenceDecision = new WithdrawOffenceDecision(OFFENCE_ID, CASE_DECISION_ID, WITHDRAWAL_REASON_ID, NO_VERDICT);


        final JsonObject decisionSavedPayload = decisionSavedOffenceConverter.convertOffenceDecision(new OffenceDecisionView(offenceDecision));
        assertThat(decisionSavedPayload,
                payloadIsJson(allOf(
                        withJsonPath("type", is(WITHDRAW.toString())),
                        withJsonPath("withdrawalReasonId", is(WITHDRAWAL_REASON_ID.toString())),
                        withJsonPath("offenceDecisionInformation[0].offenceId", is(OFFENCE_ID.toString())),
                        withJsonPath("offenceDecisionInformation[0].verdict", is(NO_VERDICT.toString()))
                )));
    }
}
