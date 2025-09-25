package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.OffenceCode;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.update-offence-code-request-received")
public class UpdateOffenceCodeRequestReceived implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID caseId;

    private List<OffenceCode> updatedOffenceCodes;

    private UpdateOffenceCodeRequestReceived() {
    }

    @JsonCreator
    private UpdateOffenceCodeRequestReceived(
            @JsonProperty(value = "caseId", required = true) final UUID caseId,
            @JsonProperty(value = "updatedOffenceCodes", required = true) final List<OffenceCode> updatedOffenceCodes) {
        this.caseId = caseId;
        this.updatedOffenceCodes = updatedOffenceCodes;
    }

    public static UpdateOffenceCodeRequestReceived.Builder builder() {
        return new UpdateOffenceCodeRequestReceived.Builder();
    }

    public static UpdateOffenceCodeRequestReceived.Builder builder(final UpdateOffenceCodeRequestReceived copy) {
        final UpdateOffenceCodeRequestReceived.Builder builder = new UpdateOffenceCodeRequestReceived.Builder();
        builder.caseId = copy.getCaseId();
        builder.updatedOffenceCodes = copy.getUpdatedOffenceCodes();
        return builder;
    }

    public List<OffenceCode> getUpdatedOffenceCodes() {
        return this.updatedOffenceCodes;
    }

    public UUID getCaseId() {
        return this.caseId;
    }

    public static final class Builder {
        private UUID caseId;

        private List<OffenceCode> updatedOffenceCodes;

        private Builder() {
        }

        public UpdateOffenceCodeRequestReceived.Builder withCaseId(final UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public UpdateOffenceCodeRequestReceived.Builder withUpdateOffenceCodes(final List<OffenceCode> updatedOffenceCodes) {
            this.updatedOffenceCodes = updatedOffenceCodes;
            return this;
        }

        public UpdateOffenceCodeRequestReceived build() {
            return new UpdateOffenceCodeRequestReceived(caseId, updatedOffenceCodes);
        }
    }

}
