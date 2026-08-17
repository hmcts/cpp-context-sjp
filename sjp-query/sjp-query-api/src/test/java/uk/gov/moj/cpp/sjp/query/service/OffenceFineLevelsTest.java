package uk.gov.moj.cpp.sjp.query.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.math.BigDecimal;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceFineLevelsTest {

    final int fineLevel1 = 3;
    final int fineLevel2 = 5;

    final JsonArray offenceFineLevels = createArrayBuilder().add(
            createObjectBuilder().add("fineLevel", 1).add("maxValue", new BigDecimal(200))).add(
            createObjectBuilder().add("fineLevel", 2).add("maxValue", new BigDecimal(500))).add(
            createObjectBuilder().add("fineLevel", 3).add("maxValue", new BigDecimal(1000))).add(
            createObjectBuilder().add("fineLevel", 4).add("maxValue", new BigDecimal(2500))).add(
            createObjectBuilder().add("fineLevel", 5).add("maxValue", new BigDecimal(999999999.99))).build();


    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @BeforeEach
    public void init() {
        when(referenceDataService.getOffenceFineLevels(jsonEnvelope)).thenReturn(offenceFineLevels.getValuesAs(JsonObject.class));
    }

    @Test
    public void shouldGetCachedOffenceFineLevels() {

        final OffenceFineLevels offenceFineLevels = new OffenceFineLevels(referenceDataService, jsonEnvelope);

        assertThat(offenceFineLevels.getOffenceMaxFineValue(fineLevel1), is(Optional.of(new BigDecimal(1000))));
        assertThat(offenceFineLevels.getOffenceMaxFineValue(fineLevel2), is(Optional.of(new BigDecimal(999999999.99))));

        verify(referenceDataService, times(1)).getOffenceFineLevels(any());
    }
}
