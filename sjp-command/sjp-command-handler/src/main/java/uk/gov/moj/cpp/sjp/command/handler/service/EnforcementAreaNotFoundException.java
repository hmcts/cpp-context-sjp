package uk.gov.moj.cpp.sjp.command.handler.service;

public class EnforcementAreaNotFoundException extends RuntimeException {

    public EnforcementAreaNotFoundException(final String message) {
        super(message);
    }

    public EnforcementAreaNotFoundException(final String postcode, final String localJusticeAreaNationalCourtCode) {
        this(String.format("Enforcement area not found for postcode = %s nor local justice area national court code = %s", postcode, localJusticeAreaNationalCourtCode));
    }
}
