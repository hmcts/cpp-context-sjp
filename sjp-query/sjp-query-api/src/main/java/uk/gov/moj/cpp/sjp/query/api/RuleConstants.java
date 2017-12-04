package uk.gov.moj.cpp.sjp.query.api;

import static java.util.Arrays.asList;

import java.util.List;

public final class RuleConstants {

    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_TFL_USERS = "TFL Users";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";

    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getQueryCaseActionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_TFL_USERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCaseByUrnActionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_TFL_USERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQuerySjpCaseByUrnActionGroups() {
        return asList(GROUP_TFL_USERS, GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCaseByUrnPostcodeActionGroups() {
        //TODO replace with online-plea user
        return asList(GROUP_TFL_USERS, GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCasesSearchActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_TFL_USERS);
    }

    public static List<String> getQueryCasesMissingSjpnActionGroups() {
        return asList(GROUP_TFL_USERS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getQueryFinancialMeansActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryEmployerActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryFindCaseSearchResultsActionGroups() {
        return asList(GROUP_TFL_USERS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }


    public static List<String> getQueryCaseDocumentsActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_TFL_USERS);
    }

    public static List<String> getQueryCaseDefendantsActionGroups() {
        return asList(GROUP_SYSTEM_USERS, GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getQueryCasesSearchByMaterialIdActionGroups() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryAwaitingCasesActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryCasesReferredToCourtActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getNotReadyCasesGroupedByAgeActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getOldestCaseAgeActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_SYSTEM_USERS);
    }

    public static List<String> getQueryResultOrders() {
        return asList(GROUP_COURT_ADMINISTRATORS);
    }

    public static List<String> getQueryFindCasesActionGroups() {
        return asList(GROUP_COURT_ADMINISTRATORS, GROUP_LEGAL_ADVISERS, GROUP_TFL_USERS);
    }
}
