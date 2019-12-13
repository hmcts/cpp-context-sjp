package uk.gov.moj.cpp.sjp.query.api;

import static java.util.Arrays.asList;

import java.util.Collections;
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

    public static List<String> getQueryCaseActionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_SJP_PROSECUTORS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCaseByUrnActionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_SJP_PROSECUTORS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCaseByUrnPostcodeActionGroups() {
        return Collections.singletonList(GROUP_ONLINE_PLEA_SYSTEM_USERS);
    }

    public static List<String> getQueryCasesMissingSjpnActionGroups() {
        return asList(GROUP_SJP_PROSECUTORS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getQueryFinancialMeansActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryEmployerActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryFindCaseSearchResultsActionGroups() {
        return asList(GROUP_SJP_PROSECUTORS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getQueryCaseDocumentsActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getQueryCasesSearchByMaterialIdActionGroups() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryResultOrders() {
        return asList(GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryFindCasesActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getQueryDefendantsOnlinePleaGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getQueryPendingDatesToAvoidActionGroups() {
        return asList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getQuerySessionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SJP_PROSECUTORS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryCaseAssignmentGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getReadyCasesGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getProsecutingAuthorityGroups() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryDefendantDetailsUpdates() {
        return asList(GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getCaseNotesGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getAllowedGroupsForTransparencyReport() {
        return asList(GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getAllowedGroupsForVerdictCalculation() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getAllowedGroupsForCourtExtract() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getAllowedGroupsForCaseResults() {
        return asList(GROUP_SYSTEM_USERS, GROUP_SJP_PROSECUTORS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

}
