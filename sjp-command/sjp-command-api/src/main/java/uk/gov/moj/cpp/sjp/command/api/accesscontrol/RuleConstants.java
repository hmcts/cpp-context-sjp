package uk.gov.moj.cpp.sjp.command.api.accesscontrol;

import java.util.Arrays;
import java.util.List;

public final class RuleConstants {

    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_TFL_USERS = "TFL Users";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";

    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getCreateSjpCaseActionGroups() {
        return Arrays.asList(GROUP_TFL_USERS);
    }

    public static List<String> getAddCaseDocumentActionGroups() {
        return Arrays.asList(GROUP_SYSTEM_USERS, GROUP_TFL_USERS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUploadCaseDocumentActionGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getUpdatePleaActionGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateFinancialMeansGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateEmployerGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getCancelPleaActionGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static String[] getCaseReopenedActionGroups() {
        return new String[]{GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS};
    }

    public static List<String> getRequestWithdrawalOfAllOffencesGroups() {
        return Arrays.asList(GROUP_TFL_USERS);
    }

    public static List<String> getCancelRequestWithdrawalOfAllOffencesGroups() {
        return Arrays.asList(GROUP_TFL_USERS);
    }

    public static List<String> getAddPersonInfoGroups() {
        return Arrays.asList(GROUP_TFL_USERS);
    }


    public static List<String> getActionCourtReferralGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateDefendantDetails() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }
}

