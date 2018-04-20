package uk.gov.moj.cpp.sjp.event.listener;

import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

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
public class DefendantDetailsMovedFromPeopleListenerTest {

    private static final UUID CASE_ID = UUID.randomUUID();

    private final CaseDetail aCase = new CaseDetail(CASE_ID);

    @InjectMocks
    private DefendantDetailsMovedFromPeopleListener listener;

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseSearchResultService caseSearchResultService;

    @Spy
    private JsonEnvelope eventEnvelope = JsonEnvelope.envelopeFrom(
            MetadataBuilderFactory.metadataWithRandomUUIDAndName(),
            createObjectBuilder()
                    .add("caseId", CASE_ID.toString())
                    .add("address", createObjectBuilder().build())
                    .add("contactNumber", createObjectBuilder().build())
                    .build());

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
                null,
                null,
                null,
                null
        );
    }
}
