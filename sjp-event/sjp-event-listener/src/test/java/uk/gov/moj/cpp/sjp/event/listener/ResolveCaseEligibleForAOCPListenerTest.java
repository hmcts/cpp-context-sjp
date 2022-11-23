package uk.gov.moj.cpp.sjp.event.listener;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static java.util.UUID.randomUUID;
import static java.util.Arrays.asList;
import static java.math.BigDecimal.valueOf;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.event.CaseEligibleForAOCP;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;

import java.math.BigDecimal;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResolveCaseEligibleForAOCPListenerTest {

    @Mock
    private CaseService caseService;

    @Mock
    OffenceRepository offenceRepository;

    @InjectMocks
    private ResolveCaseEligibleForAOCPListener resolveCaseEligibleForAOCPListener;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter converter = new JsonObjectToObjectConverter(objectMapper);

    @Captor
    private ArgumentCaptor<CaseDetail> caseDetailCaptor;

    @Test
    public void shouldUpdateAOCPCostDetailsForCaseAndOffence() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(caseId);

        final OffenceDetail offenceDetail1 = new OffenceDetail();
        offenceDetail1.setId(offenceId1);
        offenceDetail1.setSequenceNumber(1);

        final OffenceDetail offenceDetail2 = new OffenceDetail();
        offenceDetail2.setId(offenceId2);
        offenceDetail2.setSequenceNumber(2);

        final DefendantDetail defendantDetail = new DefendantDetail();
        defendantDetail.setId(defendantId);
        defendantDetail.setOffences(asList(offenceDetail1, offenceDetail2));

        caseDetail.setDefendant(defendantDetail);

        when(caseService.findById(caseId)).thenReturn(caseDetail);

        resolveCaseEligibleForAOCPListener.handleCaseEligibleForAOCP(createPayload(caseId, defendantId, offenceId1, offenceId2));

        verify(caseService).saveCaseDetail(caseDetailCaptor.capture());

        final CaseDetail caseDetail1 = caseDetailCaptor.getValue();
        assertThat(caseDetail1.getId(), is(caseId));
        assertThat(caseDetail1.getAocpVictimSurcharge(), is(caseDetail.getAocpVictimSurcharge()));
        assertThat(caseDetail1.getAocpEligible(), is(true));

        OffenceDetail offenceDetailUpdated1 = caseDetail.getDefendant().getOffences().get(0);
        assertThat(offenceDetailUpdated1.getId(), is(offenceId1));
        assertThat(offenceDetailUpdated1.getAocpStandardPenalty(), is(valueOf(100)));

        OffenceDetail offenceDetailUpdated2 = caseDetail.getDefendant().getOffences().get(1);
        assertThat(offenceDetailUpdated2.getId(), is(offenceId2));
        assertThat(offenceDetailUpdated2.getAocpStandardPenalty(), is(valueOf(30)));
    }

    private JsonEnvelope createPayload(final UUID caseId, final UUID defendantId, final UUID offenceId1, final UUID offenceId2) {
        final JsonObject caseReceivedEventPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("costs", 5)
                .add("victimSurcharge", 34)
                .add("defendant", createObjectBuilder()
                        .add("id", defendantId.toString())
                        .add("offences", createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("id", offenceId1.toString())
                                        .add("compensation", 40)
                                        .add("aocpStandardPenaltyAmount", 100)
                                )
                                .add(createObjectBuilder()
                                        .add("id", offenceId2.toString())
                                        .add("compensation", 40)
                                        .add("aocpStandardPenaltyAmount", 30)
                                )
                        )
                )
                .build();

        return EnvelopeFactory.createEnvelope(CaseEligibleForAOCP.EVENT_NAME, caseReceivedEventPayload);
    }
}
