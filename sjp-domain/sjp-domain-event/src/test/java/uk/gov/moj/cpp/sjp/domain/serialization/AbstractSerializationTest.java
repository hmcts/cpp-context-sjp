package uk.gov.moj.cpp.sjp.domain.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Serialise and Deserialize the same object to check that it doesn't lose information.
 * @param <T> entity to serialize
 */
public abstract class AbstractSerializationTest<T> {

    private static ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    /**
     * @return a Map<K, V>
     * - K: object to serialise
     * - V: matcher to assert the expected serialization
     */
    protected abstract Map<T, Matcher<String>> getParams();

    @Test
    public void canSerializeDeserialize() throws IOException {
        for (Map.Entry<T, Matcher<String>> inputToMatcher : getParams().entrySet()) {
            assertSerializeDeserializeOf(inputToMatcher.getKey(), inputToMatcher.getValue());
        }
    }

    private void assertSerializeDeserializeOf(T input, Matcher<String> matcher) throws IOException {
        final String serialized = objectMapper.writeValueAsString(input);

        assertThat("Failure on expected Serialization: ", serialized, matcher);

        final Object deserialized = objectMapper.readValue(serialized, input.getClass());

        assertThat("Failure on expected Deserialization: ", deserialized, equalTo(input));
    }

}
