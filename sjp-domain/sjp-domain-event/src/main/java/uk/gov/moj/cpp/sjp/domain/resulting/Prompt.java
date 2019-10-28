package uk.gov.moj.cpp.sjp.domain.resulting;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Prompt {
    private final UUID promptDefinitionId;
    private final String value;

    @JsonCreator
    public Prompt(@JsonProperty("promptDefinitionId") final UUID promptDefinitionId, @JsonProperty("value") final String value) {
        this.promptDefinitionId = promptDefinitionId;
        this.value = value;
    }

    public static Builder prompts() {
        return new uk.gov.moj.cpp.sjp.domain.resulting.Prompt.Builder();
    }

    public UUID getPromptDefinitionId() {
        return promptDefinitionId;
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

        return java.util.Objects.equals(this.promptDefinitionId, that.promptDefinitionId) &&
                java.util.Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(promptDefinitionId, value);
    }

    @Override
    public String toString() {
        return "Prompt{" +
                "promptDefinitionId='" + promptDefinitionId + "'," +
                "value='" + value + "'" +
                "}";
    }

    public static class Builder {
        private UUID promptDefinitionId;

        private String value;

        public Builder withPromptDefinitionId(final UUID promptDefinitionId) {
            this.promptDefinitionId = promptDefinitionId;
            return this;
        }

        public Builder withValue(final String value) {
            this.value = value;
            return this;
        }

        public Prompt build() {
            return new uk.gov.moj.cpp.sjp.domain.resulting.Prompt(promptDefinitionId, value);
        }
    }
}
