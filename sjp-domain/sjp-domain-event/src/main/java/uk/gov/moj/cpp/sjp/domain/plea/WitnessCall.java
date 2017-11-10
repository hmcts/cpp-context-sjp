package uk.gov.moj.cpp.sjp.domain.plea;

public class WitnessCall {

    private String contactDetails;
    private LanguageInterpreter languageInterpreter;

    public WitnessCall() {
        //default constructor
    }

    public WitnessCall(String contactDetails, LanguageInterpreter languageInterpreter) {
        this.contactDetails = contactDetails;
        this.languageInterpreter = languageInterpreter;
    }

    public String getContactDetails() {
        return contactDetails;
    }

    public LanguageInterpreter getLanguageInterpreter() {
        return languageInterpreter;
    }
}
