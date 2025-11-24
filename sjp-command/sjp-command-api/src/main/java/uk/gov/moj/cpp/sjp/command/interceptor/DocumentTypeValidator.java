package uk.gov.moj.cpp.sjp.command.interceptor;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class DocumentTypeValidator {

    private final List<String> fileExtensions = Arrays.asList("doc", "docx", "jpg", "jpeg", "pdf", "txt");

    public boolean isValid(final String fileName) {
        if (StringUtils.isNotEmpty(fileName)) {
            final String extension = FilenameUtils.getExtension(fileName).toLowerCase();
            if (fileExtensions.contains(extension)) {
                return true;
            }
        }
        return false;
    }
}