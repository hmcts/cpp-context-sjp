package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ProsecutionCaseView {

    private final UUID id;
    private final String initiationCode;
    private final String statementOfFacts;
    private final String statementOfFactsWelsh;
    private final ProsecutionCaseIdentifierView prosecutionCaseIdentifier;
    private final List<DefendantView> defendants;
    private final String originatingOrganisation;

    public ProsecutionCaseView(final UUID id,
                               final String initiationCode,
                               final String statementOfFacts,
                               final String statementOfFactsWelsh,
                               final ProsecutionCaseIdentifierView prosecutionCaseIdentifier,
                               final List<DefendantView> defendants,
                               final String originatingOrganisation) {
        this.id = id;
        this.initiationCode = initiationCode;
        this.statementOfFacts = statementOfFacts;
        this.statementOfFactsWelsh = statementOfFactsWelsh;
        this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
        this.defendants = defendants;
        this.originatingOrganisation = originatingOrganisation;
    }

    public UUID getId() {
        return id;
    }

    public String getInitiationCode() {
        return initiationCode;
    }

    public String getStatementOfFacts() {
        return statementOfFacts;
    }

    public String getStatementOfFactsWelsh() {
        return statementOfFactsWelsh;
    }

    public ProsecutionCaseIdentifierView getProsecutionCaseIdentifier() {
        return prosecutionCaseIdentifier;
    }

    public List<DefendantView> getDefendants() {
        return defendants;
    }

    public String getOriginatingOrganisation() {
        return originatingOrganisation;
    }

    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ProsecutionCaseView that = (ProsecutionCaseView) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(initiationCode, that.initiationCode) &&
                Objects.equals(statementOfFacts, that.statementOfFacts) &&
                Objects.equals(prosecutionCaseIdentifier, that.prosecutionCaseIdentifier) &&
                Objects.equals(defendants, that.defendants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, initiationCode, statementOfFacts, prosecutionCaseIdentifier, defendants);
    }
}
