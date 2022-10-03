package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.event.listener.converter.DefendantToDefendantDetails;
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
    @InjectMocks
    private DefendantToDefendantDetails defendantToDefendantDetailsConverter = new DefendantToDefendantDetails();

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
        final Address address = new Address("test", "test1", "test2", "test3", "test4", "OX4 3ED");
        final ContactDetails contactDetails = new ContactDetails("01885654327", "018856443222", "12388383", "criminal@criminal.com", null);


        final PleadOnline pleadOnline = new PleadOnline(caseDetail.getDefendant().getId(), null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                LegalEntityDefendant.legalEntityDefendant().withAdddres(address).withContactDetails(contactDetails).withPosition(null).withName("test").build(),
                null
        );
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), PleadOnline.class)).thenReturn(pleadOnline);

        onlinePleaReceivedListener.onlinePleaReceived(event);

        verify(caseRepository).findBy(caseId);
        verify(caseRepository).save(caseDetail);
        verify(onlinePleaRepository).saveOnlinePlea(any(OnlinePlea.class));
    }
}
