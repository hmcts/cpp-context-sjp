package uk.gov.moj.cpp.sjp.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefendantCourtInterpreter implements Serializable {

    private static final long serialVersionUID = -3915928760504041172L;

    private String language;

    private Boolean needed;

    @JsonCreator
    public DefendantCourtInterpreter(@JsonProperty("language") final String language,
                                     @JsonProperty("needed") final Boolean needed) {
        this.language = language;
        this.needed = needed;
    }

    public String getLanguage() {
        return language;
    }

    public Boolean getNeeded() {
        return needed;
    }

    @Override
    @SuppressWarnings("squid:S00122")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DefendantCourtInterpreter that = (DefendantCourtInterpreter) o;
        return language.equals(that.language) &&
                needed.equals(that.needed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, needed);
    }
}
