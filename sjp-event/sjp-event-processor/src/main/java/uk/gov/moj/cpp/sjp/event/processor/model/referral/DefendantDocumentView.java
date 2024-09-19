package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class DefendantDocumentView {

    private final UUID prosecutionCaseId;

    private final List<UUID> defendants;

    public DefendantDocumentView(final UUID prosecutionCaseId,
                                 final List<UUID> defendants) {
        this.prosecutionCaseId = prosecutionCaseId;
        this.defendants = defendants;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public List<UUID> getDefendants() {
        return defendants;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prosecutionCaseId, defendants);
    }

    @Override
    public String toString() {
        return "DefendantDocumentView{" +
                "prosecutionCaseId=" + prosecutionCaseId +
                ", defendants=" + defendants +
                '}';
    }
}
