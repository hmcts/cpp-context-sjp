package uk.gov.moj.cpp.sjp.model.prosecution;


import static java.util.Optional.ofNullable;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class DefendantView {

    private final UUID id;
    private final UUID masterDefendantId;
    private final UUID prosecutionCaseId;
    private final Integer numberOfPreviousConvictionsCited;
    private final String mitigation;
    private final List<OffenceView> offences;
    private final PersonDefendantView personDefendant;
    private final List<DefendantAliasView> aliases;

    private final LegalEntityDefendant legalEntityDefendant;
    public DefendantView(final UUID id,
                         final UUID prosecutionCaseId,
                         final Integer numberOfPreviousConvictionsCited,
                         final String mitigation,
                         final List<OffenceView> offences,
                         final PersonDefendantView personDefendant,
                         final List<DefendantAliasView> aliases,
                         final LegalEntityDefendant legalEntityDefendant) {

        this.id = id;
        this.masterDefendantId = id;
        this.prosecutionCaseId = prosecutionCaseId;
        this.numberOfPreviousConvictionsCited = numberOfPreviousConvictionsCited;
        this.mitigation = mitigation;
        this.offences = ofNullable(offences).map(LinkedList::new).orElse(null);
        this.personDefendant = personDefendant;
        this.aliases = ofNullable(aliases).map(LinkedList::new).orElse(null);
        this.legalEntityDefendant = legalEntityDefendant;
    }
    public UUID getId() {
        return id;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public Integer getNumberOfPreviousConvictionsCited() {
        return numberOfPreviousConvictionsCited;
    }

    public String getMitigation() {
        return mitigation;
    }

    public List<OffenceView> getOffences() {
        return ofNullable(offences).map(Collections::unmodifiableList).orElse(null);
    }

    public PersonDefendantView getPersonDefendant() {
        return personDefendant;
    }

    public List<DefendantAliasView> getAliases() {
        return ofNullable(aliases).map(Collections::unmodifiableList).orElse(null);
    }
    public LegalEntityDefendant getLegalEntityDefendant() {
        return legalEntityDefendant;
    }

    public static Builder defendantView() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID prosecutionCaseId;
        private Integer numberOfPreviousConvictionsCited;
        private String mitigation;
        private List<OffenceView> offences;
        private PersonDefendantView personDefendant;
        private List<DefendantAliasView> aliases;
        private LegalEntityDefendant legalEntityDefendant;
        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
            this.prosecutionCaseId = prosecutionCaseId;
            return this;
        }

        public Builder withNumberOfPreviousConvictionsCited(final Integer numberOfPreviousConvictionsCited) {
            this.numberOfPreviousConvictionsCited = numberOfPreviousConvictionsCited;
            return this;
        }

        public Builder withMitigation(final String mitigation) {
            this.mitigation = mitigation;
            return this;
        }

        public Builder withOffences(final List<OffenceView> offences) {
            this.offences = ofNullable(offences).map(LinkedList::new).orElse(null);
            return this;
        }

        public Builder withPersonDefendant(final PersonDefendantView personDefendant) {
            this.personDefendant = personDefendant;
            return this;
        }
        public LegalEntityDefendant getLegalEntityDefendant() {
            return legalEntityDefendant;
        }
        public Builder withAliases(final List<DefendantAliasView> defendantAliasViews) {
            this.aliases = ofNullable(defendantAliasViews).map(LinkedList::new).orElse(null);
            return this;
        }
        public Builder withLegalEntityDefendant(final LegalEntityDefendant legalEntityDefendant) {
            this.legalEntityDefendant = legalEntityDefendant;
            return this;
        }
        public DefendantView build() {
            return new DefendantView(id, prosecutionCaseId, numberOfPreviousConvictionsCited, mitigation,
                    offences, personDefendant, aliases,legalEntityDefendant);
        }
    }
}
