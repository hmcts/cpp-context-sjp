package uk.gov.moj.cpp.sjp.command.handler.service;

import static java.util.Optional.ofNullable;

import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

public class EnforcementAreaService {

    @Inject
    private ReferenceDataService referenceDataService;

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public JsonObject getEnforcementArea(final String defendantPostcode, final String localJusticeAreaNationalCourtCode, final JsonEnvelope sourceEnvelope) {
        return ofNullable(defendantPostcode)
                .flatMap(postcode -> referenceDataService.getEnforcementAreaByPostcode(postcode, sourceEnvelope))
                .orElseGet(() -> referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(localJusticeAreaNationalCourtCode, sourceEnvelope)
                        .orElseThrow(() -> new EnforcementAreaNotFoundException(defendantPostcode, localJusticeAreaNationalCourtCode)));

    }
}
