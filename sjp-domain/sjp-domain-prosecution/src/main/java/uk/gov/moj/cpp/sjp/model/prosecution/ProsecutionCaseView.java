package uk.gov.moj.cpp.sjp.model.prosecution;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
// better deprecate this and use the core domain object instead
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
        this.defendants = ofNullable(defendants).map(LinkedList::new).orElse(null);
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
        return ofNullable(defendants).map(Collections::unmodifiableList).orElse(null);
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

    public static Builder prosecutionCaseView() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String initiationCode;
        private String statementOfFacts;
        private String statementOfFactsWelsh;
        private ProsecutionCaseIdentifierView prosecutionCaseIdentifier;
        private List<DefendantView> defendants;
        private String originatingOrganisation;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withInitiationCode(final String initiationCode) {
            this.initiationCode = initiationCode;
            return this;
        }

        public Builder withStatementOfFacts(final String statementOfFacts) {
            this.statementOfFacts = statementOfFacts;
            return this;
        }

        public Builder withStatementOfFactsWelsh(final String statementOfFactsWelsh) {
            this.statementOfFactsWelsh = statementOfFactsWelsh;
            return this;
        }

        public Builder withProsecutionCaseIdentifier(final ProsecutionCaseIdentifierView prosecutionCaseIdentifier) {
            this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
            return this;
        }

        public Builder withDefendant(final DefendantView defendantView) {
            this.defendants = singletonList(defendantView);
            return this;
        }

        public Builder withOriginatingOrganisation(final String originatingOrganisation) {
            this.originatingOrganisation = originatingOrganisation;
            return this;
        }

        public ProsecutionCaseView build() {
            return new ProsecutionCaseView(this.id, this.initiationCode, this.statementOfFacts,
                    this.statementOfFactsWelsh, this.prosecutionCaseIdentifier, this.defendants,
                    this.originatingOrganisation);
        }
    }
}
