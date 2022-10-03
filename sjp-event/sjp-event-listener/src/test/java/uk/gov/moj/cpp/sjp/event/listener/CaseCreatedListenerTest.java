package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.event.listener.converter.CaseReceivedToCase;
import uk.gov.moj.cpp.sjp.event.listener.converter.SjpCaseCreatedToCase;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseCreatedListenerTest {

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    @SuppressWarnings("deprecation")
    private SjpCaseCreatedToCase sjpConverter;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private SjpCaseCreated sjpEvent;

    @Mock
    private CaseDetail caseDetail;

    @Mock
    private JsonObject payload;

    @Mock
    private CaseReceived caseReceived;

    @Mock
    private CaseReceivedToCase caseReceivedToCaseConverter;

    @InjectMocks
    private CaseCreatedListener listener;

    @Mock
    private CaseSearchResultService caseSearchResultService;

    @Test
    @SuppressWarnings("deprecation")
    public void shouldHandleCreateSjpCaseEvent() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, SjpCaseCreated.class)).thenReturn(sjpEvent);
        when(sjpConverter.convert(sjpEvent)).thenReturn(caseDetail);

        listener.sjpCaseCreated(envelope);

        verify(caseRepository).save(caseDetail);
    }

    @Test
    public void shouldHandleCaseReceivedEvent(){
        givenCaseReceivedEventIsRaised();
        whenTheListenerGetsTheEvent();
    }

    protected void whenTheListenerGetsTheEvent() {
        listener.caseReceived(envelope);
    }

    protected void givenCaseReceivedEventIsRaised() {
        DefendantDetail defendantDetail = mock(DefendantDetail.class);
        PersonalDetails personalDetails = mock(PersonalDetails.class);
        LegalEntityDetails legalEntityDetails=mock(LegalEntityDetails.class);
        when(defendantDetail.getPersonalDetails()).thenReturn(personalDetails);
        when(defendantDetail.getLegalEntityDetails()).thenReturn(legalEntityDetails);
        when(caseDetail.getDefendant()).thenReturn(defendantDetail);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, CaseReceived.class)).thenReturn(caseReceived);
        when(caseReceivedToCaseConverter.convert(caseReceived)).thenReturn(caseDetail);
    }

}