package uk.gov.moj.sjp.it.util.matchers;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.AllOf.allOf;

import org.hamcrest.Matcher;

public class RegionsMatcher {

    private RegionsMatcher() {
    }

    public static Matcher region(final String id, final String name) {
        return hasItem(isJson(allOf(
                withJsonPath("$.id", equalTo(id)),
                withJsonPath("$.name", equalTo(name))
        )));
    }
}
