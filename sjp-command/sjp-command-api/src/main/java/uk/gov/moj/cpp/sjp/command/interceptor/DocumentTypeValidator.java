package uk.gov.moj.cpp.sjp.command.interceptor;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class DocumentTypeValidator {

    private static Pattern fileExtnPtrn = Pattern
            .compile("([^\\s]+(\\.(?i)(doc|docx|jpg|jpeg|pdf|txt))$)");

    public boolean isValid(final String fileName) {
        if (StringUtils.isNotEmpty(fileName)
                && fileExtnPtrn.matcher(fileName.trim()).matches()) {
            return true;
        }
        return false;
    }
}