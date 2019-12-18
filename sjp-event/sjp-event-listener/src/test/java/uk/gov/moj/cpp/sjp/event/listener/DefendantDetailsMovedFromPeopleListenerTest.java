package uk.gov.moj.cpp.sjp.event.listener;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.event.listener.converter.ContactDetailsToContactDetailsEntity;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantDetailsMovedFromPeopleListenerTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    private final CaseDetail aCase = new CaseDetail(CASE_ID);
    @Spy
    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final JsonEnvelope eventEnvelope = JsonEnvelope.envelopeFrom(
            MetadataBuilderFactory.metadataWithRandomUUIDAndName(),
            createObjectBuilder()
                    .add("caseId", CASE_ID.toString())
                    .add("address", createObjectBuilder().build())
                    .add("contactNumber", createObjectBuilder().build())
                    .build());
    @InjectMocks
    private DefendantDetailsMovedFromPeopleListener listener;
    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseSearchResultService caseSearchResultService;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AddressToAddressEntity addressToAddressEntity;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(caseRepository.findBy(CASE_ID)).thenReturn(aCase);
    }

    @Test
    public void shouldUpdateDefendantDetails() {
        // when
        listener.defendantDetailsUpdated(eventEnvelope);

        // then
        verify(caseRepository).save(aCase);
        verify(caseSearchResultService).onDefendantDetailsUpdated(
                CASE_ID,
                aCase.getDefendant().getId(),
                null,
                null,
                null,
                null
        );
    }
}
