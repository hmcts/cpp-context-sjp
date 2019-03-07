package uk.gov.moj.cpp.sjp.domain.transformation.offencedate;

/**
 * Thrown if the transformed event doesn't pass schema validation.
 */
public class TransformationException extends RuntimeException {

    public TransformationException(String message) {
        super(message);
    }
}
