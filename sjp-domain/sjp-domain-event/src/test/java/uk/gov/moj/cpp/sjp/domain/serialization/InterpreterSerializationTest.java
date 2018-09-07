package uk.gov.moj.cpp.sjp.domain.serialization;

import static org.hamcrest.CoreMatchers.equalTo;

import uk.gov.moj.cpp.sjp.domain.Interpreter;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;

public class InterpreterSerializationTest extends AbstractSerializationTest<Interpreter> {

    private static final Interpreter FULL_INTERPRETER = Interpreter.of("French");
    private static final Interpreter EMPTY_INTERPRETER = Interpreter.of(null);

    private static final String EXPECTED_FULL_INTERPRETER_SERIALIZATION = "{\"language\":\"French\",\"needed\":true}";
    private static final String EXPECTED_EMPTY_INTERPRETER_SERIALIZATION = "{\"needed\":false}";

    @Override
    Map<Interpreter, Matcher<String>> getParams() {
        return ImmutableMap.of(
                FULL_INTERPRETER, equalTo(EXPECTED_FULL_INTERPRETER_SERIALIZATION),
                EMPTY_INTERPRETER, equalTo(EXPECTED_EMPTY_INTERPRETER_SERIALIZATION)
        );
    }

}