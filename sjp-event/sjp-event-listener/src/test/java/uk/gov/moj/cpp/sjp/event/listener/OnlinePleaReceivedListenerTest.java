package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.listener.converter.DefendantToDefendantDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OnlinePleaReceivedListenerTest {

    @Mock
    private CaseRepository caseRepository;

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    @InjectMocks
    private DefendantToDefendantDetails defendantToDefendantDetailsConverter = new DefendantToDefendantDetails();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private OnlinePleaRepository.PersonDetailsOnlinePleaRepository onlinePleaRepository;

    @InjectMocks
    private OnlinePleaReceivedListener onlinePleaReceivedListener;

    @BeforeEach
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
                .withPayloadOf(createObjectBuilder()
                        .add("organisation", createObjectBuilder()
                                        .add("name", "Test organization")
                                        .add("address", createObjectBuilder()
                                                .add("address1", "test")
                                                .build())
                                        .build())
                                .build(), "legalEntityDefendant")
                .withPayloadOf(createObjectBuilder().add("tradingMordThan12Months", true).build(), "legalEntityFinancialMeans")
                .build();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(caseId);

        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        onlinePleaReceivedListener.onlinePleaReceived(event);

        verify(caseRepository).findBy(caseId);
        verify(caseRepository).save(caseDetail);
        verify(onlinePleaRepository).saveOnlinePlea(any(OnlinePlea.class));
    }
}
