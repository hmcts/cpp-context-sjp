package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZoneOffset.UTC;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.time.ZonedDateTime;

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
    private JsonObject payload;

    @Mock
    private DefendantNameUpdated defendantNameUpdated;

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
    public void shouldUseEventCreatedAtWhenUpdatedAtNotPresentInEvent() {
        ZonedDateTime eventCreationTime = ZonedDateTime.now(UTC);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-name-updated")
                        .createdAt(eventCreationTime),
                payload);

        when(jsonObjectToObjectConverter.convert(payload, DefendantNameUpdated.class)).thenReturn(defendantNameUpdated);
        when(caseRepository.findBy(defendantNameUpdated.getCaseId())).thenReturn(caseDetail);
        when(caseDetail.getDefendant()).thenReturn(defendantDetail);
        when(defendantDetail.getPersonalDetails()).thenReturn(personalDetails);

        defendantPersonalDetailsChangesListener.defendantNameUpdated(envelope);

        verify(caseDetail).markDefendantNameUpdated(eventCreationTime);
        verify(caseRepository).save(caseDetail);
    }

    @Test
    public void shouldUpdateDefendantNameChangedTimestamp() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-name-updated"),
                payload);

        when(jsonObjectToObjectConverter.convert(payload, DefendantNameUpdated.class)).thenReturn(defendantNameUpdated);
        when(caseRepository.findBy(defendantNameUpdated.getCaseId())).thenReturn(caseDetail);

        ZonedDateTime updatedAt = ZonedDateTime.now(UTC);
        when(defendantNameUpdated.getUpdatedAt()).thenReturn(updatedAt);

        when(caseDetail.getDefendant()).thenReturn(defendantDetail);
        when(defendantDetail.getPersonalDetails()).thenReturn(personalDetails);

        defendantPersonalDetailsChangesListener.defendantNameUpdated(envelope);

        verify(caseDetail).markDefendantNameUpdated(updatedAt);
        verify(caseRepository).save(caseDetail);
    }

    @Test
    public void shouldUpdateDefendantDobChangedTimestamp() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-date-of-birth-updated"),
                payload);

        when(jsonObjectToObjectConverter.convert(payload, DefendantDateOfBirthUpdated.class)).thenReturn(defendantDateOfBirthUpdated);
        when(caseRepository.findBy(defendantDateOfBirthUpdated.getCaseId())).thenReturn(caseDetail);

        ZonedDateTime updatedAt = ZonedDateTime.now(UTC);
        when(defendantDateOfBirthUpdated.getUpdatedAt()).thenReturn(updatedAt);

        when(caseDetail.getDefendant()).thenReturn(defendantDetail);
        when(defendantDetail.getPersonalDetails()).thenReturn(personalDetails);

        defendantPersonalDetailsChangesListener.defendantDateOfBirthUpdated(envelope);

        verify(caseDetail).markDefendantDateOfBirthUpdated(updatedAt);
        verify(caseRepository).save(caseDetail);
    }

    @Test
    public void shouldUpdateDefendantAddressChangedTimestamp() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-address-updated"),
                payload);

        when(jsonObjectToObjectConverter.convert(payload, DefendantAddressUpdated.class)).thenReturn(defendantAddressUpdated);
        when(caseRepository.findBy(defendantAddressUpdated.getCaseId())).thenReturn(caseDetail);

        ZonedDateTime updatedAt = ZonedDateTime.now(UTC);
        when(defendantAddressUpdated.getUpdatedAt()).thenReturn(updatedAt);

        when(caseDetail.getDefendant()).thenReturn(defendantDetail);
        when(defendantDetail.getPersonalDetails()).thenReturn(personalDetails);

        defendantPersonalDetailsChangesListener.defendantAddressUpdated(envelope);

        verify(caseDetail).markDefendantAddressUpdated(updatedAt);
        verify(caseRepository).save(caseDetail);
    }

}
