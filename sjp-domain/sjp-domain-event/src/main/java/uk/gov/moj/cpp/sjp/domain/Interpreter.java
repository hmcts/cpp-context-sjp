package uk.gov.moj.cpp.sjp.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Interpreter implements Serializable {

    private static final long serialVersionUID = 1173695109482711206L;

    private static final Interpreter NOT_NEEDED = new Interpreter(null);

    private final String language;

    @JsonCreator
    private Interpreter(@JsonProperty("language") final String language) {
        this.language = language;
    }

    public static Interpreter of(final String language) {
        return Optional.ofNullable(language)
                .filter(Interpreter::isNeeded)
                .map(Interpreter::new)
                .orElse(NOT_NEEDED);
    }

    public String getLanguage() {
        return language;
    }

    public Boolean isNeeded() {
        return isNeeded(language);
    }

    public static boolean isNeeded(final String language) {
        return ! StringUtils.isEmpty(language);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Interpreter)) {
            return false;
        }

        final Interpreter that = (Interpreter) o;
        return Objects.equals(this.language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.language);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
