package uk.gov.moj.cpp.sjp.domain.resulting;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Prompt {
    private final UUID id;
    private final String value;

    @JsonCreator
    public Prompt(@JsonProperty("id") final UUID id, @JsonProperty("value") final String value) {
        this.id = id;
        this.value = value;
    }

    public static Builder prompts() {
        return new uk.gov.moj.cpp.sjp.domain.resulting.Prompt.Builder();
    }

    public UUID getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final uk.gov.moj.cpp.sjp.domain.resulting.Prompt that = (uk.gov.moj.cpp.sjp.domain.resulting.Prompt) obj;

        return java.util.Objects.equals(this.id, that.id) &&
                java.util.Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, value);
    }

    @Override
    public String toString() {
        return "Prompt{" +
                "id='" + id + "'," +
                "value='" + value + "'" +
                "}";
    }

    public static class Builder {
        private UUID id;

        private String value;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withValue(final String value) {
            this.value = value;
            return this;
        }

        public Prompt build() {
            return new uk.gov.moj.cpp.sjp.domain.resulting.Prompt(id, value);
        }
    }
}
