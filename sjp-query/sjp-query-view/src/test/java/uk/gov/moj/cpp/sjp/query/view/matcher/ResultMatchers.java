package uk.gov.moj.cpp.sjp.query.view.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.converter.ResultCode;

import java.math.BigDecimal;

import javax.json.JsonValue;

import org.hamcrest.Matcher;

public class ResultMatchers {

    public static Matcher<JsonValue> FO(final BigDecimal fine) {
        return isJson(allOf(
                withJsonPath("$.code", equalTo(ResultCode.FO.name())),
                withJsonPath("$.resultTypeId", equalTo(ResultCode.FO.getResultDefinitionId().toString())),
                withJsonPath("$.terminalEntries[0].index", equalTo(Prompt.AMOUNT_OF_FINE.getIndex())),
                withJsonPath("$.terminalEntries[0].value", equalTo(fine.toString()))
        ));
    }
}
