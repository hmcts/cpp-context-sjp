package uk.gov.moj.cpp.sjp.query.view.util.results;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.D;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.D45;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.DPR;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.WDRNNOT;

import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.UUID;

import org.hamcrest.Matcher;

public class ResultsMatchers {

    private ResultsMatchers() {
        // Utils class should have private ctor
    }

    public static Matcher isOffenceDecision(final UUID offenceId, final VerdictType verdict) {
        return isJson(allOf(
                withJsonPath("$.id", equalTo(offenceId.toString())),
                withJsonPath("$.verdict", equalTo(verdict.toString()))
        ));
    }

    public static Matcher hasResults(final Matcher... matchers) {
        return hasJsonPath("$.results[*]", containsInAnyOrder(matchers));
    }


    public static Matcher WDRNNOT(final String withdrawReason) {
        return isJson(allOf(
                withJsonPath("code", equalTo(WDRNNOT.name())),
                withJsonPath("resultTypeId", equalTo(WDRNNOT.getResultDefinitionId().toString())),
                withJsonPath("terminalEntries[*]", hasSize(1)),
                withJsonPath("terminalEntries[0].index", equalTo(2)),
                withJsonPath("terminalEntries[0].value", equalTo(withdrawReason))
        ));
    }

    public static Matcher D45(final String name) {
        return isJson(allOf(
                withJsonPath("code", equalTo(D45.name())),
                withJsonPath("resultTypeId", equalTo(D45.getResultDefinitionId().toString())),
                withJsonPath("terminalEntries[*]", hasSize(1)),
                withJsonPath("terminalEntries[0].index", equalTo(1)),
                withJsonPath("terminalEntries[0].value", equalTo(name))
        ));
    }

    public static Matcher DPR(final String value) {
        return isJson(allOf(
                withJsonPath("code", equalTo(DPR.name())),
                withJsonPath("resultTypeId", equalTo(DPR.getResultDefinitionId().toString())),
                withJsonPath("terminalEntries[*]", hasSize(1)),
                withJsonPath("terminalEntries[0].index", equalTo(1)),
                withJsonPath("terminalEntries[0].value", equalTo(value))
        ));
    }

    public static Matcher D() {
        return isJson(allOf(
                withJsonPath("code", equalTo(D.name())),
                withJsonPath("resultTypeId", equalTo(D.getResultDefinitionId().toString())),
                withJsonPath("terminalEntries[*]", empty())
        ));
    }
}
