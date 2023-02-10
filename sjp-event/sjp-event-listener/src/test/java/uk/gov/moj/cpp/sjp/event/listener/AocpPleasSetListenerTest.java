package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.AocpPleasSet;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AocpPleasSetListenerTest {

    @InjectMocks
    private AocpPleasSetListener listener;

    @Mock
    private CaseService caseService;

    @Mock
    private CaseRepository caseRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter converter = new JsonObjectToObjectConverter(objectMapper);

    @Test
    public void shouldHandleAocpPleasSet() throws JsonProcessingException {
        final UUID offenceId = randomUUID();
        final JsonEnvelope envelope = createJsonEnvelope(offenceId);

        final OffenceDetail offenceDetail = new OffenceDetail();
        offenceDetail.setId(offenceId);
        offenceDetail.setSequenceNumber(1);

        final DefendantDetail defendantDetail =new DefendantDetail();
        defendantDetail.setOffences(asList(offenceDetail));

        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setDefendant(defendantDetail);

        when(caseRepository.findBy(any())).thenReturn(new CaseDetail());
        when(caseService.findById(any())).thenReturn(caseDetail);

        listener.handleAocpPleasSet(envelope);

        verify(caseService).saveCaseDetail(caseDetail);
    }

    private JsonEnvelope createJsonEnvelope(final UUID offenceId) throws JsonProcessingException {
        final AocpPleasSet pleasSet = new AocpPleasSet(randomUUID(), asList(new Plea(randomUUID(), offenceId, PleaType.GUILTY)), ZonedDateTime.now(), PleaMethod.ONLINE);
        final String json = objectMapper.writeValueAsString(pleasSet);
        return createEnvelope("sjp.events.aocp-pleas-set", Json.createReader(new StringReader(json)).readObject());
    }
}
