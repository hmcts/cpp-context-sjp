package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.event.DefendantAcceptedAocp;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpOnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.util.Arrays;
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
public class DefendantAcceptedAocpListenerTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private OffenceRepository offenceRepository;

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private OnlinePleaRepository.PersonDetailsOnlinePleaRepository onlinePleaRepository;

    @Mock
    private AocpOnlinePleaRepository.PersonDetailsOnlinePleaRepository aocpOnlinePleaRepository;

    @Mock
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    @InjectMocks
    private DefendantAcceptedAocpListener defendantAcceptedAocpListener;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldUpdateDefendantAcceptedAOCP() {
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final JsonEnvelope event = envelope()
                .with(metadataBuilder()
                        .withName("sjp.events.defendant-accepted-aocp")
                        .withId(randomUUID())
                        .createdAt(now()))
                .withPayloadOf(caseId.toString(), "caseId")
                .build();

        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(caseId);

        final OffenceDetail offenceDetail = new OffenceDetail();
        offenceDetail.setPlea(NOT_GUILTY);

        final Offence offence = new Offence(offenceId, NOT_GUILTY, null, null);

        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(offenceRepository.findBy(offenceId)).thenReturn(offenceDetail);

        final DefendantAcceptedAocp defendantAcceptedAocp = new DefendantAcceptedAocp(caseDetail.getId(), null,
                Arrays.asList(offence), null, null, true, null, null);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), DefendantAcceptedAocp.class)).thenReturn(defendantAcceptedAocp);

        defendantAcceptedAocpListener.handleDefendantAcceptedAocp(event);

        verify(caseRepository).findBy(caseId);
        verify(onlinePleaDetailRepository).save(any(OnlinePleaDetail.class));
        verify(aocpOnlinePleaRepository).save(any(AocpOnlinePlea.class));
        verify(caseRepository).save(any(CaseDetail.class));
    }
}
