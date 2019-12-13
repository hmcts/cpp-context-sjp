package uk.gov.moj.cpp.sjp.command.api.accesscontrol;

import static java.util.Collections.singletonList;

import java.util.Arrays;
import java.util.List;

public final class RuleConstants {

    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_ONLINE_PLEA_SYSTEM_USERS = "Online Plea System Users";
    private static final String GROUP_SJP_PROSECUTORS = "SJP Prosecutors";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";


    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getCreateSjpCaseActionGroups() {
        return Arrays.asList(GROUP_SJP_PROSECUTORS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getPleadOnlineActionGroups() {
        return Arrays.asList(GROUP_ONLINE_PLEA_SYSTEM_USERS);
    }

    public static List<String> getAddCaseDocumentActionGroups() {
        return Arrays.asList(GROUP_SYSTEM_USERS, GROUP_SJP_PROSECUTORS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateDefendantNationalInsuranceNumberGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUploadCaseDocumentActionGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_SYSTEM_USERS);
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

    public static List<String> getDeleteEmployerGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getCancelPleaActionGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static String[] getCaseReopenedActionGroups() {
        return new String[]{GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS};
    }

    public static List<String> getRequestWithdrawalOfAllOffencesGroups() {
        return singletonList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getCancelRequestWithdrawalOfAllOffencesGroups() {
        return singletonList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getActionCourtReferralGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateDefendantDetails() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateHearingRequirementsGroups() {
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getStartSessionGroups() {
        return singletonList(GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getAddDatesToAvoidActionGroups() {
        return singletonList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getEndSessionGroups() {
        return singletonList(GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getAssignCaseGroups() {
        return singletonList(GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getAcknowledgeDefendantDetailsUpdatesGroups() {
        return singletonList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getRequestTransparencyReportGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> datesToAvoidRequiredGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getAddCaseNoteGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getOffenceWithdrawalRequestActionGroups() {
        return Arrays.asList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getSetPleasGroups(){
        return Arrays.asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getSaveDecisionActionGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISERS);
    }

    public static List<String> resolveCaseStatusGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }
    public static List<String> getDeleteFinancialMeansGroups() {
        return Arrays.asList(GROUP_SYSTEM_USERS);
    }

}

