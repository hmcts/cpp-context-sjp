package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.LJA_CODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.LJA_CODE_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.LJA_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.LJA_NAME_KEY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.LJA_WELSH_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.TestConstants.LJA_WELSH_NAME_KEY;

import uk.gov.justice.core.courts.LjaDetails;

import java.util.Optional;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LJADetailsConverterTest {

    @InjectMocks
    LJADetailsConverter lJADetailsConverter;


    @Test
    public void shouldConvertLJADetails() {


        final LjaDetails ljaDetails = lJADetailsConverter.convert(getSjpSessionPayload(), getCourt());

        assertThat(ljaDetails.getLjaCode(), is(LJA_CODE));
        assertThat(ljaDetails.getLjaName(), is(LJA_NAME));
        assertThat(ljaDetails.getWelshLjaName(), is(LJA_WELSH_NAME));


    }


    public static JsonObject getSjpSessionPayload() {
        return JsonObjects.createObjectBuilder()
                .add(LJA_CODE_KEY, LJA_CODE)
                .build();
    }

    public static Optional<JsonObject> getCourt() {
        return Optional.of(JsonObjects.createObjectBuilder()
                .add(LJA_NAME_KEY, LJA_NAME)
                .add(LJA_WELSH_NAME_KEY, LJA_WELSH_NAME)

                .build());
    }

}
