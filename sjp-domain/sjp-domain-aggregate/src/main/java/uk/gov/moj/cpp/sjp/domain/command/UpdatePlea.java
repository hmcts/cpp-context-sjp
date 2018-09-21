package uk.gov.moj.cpp.sjp.domain.command;


import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdatePlea extends ChangePlea {

    private final PleaType plea;
    private final String interpreterLanguage;
    // Boolean interpreterRequired; - extra field serialised as part of this class
    private final Boolean speakWelsh;

    public UpdatePlea(final UUID caseId,
                      final UUID offenceId,
                      final PleaType plea) {
        this(caseId, offenceId, plea, null, null);
    }

    @JsonCreator
    public UpdatePlea(@JsonProperty("caseId") final UUID caseId,
                      @JsonProperty("offenceId") final UUID offenceId,
                      @JsonProperty("plea") final PleaType plea,
                      @JsonProperty("interpreterLanguage") final String interpreterLanguage,
                      @JsonProperty("speakWelsh") final Boolean speakWelsh) {
        super(caseId, offenceId);
        this.plea = plea;
        this.interpreterLanguage = interpreterLanguage;
        this.speakWelsh = speakWelsh;
    }

    public PleaType getPlea() {
        return plea;
    }

    @SuppressWarnings("unused") // used during serialization
    @JsonProperty("interpreterRequired")
    public Boolean getInterpreterRequired() {
        return Interpreter.isNeeded(interpreterLanguage);
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }

    public Boolean getSpeakWelsh() {
        return speakWelsh;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UpdatePlea)) {
            return false;
        }

        final UpdatePlea that = (UpdatePlea) o;
        return plea == that.plea &&
                Objects.equals(this.interpreterLanguage, that.interpreterLanguage) &&
                Objects.equals(this.speakWelsh, that.speakWelsh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plea, interpreterLanguage, speakWelsh);
    }

}
