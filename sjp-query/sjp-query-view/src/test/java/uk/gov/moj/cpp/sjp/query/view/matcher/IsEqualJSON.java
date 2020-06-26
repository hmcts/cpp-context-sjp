package uk.gov.moj.cpp.sjp.query.view.matcher;

import static java.util.Objects.nonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Factory;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

/**
 * A Matcher for comparing JSON. Example usage:
 * <pre>
 * assertThat(new String[] {"foo", "bar"}, equalToJSON("[\"foo\", \"bar\"]"));
 * </pre>
 */
public class IsEqualJSON extends DiagnosingMatcher<Object> {

    private final String expectedJSON;
    private final CustomComparator customComparator;
    private JSONCompareMode defaultCompareMode;

    public IsEqualJSON(final String expectedJSON, final CustomComparator customComparator) {
        this.expectedJSON = expectedJSON;
        this.customComparator = customComparator;
        this.defaultCompareMode = JSONCompareMode.STRICT;
    }

    /**
     * Converts the specified object into a JSON string.
     *
     * @param o the object to convert
     * @return the JSON string
     */
    private static String toJSONString(final Object o) {
        try {
            return o instanceof String ?
                    (String) o : new ObjectMapper().writeValueAsString(o);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the specified file into a string.
     *
     * @param path the path to read
     * @return the contents of the file
     */
    private static String getFileContents(final Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a matcher that matches when the examined object is equal to the specified JSON
     * string. For example:
     * <pre>
     * assertThat(new String[] {"foo", "bar"},
     *            equalToJSON("[\"foo\", \"bar\"]"));
     * </pre>
     *
     * @param expectedJSON the expected JSON string
     * @return the JSON matcher
     */
    @Factory
    public static IsEqualJSON equalToJSON(final String expectedJSON) {
        return new IsEqualJSON(expectedJSON, null);
    }

    /**
     * Creates a matcher that matches when the examined object is equal to the specified JSON
     * string. For example:
     * <pre>
     * assertThat(new String[] {"foo", "bar"},
     *            equalToJSON("[\"foo\", \"bar\"]"));
     * </pre>
     *
     * @param expectedJSON the expected JSON string
     * @return the JSON matcher
     */
    @Factory
    public static IsEqualJSON equalToJSON(final String expectedJSON, final CustomComparator customComparator) {
        return new IsEqualJSON(expectedJSON, customComparator);
    }

    /**
     * Creates a matcher that matches when the examined object is equal to the JSON in the specified
     * file. For example:
     * <pre>
     * assertThat(new String[] {"foo", "bar"},
     *            equalToJSONInFile(Paths.get("/tmp/foo.json"));
     * </pre>
     *
     * @param expectedJSON the path containing the expected JSON
     * @return the JSON matcher
     */
    @Factory
    public static IsEqualJSON equalToJSONInFile(final Path expectedPath) {
        return equalToJSON(getFileContents(expectedPath));
    }

    /**
     * Creates a matcher that matches when the examined object is equal to the JSON contained in the
     * file with the specified name. For example:
     * <pre>
     * assertThat(new String[] {"foo", "bar"},
     *            equalToJSONInFile("/tmp/foo.json"));
     * </pre>
     *
     * @param expectedJSON the name of the file containing the expected JSON
     * @return the JSON matcher
     */
    @Factory
    public static IsEqualJSON equalToJSONInFile(final String expectedFileName) {
        return equalToJSONInFile(Paths.get(expectedFileName));
    }

    /**
     * Changes this matcher's JSON compare mode to lenient.
     *
     * @return this matcher
     */
    public IsEqualJSON leniently() {
        defaultCompareMode = JSONCompareMode.LENIENT;
        return this;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(expectedJSON);
    }

    @Override
    protected boolean matches(final Object actual,
                              final Description mismatchDescription) {
        final String actualJSON = toJSONString(actual);
        final JSONCompareResult result = compareJson(actualJSON);
        if (!result.passed()) {
            mismatchDescription.appendText(result.getMessage());
        }
        return result.passed();
    }

    private JSONCompareResult compareJson(final String actualJSON) {
        if (nonNull(customComparator)) {
            return JSONCompare.compareJSON(expectedJSON, actualJSON, customComparator);
        } else {
            return JSONCompare.compareJSON(expectedJSON, actualJSON, defaultCompareMode);
        }
    }
}