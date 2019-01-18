package uk.gov.moj.cpp.sjp.domain.transformation.datecreated.exception;

public class TransformationException extends RuntimeException {

    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformationException(String message) {
        super(message);
    }
}
