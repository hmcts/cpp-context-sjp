package uk.gov.moj.cpp.sjp.domain.serialization;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.equalTo;

import uk.gov.justice.json.schemas.domains.sjp.Gender;

import java.util.Arrays;
import java.util.Map;

import org.hamcrest.Matcher;

public class GenderSerializationTest extends AbstractSerializationTest<Gender> {

    @Override
    Map<Gender, Matcher<String>> getParams() {
        return Arrays.stream(Gender.values())
                .collect(toMap(identity(), g -> equalTo("\"" + g.toString() + "\"")));
    }

}