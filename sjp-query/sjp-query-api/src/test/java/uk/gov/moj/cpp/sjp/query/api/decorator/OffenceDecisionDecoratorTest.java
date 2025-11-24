package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.query.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceDecisionDecoratorTest {

    final UUID caseId = randomUUID();

    @Mock
    private JsonEnvelope queryEnvelope;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private WithdrawalReasons withdrawalReasons;

    @InjectMocks
    private OffenceDecisionDecorator offenceDecisionDecorator;

    @Test
    public void shouldNotDecorateOffenceDecisionWhenDecisionNotPresent() {
        final JsonObject originalCase = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();

        final JsonObject decoratedCase = offenceDecisionDecorator.decorate(originalCase, queryEnvelope, withdrawalReasons);

        assertThat(decoratedCase, is(originalCase));
    }

    @Test
    public void shouldDecorateOffenceDecisions() {
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();

        final UUID decision1Id = randomUUID();
        final UUID decision2Id = randomUUID();
        final UUID session1Id = randomUUID();
        final UUID session2Id = randomUUID();
        final ZonedDateTime decision1SavedAt = now().minusDays(1);
        final ZonedDateTime decision2SavedAt = now();

        final UUID referralReasonId = randomUUID();
        final String referralReason = "Case unsuitable for SJP";

        final UUID withdrawalReasonId = randomUUID();
        final String withdrawalReason = "Insufficient evidence";

        final JsonObject adjournOffence1 = adjourn(offence1Id);
        final JsonObject adjournOffence2 = adjourn(offence2Id);
        final JsonObject adjournOffence3 = adjourn(offence3Id);
        final JsonObject withdrawOffence1 = withdraw(offence1Id, withdrawalReasonId);
        final JsonObject referForCourtHearingOffences2And3 = referForCourtHearing(referralReasonId, offence2Id, offence3Id);

        when(referenceDataService.getReferralReasons(queryEnvelope))
                .thenReturn(singletonList(createObjectBuilder()
                        .add("id", referralReasonId.toString())
                        .add("reason", referralReason)
                        .build()));

        when(withdrawalReasons.getWithdrawalReason(withdrawalReasonId))
                .thenReturn(of(withdrawalReason));

        final JsonObject originalCase = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("caseDecisions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", decision1Id.toString())
                                .add("sessionId", session1Id.toString())
                                .add("savedAt", decision1SavedAt.toString())
                                .add("offenceDecisions", createArrayBuilder()
                                        .add(adjournOffence1)
                                        .add(adjournOffence2)
                                        .add(adjournOffence3)))
                        .add(createObjectBuilder()
                                .add("id", decision2Id.toString())
                                .add("sessionId", session2Id.toString())
                                .add("savedAt", decision2SavedAt.toString())
                                .add("offenceDecisions", createArrayBuilder()
                                        .add(withdrawOffence1)
                                        .add(referForCourtHearingOffences2And3))))
                .build();


        final JsonObject expectedDecoratedCase = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("caseDecisions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", decision1Id.toString())
                                .add("sessionId", session1Id.toString())
                                .add("savedAt", decision1SavedAt.toString())
                                .add("offenceDecisions", createArrayBuilder()
                                        .add(adjournOffence1)
                                        .add(adjournOffence2)
                                        .add(adjournOffence3)))
                        .add(createObjectBuilder()
                                .add("id", decision2Id.toString())
                                .add("sessionId", session2Id.toString())
                                .add("savedAt", decision2SavedAt.toString())
                                .add("offenceDecisions", createArrayBuilder()
                                        .add(decoratedWithdrawal(withdrawOffence1, withdrawalReason))
                                        .add(decoratedReferForCourtHearing(referForCourtHearingOffences2And3, referralReason)))))
                .build();

        final JsonObject actualDecoratedCase = offenceDecisionDecorator.decorate(originalCase, queryEnvelope, withdrawalReasons);

        assertThat(actualDecoratedCase, is(expectedDecoratedCase));
    }

    private static JsonObject adjourn(final UUID offenceId) {
        return createObjectBuilder()
                .add("offenceId", offenceId.toString())
                .add("decisionType", DecisionType.ADJOURN.name())
                .add("adjournTo", LocalDate.now().toString())
                .build();
    }

    private static JsonObject withdraw(final UUID offenceId, final UUID withdrawalReasonId) {
        return createObjectBuilder()
                .add("offenceId", offenceId.toString())
                .add("decisionType", DecisionType.WITHDRAW.name())
                .add("withdrawalReasonId", withdrawalReasonId.toString())
                .build();
    }

    private static JsonObject decoratedWithdrawal(final JsonObject originalWithdrawal, final String withdrawalReason) {
        return JsonObjects.createObjectBuilder(originalWithdrawal)
                .add("withdrawalReason", withdrawalReason)
                .build();
    }

    private static JsonObject referForCourtHearing(final UUID referralReasonId, final UUID... offenceIds) {
        return createObjectBuilder()
                .add("offenceIds", stream(offenceIds).map(UUID::toString).reduce(createArrayBuilder(), JsonArrayBuilder::add, JsonArrayBuilder::add))
                .add("decisionType", DecisionType.REFER_FOR_COURT_HEARING.name())
                .add("referralReasonId", referralReasonId.toString())
                .add("estimatedHearingDuration", 10)
                .build();
    }

    private static JsonObject decoratedReferForCourtHearing(final JsonObject originalReferForCourtHearing, final String referralReason) {
        return JsonObjects.createObjectBuilder(originalReferForCourtHearing)
                .add("referralReason", referralReason)
                .build();
    }
}
