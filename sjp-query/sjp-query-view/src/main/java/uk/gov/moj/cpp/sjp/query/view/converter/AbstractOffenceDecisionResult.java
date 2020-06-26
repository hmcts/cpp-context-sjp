package uk.gov.moj.cpp.sjp.query.view.converter;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ID;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.OFFENCE_DECISION_INFORMATION;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.OFFENCE_ID;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.RESULTS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.VERDICT;

import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public abstract class AbstractOffenceDecisionResult {

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
        return createObjectBuilder()
                .add(ID, getOffenceId())
                .add(VERDICT, getVerdict())
                .add(RESULTS, results);
    }

    private void addPressRestriction() {
        if (hasPressRestriction()) {
            results.add(new PressRestrictionResult(getPressRestriction(), referenceData, resultedOn).toJsonObjectBuilder());
        }
    }

    public JsonObject getOffenceDecision() {
        return offenceDecision;
    }

    public CachedReferenceData getReferenceData() {
        return referenceData;
    }

    public String getVerdict() {
        return offenceDecision.getJsonArray(OFFENCE_DECISION_INFORMATION).getJsonObject(0).getString(VERDICT);
    }

    public String getOffenceId() {
        return offenceDecision.getJsonArray(OFFENCE_DECISION_INFORMATION).getJsonObject(0).getString(OFFENCE_ID);
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
}
