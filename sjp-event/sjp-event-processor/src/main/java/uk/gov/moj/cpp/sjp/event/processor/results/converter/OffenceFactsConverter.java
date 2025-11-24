package uk.gov.moj.cpp.sjp.event.processor.results.converter;


import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;


public class OffenceFactsConverter {
    public OffenceFacts getOffenceFacts(final Offence offence) {
        return OffenceFacts.offenceFacts()
                .withVehicleMake(offence.getVehicleMake())
                .withVehicleRegistration(offence.getVehicleRegistrationMark())
                .build();
    }
}
