package uk.gov.moj.cpp.sjp.query.view.service;

import static javax.json.Json.createReader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import uk.gov.justice.services.core.requester.Requester;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
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
