package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.io.StringReader;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleasSetListenerTest {

    @InjectMocks
    private PleasSetListener listener;

    @Mock
    private CaseRepository caseRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter converter = new JsonObjectToObjectConverter(objectMapper);

    private final static String DISABILITY_NEEDS = "Hearing aid";

    @BeforeEach
    public void setUp() {
        when(caseRepository.findBy(any(UUID.class))).thenReturn(new CaseDetail());
    }

    @Test
    public void shouldSaveDisabilityNeedsForDefendant() throws JsonProcessingException {
        JsonEnvelope envelope = givenPleasSetEventWasRaised();
        whenItIsPickedUpByListener(envelope);
        itStoresDisabilityNeedsDetails();
    }

    private void itStoresDisabilityNeedsDetails() {
        final ArgumentCaptor<CaseDetail> caseDetailArgumentCaptor = ArgumentCaptor.forClass(CaseDetail.class);
        verify(caseRepository).save(caseDetailArgumentCaptor.capture());
        assertThat(caseDetailArgumentCaptor.getValue().getDefendant().getDisabilityNeeds(), is(DISABILITY_NEEDS));
    }

    private void whenItIsPickedUpByListener(final JsonEnvelope envelope) {
        listener.updateDisabilityNeeds(envelope);
    }

    private JsonEnvelope givenPleasSetEventWasRaised() throws JsonProcessingException {
        final PleasSet pleasSet = new PleasSet(randomUUID(),
                new DefendantCourtOptions(null,false, disabilityNeedsOf(DISABILITY_NEEDS)),
                singletonList(new Plea(randomUUID(), randomUUID(), PleaType.GUILTY)));

        return createEnvelope("sjp.events.pleas-set", convertToJsonObject(pleasSet));

    }

    public JsonObject convertToJsonObject(PleasSet pleasSet) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(pleasSet);
        return Json.createReader(new StringReader(json)).readObject();
    }



}
