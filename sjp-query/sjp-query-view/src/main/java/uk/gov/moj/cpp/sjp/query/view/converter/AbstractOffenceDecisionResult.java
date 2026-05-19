package uk.gov.moj.cpp.sjp.query.view.converter;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public abstract class AbstractOffenceDecisionResult {

    private static final String VERDICT = "verdict";

    private final JsonObject offenceDecision;
    private final CachedReferenceData referenceData;
    private final String resultedOn;
    private final JsonArrayBuilder results;

    public AbstractOffenceDecisionResult(final JsonObject offenceDecision, final CachedReferenceData referenceData, final String resultedOn) {
        this.offenceDecision = offenceDecision;
        this.referenceData = referenceData;
        this.resultedOn = resultedOn;
        this.results = createArrayBuilder();
    }

    protected abstract JsonObjectBuilder toJsonObjectBuilder();

    protected void addResult(final ResultCode code) {
        addResult(code, createArrayBuilder());
    }

    protected void addResult(final ResultCode code, final JsonArrayBuilder terminalEntries) {
        results.add(createObjectBuilder()
                .add("code", code.name())
                .add("resultTypeId", getResultTypeId(code))
                .add("terminalEntries", terminalEntries));
    }

    protected JsonArrayBuilder terminalEntries(final int index, final String value) {
        return createArrayBuilder().add(createObjectBuilder()
                .add("index", index)
                .add("value", value));
    }

    protected JsonObjectBuilder createOffenceDecision() {
        addPressRestriction();

        final JsonObjectBuilder decision = createObjectBuilder()
                .add("id", getOffenceId())
                .add("results", results);

        if (hasVerdict()) {
            decision.add(VERDICT, getVerdict());
        }

        return decision;
    }

    public JsonObject getOffenceDecision() {
        return offenceDecision;
    }

    public CachedReferenceData getReferenceData() {
        return referenceData;
    }

    public String getVerdict() {
        return getOffenceDecisionInformation().getString(VERDICT);
    }

    public String getOffenceId() {
        return getOffenceDecisionInformation().getString("offenceId");
    }

    public boolean hasPressRestriction() {
        return offenceDecision.containsKey("pressRestriction");
    }

    public JsonObject getPressRestriction() {
        return offenceDecision.getJsonObject("pressRestriction");
    }

    public String getResultTypeId(final ResultCode resultCode) {
        return referenceData.getResultId(resultCode.name()).toString();
    }

    private JsonObject getOffenceDecisionInformation() {
        return offenceDecision.getJsonArray("offenceDecisionInformation").getJsonObject(0);
    }

    private void addPressRestriction() {
        if (hasPressRestriction()) {
            results.add(new PressRestrictionResult(getPressRestriction(), referenceData, resultedOn).toJsonObjectBuilder());
        }
    }

    private boolean hasVerdict() {
        return getOffenceDecisionInformation().containsKey(VERDICT);
    }
}
