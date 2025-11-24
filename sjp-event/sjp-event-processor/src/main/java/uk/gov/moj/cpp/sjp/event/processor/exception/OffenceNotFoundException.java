package uk.gov.moj.cpp.sjp.event.processor.exception;

public class OffenceNotFoundException extends RuntimeException {

    private final String offenceCode;

    public OffenceNotFoundException(final String offenceCode) {
        super(String.format("Offence %s not found", offenceCode));
        this.offenceCode = offenceCode;
    }

    public String getOffenceCode() {
        return offenceCode;
    }
}
