package uk.gov.moj.cpp.sjp.query.api.service;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictService;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class SjpVerdictService {

    private final VerdictService verdictService = new VerdictService();

    public JsonObject calculateVerdicts(JsonObject offencesDecisionsJson) {
        final JsonArrayBuilder verdictsArray = createArrayBuilder();

        offencesDecisionsJson.getJsonArray("decisions").
                getValuesAs(JsonObject.class).
                stream().
                map(this::calculateVerdict).
                forEach(verdictsArray::add);

        return createObjectBuilder().
                add("verdicts", verdictsArray).
                build();

    }

    private JsonObject calculateVerdict(final JsonObject offenceDecision) {

        final PleaType pleaType = offenceDecision.containsKey("pleaType") && !offenceDecision.isNull("pleaType") ?
                PleaType.valueOf(offenceDecision.getString("pleaType")) : null;
        final int offenceNumber = offenceDecision.getInt("offenceNumber");
        final DecisionType decisionType = DecisionType.valueOf(offenceDecision.getString("decision"));

        final VerdictType verdict = verdictService.calculateVerdict(pleaType, decisionType);

        return createObjectBuilder().
                add("offenceNumber", offenceNumber).
                add("verdict", verdict.name()).
                build();
    }

}
