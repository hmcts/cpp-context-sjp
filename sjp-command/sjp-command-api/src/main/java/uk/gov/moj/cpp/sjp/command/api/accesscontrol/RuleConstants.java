package uk.gov.moj.cpp.sjp.command.api.accesscontrol;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.List;

public final class RuleConstants {

    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_ONLINE_PLEA_SYSTEM_USERS = "Online Plea System Users";
    private static final String GROUP_SJP_PROSECUTORS = "SJP Prosecutors";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
    private static final String GROUP_MAGISTRATES = "Magistrates";


    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getCreateSjpCaseActionGroups() {
        return asList(GROUP_SJP_PROSECUTORS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getPleadOnlineActionGroups() {
        return asList(GROUP_ONLINE_PLEA_SYSTEM_USERS);
    }

    public static List<String> getAddCaseDocumentActionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_SJP_PROSECUTORS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateDefendantNationalInsuranceNumberGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getUploadCaseDocumentActionGroups() {
        return asList(GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getUpdatePleaActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateFinancialMeansGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getUpdateEmployerGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getDeleteEmployerGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getCancelPleaActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
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
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getUpdateDefendantDetails() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS, GROUP_MAGISTRATES);
    }

    public static List<String> getUpdateHearingRequirementsGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getStartSessionGroups() {
        return asList(GROUP_LEGAL_ADVISERS, GROUP_MAGISTRATES);
    }

    public static List<String> getStartAocpSessionGroups() {
        return asList(GROUP_LEGAL_ADVISERS, GROUP_MAGISTRATES, GROUP_SYSTEM_USERS);
    }

    public static List<String> getAddDatesToAvoidActionGroups() {
        return singletonList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getEndSessionGroups() {
        return asList(GROUP_LEGAL_ADVISERS, GROUP_MAGISTRATES);
    }

    public static List<String> getEndAocpSessionGroups() {
        return asList(GROUP_LEGAL_ADVISERS, GROUP_MAGISTRATES, GROUP_SYSTEM_USERS);
    }

    public static List<String> getAssignCaseGroups() {
        return asList(GROUP_LEGAL_ADVISERS, GROUP_MAGISTRATES);
    }

    public static List<String> getAcknowledgeDefendantDetailsUpdatesGroups() {
        return singletonList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getRequestTransparencyReportGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getRequestPressTransparencyReportGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getRequestDeleteDocsGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> datesToAvoidRequiredGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getAddCaseNoteGroups() {
        return asList(GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getOffenceWithdrawalRequestActionGroups() {
        return asList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getSetPleasGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS, GROUP_MAGISTRATES);
    }

    public static List<String> getSaveDecisionActionGroups() {
        return asList(GROUP_LEGAL_ADVISERS, GROUP_MAGISTRATES);
    }

    public static List<String> getAocpSaveDecisionActionGroups() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> resolveCaseStatusGroups() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> reserveCaseGroups() {
        return singletonList(GROUP_LEGAL_ADVISERS);
    }

    public static List<String> undoReserveCaseGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getDeleteFinancialMeansGroups() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> updateCasesManagementStatusGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getAddCaseAssignmentRestrictionActionGroups() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getAddRequestForOutstandingFinesGroups() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getCreateCaseApplicationGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getAddFinancialImpositionCorrelationIdGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_MAGISTRATES);
    }

    public static List<String> getAddFinancialImpositionAccountNumberBdfGroups() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getSystemUsers() {
        return singletonList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getMarkAsLegalSocCheckedGroups() {
        return asList(GROUP_LEGAL_ADVISERS);
    }

}
