package uk.gov.moj.cpp.sjp.transformation.exception;

/**
 * Thrown if the transformed event doesn't pass schema validation.
 */
public class TransformationException extends RuntimeException {

    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
