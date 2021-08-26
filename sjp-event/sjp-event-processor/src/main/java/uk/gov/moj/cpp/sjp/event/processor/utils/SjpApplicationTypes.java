package uk.gov.moj.cpp.sjp.event.processor.utils;

public enum SjpApplicationTypes {

    APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP("Appearance to make statutory declaration (SJP case)"),
    APPLICATION_TO_REOPEN_CASE("Application to reopen case"),
    APPEAL_AGAINST_CONVICTION("Appeal against conviction"),
    APPEAL_AGAINST_SENTENCE("Appeal against Sentence"),
    APPEAL_AGAINST_SENTENCE_AND_CONVICTION("Appeal against Sentence and Conviction");

    private String applicationType;

    private SjpApplicationTypes(final String applicationType) {
        this.applicationType = applicationType;
    }

    public String getApplicationType() {
        return applicationType;
    }
}
