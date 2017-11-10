package uk.gov.moj.cpp.sjp.command.controller.accesscontrol;

import java.util.Arrays;
import java.util.List;

public final class RuleConstants {

    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";

    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static List<String> getUploadCaseDocumentActionGroups() {
        return Arrays.asList(GROUP_LEGAL_ADVISERS, GROUP_COURT_ADMINISTRATORS);
    }
}

