package uk.gov.moj.cpp.sjp.event.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusCreated;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.persistence.entity.ApplicationStatus.APPEAL_PENDING;

@ExtendWith(MockitoExtension.class)
public class CCApplicationStatusListenerTest {

    @InjectMocks
    private CCApplicationStatusListener listener;

    @Mock
    private CaseService caseService;

    @Captor
    private ArgumentCaptor<CaseDetail> caseDetailArgumentCaptor;


    private final static UUID CASE_ID = randomUUID();

    private final static UUID APPLICATION_ID = randomUUID();

    private CaseDetail caseDetail;


    @BeforeEach
    public void setUp() {
        caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
    }

    @Test
    public void shouldSaveCCApplicationStatusForCaseWhenCCApplicationStatusCreatedEventRaised() {
        when(caseService.findById(CASE_ID)).thenReturn(caseDetail);
        final CCApplicationStatusCreated ccApplicationStatusCreated = CCApplicationStatusCreated.ccApplicationStatusCreated()
                .withCaseId(CASE_ID)
                .withApplicationId(APPLICATION_ID)
                .withStatus(ApplicationStatus.APPEAL_PENDING)
                .build();

        final Envelope<CCApplicationStatusCreated> applicationStatusCreatedEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.cc-application-status-created"),
                ccApplicationStatusCreated);

        listener.handleCCApplicationStatusCreated(applicationStatusCreatedEnvelope);
        verify(caseService).saveCaseDetail(caseDetailArgumentCaptor.capture());

        final CaseDetail caseDetail = caseDetailArgumentCaptor.getValue();
        assertThat(caseDetail.getCcApplicationStatus(), equalTo(APPEAL_PENDING));

    }

    @Test
    public void shouldSaveCCApplicationStatusForCaseWhenCCApplicationStatusUpdatedEventRaised() {
        when(caseService.findById(CASE_ID)).thenReturn(caseDetail);
        final CCApplicationStatusUpdated ccApplicationStatusUpdated = CCApplicationStatusUpdated.ccApplicationStatusUpdated()
                .withCaseId(CASE_ID)
                .withApplicationId(APPLICATION_ID)
                .withStatus(ApplicationStatus.APPEAL_PENDING)
                .build();

        final Envelope<CCApplicationStatusUpdated> applicationStatusUpdatedEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.cc-application-status-updated"),
                ccApplicationStatusUpdated);

        listener.handleCCApplicationStatusUpdated(applicationStatusUpdatedEnvelope);
        verify(caseService).saveCaseDetail(caseDetailArgumentCaptor.capture());

        final CaseDetail caseDetail = caseDetailArgumentCaptor.getValue();
        assertThat(caseDetail.getCcApplicationStatus(), equalTo(APPEAL_PENDING));

    }




}
