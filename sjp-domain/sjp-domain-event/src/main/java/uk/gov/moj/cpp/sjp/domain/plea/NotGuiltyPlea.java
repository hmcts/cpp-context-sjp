package uk.gov.moj.cpp.sjp.domain.plea;

public class NotGuiltyPlea {
    private final String notGuiltyBecause;
    private final LanguageInterpreter languageInterpreter;
    private final EvidenceOrWitnessDisagreement evidenceOrWitnessDisagreement;
    private final WitnessCall witnessCall;

    public NotGuiltyPlea(String notGuiltyBecause, LanguageInterpreter languageInterpreter,
                         EvidenceOrWitnessDisagreement evidenceOrWitnessDisagreement, WitnessCall witnessCall) {
        this.notGuiltyBecause = notGuiltyBecause;
        this.languageInterpreter = languageInterpreter;
        this.evidenceOrWitnessDisagreement = evidenceOrWitnessDisagreement;
        this.witnessCall = witnessCall;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

    public LanguageInterpreter getLanguageInterpreter() {
        return languageInterpreter;
    }

    public EvidenceOrWitnessDisagreement getEvidenceOrWitnessDisagreement() {
        return evidenceOrWitnessDisagreement;
    }

    public WitnessCall getWitnessCall() {
        return witnessCall;
    }
}