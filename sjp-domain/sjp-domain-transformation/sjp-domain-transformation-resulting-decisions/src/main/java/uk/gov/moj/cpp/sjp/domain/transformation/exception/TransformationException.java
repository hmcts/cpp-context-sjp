package uk.gov.moj.cpp.sjp.domain.transformation.exception;

public class TransformationException extends RuntimeException {

    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformationException(String message) {
        super(message);
    }
}
