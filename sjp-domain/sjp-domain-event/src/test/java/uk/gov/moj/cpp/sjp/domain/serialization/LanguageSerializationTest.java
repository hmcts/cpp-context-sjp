package uk.gov.moj.cpp.sjp.domain.serialization;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.equalTo;

import uk.gov.justice.json.schemas.domains.sjp.Language;

import java.util.Arrays;
import java.util.Map;

import org.hamcrest.Matcher;

public class LanguageSerializationTest extends AbstractSerializationTest<Language> {

    @Override
    Map<Language, Matcher<String>> getParams() {
        return Arrays.stream(Language.values())
                .collect(toMap(identity(), lang -> equalTo("\"" + lang.name().charAt(0) + "\"")));
    }

}