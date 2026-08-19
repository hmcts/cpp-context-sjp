package uk.gov.moj.cpp.sjp.query.view.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import uk.gov.justice.services.core.requester.Requester;

import java.util.Objects;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataCachingServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private ReferenceDataCachingService cache;

    @Test
    public void shouldGetAllProsecutors() {
        when(referenceDataService.getAllProsecutors()).thenReturn(prosecutorsResponseEnvelope());
        assertTrue(Objects.nonNull(cache.getAllProsecutors()));
        assertEquals(1, cache.getAllProsecutors().size());
    }

    private Optional<JsonArray> prosecutorsResponseEnvelope() {
        final JsonObject prosecutorsData = createReader(getClass().getClassLoader().getResourceAsStream("prosecutors-ref-data.json")).readObject();
        return Optional.ofNullable(prosecutorsData.getJsonArray("prosecutors"));
    }

}
