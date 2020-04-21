package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import java.util.List;
import java.util.UUID;

public class DefendantView {

    private final UUID id;
    private final UUID prosecutionCaseId;
    private final Integer numberOfPreviousConvictionsCited;
    private final String mitigation;
    private final List<OffenceView> offences;
    private final PersonDefendantView personDefendant;
    private final List<DefendantAliasView> aliases;

    public DefendantView(final UUID id,
                         final UUID prosecutionCaseId,
                         final Integer numberOfPreviousConvictionsCited,
                         final String mitigation,
                         final List<OffenceView> offences,
                         final PersonDefendantView personDefendant,
                         final List<DefendantAliasView> aliases) {

        this.id = id;
        this.prosecutionCaseId = prosecutionCaseId;
        this.numberOfPreviousConvictionsCited = numberOfPreviousConvictionsCited;
        this.mitigation = mitigation;
        this.offences = offences;
        this.personDefendant = personDefendant;
        this.aliases = aliases;
    }

    public UUID getId() {
        return id;
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
        return offences;
    }

    public PersonDefendantView getPersonDefendant() {
        return personDefendant;
    }

    public List<DefendantAliasView> getAliases() {
        return aliases;
    }
}
