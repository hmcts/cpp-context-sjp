package uk.gov.moj.cpp.sjp.query.view.util.builders;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;

import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class OffenceDecisionBuilder {

    private UUID offenceId;
    private VerdictType verdict;
    private JsonObjectBuilder pressRestriction;
    private UUID withdrawalReasonId;

    public static OffenceDecisionBuilder offenceDecision() {
        final OffenceDecisionBuilder builder = new OffenceDecisionBuilder();
        builder.offenceId = randomUUID();
        builder.verdict = NO_VERDICT;
        return builder;
    }

    public JsonObject build() {
        final JsonObjectBuilder offenceDecision = createObjectBuilder()
                .add("offenceId", this.offenceId.toString());
        if (nonNull(this.verdict)) {
            offenceDecision.add("verdict", this.verdict.toString());
        }

        final JsonObjectBuilder result = createObjectBuilder()
                .add("offenceDecisionInformation", createArrayBuilder().add(offenceDecision));

        if (nonNull(pressRestriction)) {
            result.add("pressRestriction", pressRestriction);
        }

        if (nonNull(withdrawalReasonId)) {
            result.add("withdrawalReasonId", withdrawalReasonId.toString());
        }

        return result.build();
    }

    public OffenceDecisionBuilder withPressRestriction(final String name) {
        this.pressRestriction = createObjectBuilder()
                .add("name", name)
                .add("requested", true);
        return this;
    }

    public OffenceDecisionBuilder withPressRestrictionRevoked() {
        this.pressRestriction = createObjectBuilder()
                .addNull("name")
                .add("requested", false);
        return this;
    }

    public OffenceDecisionBuilder withWithdrawalReasonId(final UUID withdrawReasonId) {
        this.withdrawalReasonId = withdrawReasonId;
        return this;
    }

    public OffenceDecisionBuilder setAside() {
        this.verdict = null;
        return this;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public VerdictType getVerdict() {
        return this.verdict;
    }
}
