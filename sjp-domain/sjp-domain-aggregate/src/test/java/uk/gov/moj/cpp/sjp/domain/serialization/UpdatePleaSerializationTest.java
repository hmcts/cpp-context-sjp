package uk.gov.moj.cpp.sjp.domain.serialization;

import static org.hamcrest.CoreMatchers.equalTo;

import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.serialization.AbstractSerializationTest;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;

public class UpdatePleaSerializationTest extends AbstractSerializationTest<UpdatePlea> {

    private static final UpdatePlea FULL_UPDATE_PLEA = new UpdatePlea(
            UUID.fromString("0503ba22-5c4b-452c-a30d-6d3056b59d95"),
            UUID.fromString("d6c28e64-d9a8-4c5a-ad95-a13635938d9c"),
            PleaType.NOT_GUILTY,
            "French"
            );

    private static final UpdatePlea UPDATE_PLEA_WITHOUT_INTERPRETER = new UpdatePlea(
            UUID.fromString("0503ba22-5c4b-452c-a30d-6d3056b59d95"),
            UUID.fromString("d6c28e64-d9a8-4c5a-ad95-a13635938d9c"),
            PleaType.NOT_GUILTY,
            null
    );

    private static final String EXPECTED_FULL_INTERPRETER_SERIALIZATION = "{\"caseId\":\"0503ba22-5c4b-452c-a30d-6d3056b59d95\",\"offenceId\":\"d6c28e64-d9a8-4c5a-ad95-a13635938d9c\",\"plea\":\"NOT_GUILTY\",\"interpreterLanguage\":\"French\",\"interpreterRequired\":true}";
    private static final String EXPECTED_UPDATE_PLEA_WITHOUT_INTERPRETER_SERIALIZATION = "{\"caseId\":\"0503ba22-5c4b-452c-a30d-6d3056b59d95\",\"offenceId\":\"d6c28e64-d9a8-4c5a-ad95-a13635938d9c\",\"plea\":\"NOT_GUILTY\",\"interpreterRequired\":false}";

    @Override
    Map<UpdatePlea, Matcher<String>> getParams() {
        return ImmutableMap.of(
                FULL_UPDATE_PLEA, equalTo(EXPECTED_FULL_INTERPRETER_SERIALIZATION),
                UPDATE_PLEA_WITHOUT_INTERPRETER, equalTo(EXPECTED_UPDATE_PLEA_WITHOUT_INTERPRETER_SERIALIZATION)
        );
    }

}