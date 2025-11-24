package uk.gov.moj.cpp.sjp.query.view.exception;


public class ResultNotFoundException extends RuntimeException {

    private final String code;

    public ResultNotFoundException(final String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
