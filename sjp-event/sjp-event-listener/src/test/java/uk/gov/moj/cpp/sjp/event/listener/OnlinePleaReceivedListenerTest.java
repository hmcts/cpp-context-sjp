package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OnlinePleaReceivedListenerTest {

    @Mock
    private CaseRepository caseRepository;

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private OnlinePleaRepository.PersonDetailsOnlinePleaRepository onlinePleaRepository;

    @InjectMocks
    private OnlinePleaReceivedListener onlinePleaReceivedListener;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldUpdatePlea() {
        final UUID caseId = randomUUID();
        final JsonEnvelope event = envelope()
                .with(metadataBuilder()
                        .withName("sjp.events.online-plea-received")
                        .withId(randomUUID())
                        .createdAt(now()))
                .withPayloadOf(caseId.toString(), "caseId")
                .build();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(caseId);

        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        final PleadOnline pleadOnline = new PleadOnline(caseDetail.getDefendant().getId(), null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null);
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), PleadOnline.class)).thenReturn(pleadOnline);

        onlinePleaReceivedListener.onlinePleaReceived(event);

        verify(caseRepository).findBy(caseId);
        verify(caseRepository).save(caseDetail);
        verify(onlinePleaRepository).saveOnlinePlea(any(OnlinePlea.class));
    }
}
