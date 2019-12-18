package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration;

public class TransformationException extends RuntimeException {

    public TransformationException(String message) {
        super(message);
    }

    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
