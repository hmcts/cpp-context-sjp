package uk.gov.moj.cpp.sjp.event.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantPersonalDetailsUpdatedTest {

    @InjectMocks
    private DefendantPersonalDetailsChangesListener defendantPersonalDetailsChangesListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private DefendantPersonalNameUpdated defendantPersonalNameUpdated;

    @Mock
    private DefendantDateOfBirthUpdated defendantDateOfBirthUpdated;

    @Mock
    private DefendantAddressUpdated defendantAddressUpdated;

    @Mock
    private DefendantDetail defendantDetail;

    @Mock
    private PersonalDetails personalDetails;

    @Mock
    private CaseDetail caseDetail;

    @Test
    public void shouldUpdateDefendantNameChangedFlag() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantPersonalNameUpdated.class)).thenReturn(defendantPersonalNameUpdated);
        when(caseRepository.findBy(defendantPersonalNameUpdated.getCaseId())).thenReturn(caseDetail);
        when(caseDetail.getDefendant()).thenReturn(defendantDetail);
        when(defendantDetail.getPersonalDetails()).thenReturn(personalDetails);

        defendantPersonalDetailsChangesListener.defendantPersonalNameUpdated(envelope);

        verify(personalDetails).setNameChanged(Boolean.TRUE);
        verify(caseRepository).save(caseDetail);
    }

    @Test
    public void shouldUpdateDefendantDobChangedFlag() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantDateOfBirthUpdated.class)).thenReturn(defendantDateOfBirthUpdated);
        when(caseRepository.findBy(defendantDateOfBirthUpdated.getCaseId())).thenReturn(caseDetail);
        when(caseDetail.getDefendant()).thenReturn(defendantDetail);
        when(defendantDetail.getPersonalDetails()).thenReturn(personalDetails);

        defendantPersonalDetailsChangesListener.defendantDateOfBirthUpdated(envelope);

        verify(personalDetails).setDobChanged(Boolean.TRUE);
        verify(caseRepository).save(caseDetail);
    }

    @Test
    public void shouldUpdateDefendantAddressChangedFlag() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, DefendantAddressUpdated.class)).thenReturn(defendantAddressUpdated);
        when(caseRepository.findBy(defendantAddressUpdated.getCaseId())).thenReturn(caseDetail);
        when(caseDetail.getDefendant()).thenReturn(defendantDetail);
        when(defendantDetail.getPersonalDetails()).thenReturn(personalDetails);

        defendantPersonalDetailsChangesListener.defendantAddressUpdated(envelope);

        verify(personalDetails).setAddressChanged(Boolean.TRUE);
        verify(caseRepository).save(caseDetail);
    }


}
