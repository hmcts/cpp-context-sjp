package uk.gov.moj.cpp.sjp.event.processor.utils;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;

public enum SjpApplicationTypes {

    APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP("Appearance to make statutory declaration (SJP case)","MC80528"),
    APPLICATION_TO_REOPEN_CASE("Application to reopen case","MC80524"),
    APPEAL_AGAINST_CONVICTION("Appeal against conviction", StringUtils.EMPTY),
    APPEAL_AGAINST_SENTENCE("Appeal against Sentence", StringUtils.EMPTY),
    APPEAL_AGAINST_SENTENCE_AND_CONVICTION("Appeal against Sentence and Conviction", StringUtils.EMPTY),
    APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT("Appeal against sentence by a Magistrates' Court to the Crown Court","MC80803"),
    APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP("Appearance to make statutory declaration (other than SJP)","MC80527"),
    APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT("Appeal against conviction and sentence by a Magistrates' Court to the Crown Court","MC80801"),
    APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT("Appeal against conviction by a Magistrates' Court to the Crown Court","MC80802");


    private String applicationType;

    private String applicationCode;

    private SjpApplicationTypes(final String applicationType, final String applicationCode) {
        this.applicationType = applicationType;
        this.applicationCode = applicationCode;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public String getApplicationCode() {
        return applicationCode;
    }
}
