package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.apache.log4j.spi.Configurator.NULL;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.ConverterUtils.getString;

import uk.gov.justice.core.courts.LjaDetails;

import java.util.Optional;

import javax.json.JsonObject;

public class LJADetailsConverter {
    private static final String LJA_CODE = "localJusticeAreaNationalCourtCode";
    private static final String LJA_NAME = "oucodeL3Name";
    private static final String LJA_WELSH_NAME = "oucodeL3WelshName";

    public LjaDetails convert(final JsonObject sjpSessionPayload,
                              final Optional<JsonObject> court) {

        final LjaDetails.Builder ljaDetails = LjaDetails.ljaDetails();
        if (court.isPresent()) {
            ljaDetails.withLjaName(getString(court.get(), LJA_NAME));
            ljaDetails.withWelshLjaName(getString(court.get(), LJA_WELSH_NAME));
        }

        if (sjpSessionPayload != null) {
            ljaDetails.withLjaCode(sjpSessionPayload.containsKey(LJA_CODE) ? sjpSessionPayload.getString(LJA_CODE, NULL) : NULL);//Mandatory
        }
        return ljaDetails.build();
    }
}
