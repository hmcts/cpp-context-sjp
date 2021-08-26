package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static java.util.Optional.ofNullable;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import javax.inject.Inject;
import javax.json.JsonObject;

public class EnforcementAreaService {

    @Inject
    private ReferenceDataService referenceDataService;

    @SuppressWarnings({"squid:S1186", "squid:S1602"})
    public JsonObject getEnforcementArea(final String defendantPostcode, final String localJusticeAreaNationalCourtCode, final JsonEnvelope sourceEnvelope) {
        return ofNullable(defendantPostcode)
                .flatMap(postcode -> referenceDataService.getEnforcementAreaByPostcode(postcode, sourceEnvelope))
                .orElseGet(() -> referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(localJusticeAreaNationalCourtCode, sourceEnvelope)
                        .orElseThrow(() -> new EnforcementAreaNotFoundException(defendantPostcode, localJusticeAreaNationalCourtCode)));

    }
}