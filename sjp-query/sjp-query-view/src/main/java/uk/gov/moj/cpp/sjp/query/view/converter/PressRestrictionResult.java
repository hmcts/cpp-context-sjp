package uk.gov.moj.cpp.sjp.query.view.converter;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.D45;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.DPR;

import uk.gov.moj.cpp.sjp.query.view.service.CachedReferenceData;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class PressRestrictionResult {

    private final JsonObject pressRestriction;
    private final CachedReferenceData referenceData;
    private final String resultedOn;

    public PressRestrictionResult(final JsonObject pressRestriction,
                                  final CachedReferenceData referenceData,
                                  final String resultedOn) {
        this.pressRestriction = pressRestriction;
        this.referenceData = referenceData;
        this.resultedOn = resultedOn;
    }

    public JsonObjectBuilder toJsonObjectBuilder() {
        if (isRequested()) {
            return createObjectBuilder()
                    .add("code", D45.name())
                    .add("resultTypeId", getResultTypeId(D45))
                    .add("terminalEntries", terminalEntries(pressRestriction.getString("name")));
        } else {
            return createObjectBuilder()
                    .add("code", DPR.name())
                    .add("resultTypeId", getResultTypeId(DPR))
                    .add("terminalEntries", terminalEntries(resultedOn));
        }
    }

    private String getResultTypeId(final ResultCode resultCode) {
        return referenceData.getResultId(resultCode.name()).toString();
    }

    private boolean isRequested() {
        return pressRestriction.getBoolean("requested");
    }

    private JsonArrayBuilder terminalEntries(final String value) {
        return createArrayBuilder().add(terminalEntry(1, value));
    }

    private JsonObjectBuilder terminalEntry(final int index, final String value) {
        return createObjectBuilder()
                .add("index", index)
                .add("value", value);
    }
}
