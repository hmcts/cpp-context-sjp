package uk.gov.moj.cpp.sjp.command.interceptor;

public class SjpDocumentUploadException extends RuntimeException {

    public SjpDocumentUploadException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
