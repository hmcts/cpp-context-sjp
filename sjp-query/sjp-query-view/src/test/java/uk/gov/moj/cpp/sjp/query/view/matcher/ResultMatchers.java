package uk.gov.moj.cpp.sjp.query.view.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;

import java.math.BigDecimal;
import java.util.UUID;

import javax.json.JsonValue;

import org.hamcrest.Matcher;

public class ResultMatchers {

    private static final String FO = "FO";
    private static final UUID FINE_RESULT_TYPE_ID = UUID.fromString("c054d9fa-8595-4b0b-81fc-5bfdc1d7266b");

    public static Matcher<JsonValue> FO(final BigDecimal fine) {
        return isJson(allOf(
                withJsonPath("$.code", equalTo(FO)),
                withJsonPath("$.resultTypeId", equalTo(FINE_RESULT_TYPE_ID.toString())),
                withJsonPath("$.terminalEntries[0].index", equalTo(Prompt.AMOUNT_OF_FINE.getIndex())),
                withJsonPath("$.terminalEntries[0].value", equalTo(fine.toString()))
        ));
    }
}
