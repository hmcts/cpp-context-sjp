package uk.gov.moj.cpp.sjp.command.controller.accesscontrol;

import java.util.Arrays;
import java.util.List;

public final class RuleConstants {

    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
    private static final String GROUP_SJP_PROSECUTORS = "SJP Prosecutors";
    private static final String SECOND_LINE_SUPPORT = "Second Line Support";

    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getUploadCaseDocumentActionGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS, GROUP_SYSTEM_USERS, GROUP_SJP_PROSECUTORS);
    }

    public static List<String> getMarkAsLegalSocCheckedActionGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISERS);
    }

    public static List<String> getDeleteCaseDocumentActionGroups() {
        return Arrays.asList(SECOND_LINE_SUPPORT);
    }
}

