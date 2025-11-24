package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import java.time.temporal.ChronoUnit;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantDetailUpdateRequestRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
public class DefendantPersonalDetailsUpdatedTest {

    @InjectMocks
    private DefendantPersonalDetailsChangesListener defendantPersonalDetailsChangesListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private DefendantDetailUpdateRequestRepository defendantDetailUpdateRequestRepository;

    @Mock
    private JsonObject payload;

    @Mock
    private DefendantNameUpdated defendantNameUpdated;

    @Mock
    private DefendantDetailUpdateRequested defendantDetailUpdateRequested;

    @Mock
    private DefendantNameUpdateRequested defendantNameUpdateRequested;

    @Mock
    private DefendantDateOfBirthUpdateRequested defendantDateOfBirthUpdateRequested;

    @Mock
    private DefendantAddressUpdateRequested defendantAddressUpdateRequested;

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

    @Mock
    private PersonalName personalName;

    @Mock
    private Address address;

    @Mock
    private DefendantDetailUpdateRequest defendantDetailUpdateRequest;

    @Captor
    private ArgumentCaptor<DefendantDetailUpdateRequest> captor;

    @Test
    public void shouldUseEventCreatedAtWhenUpdatedAtNotPresentInEvent() {
        ZonedDateTime eventCreationTime = ZonedDateTime.now(UTC);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-name-updated")
                        .createdAt(eventCreationTime),
                payload);

        when(jsonObjectToObjectConverter.convert(payload, DefendantNameUpdated.class)).thenReturn(defendantNameUpdated);
        when(caseRepository.findBy(defendantNameUpdated.getCaseId())).thenReturn(caseDetail);

        defendantPersonalDetailsChangesListener.defendantNameUpdated(envelope);

        verify(caseDetail).markDefendantNameUpdated(eventCreationTime.truncatedTo(ChronoUnit.MILLIS));
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

        defendantPersonalDetailsChangesListener.defendantAddressUpdated(envelope);

        verify(caseDetail).markDefendantAddressUpdated(updatedAt);
        verify(caseRepository).save(caseDetail);
    }

    @Test
    public void shouldSaveDefendantNameUpdateRequested() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-name-update-requested"),
                payload);
        UUID caseId = UUID.randomUUID();
        ZonedDateTime updatedAt = ZonedDateTime.now(UTC);
        DefendantDetailUpdateRequest detailUpdateRequest = new DefendantDetailUpdateRequest.Builder()
                .withCaseId(caseId)
                .withStatus(DefendantDetailUpdateRequest.Status.PENDING)
                .withFirstName("firstName")
                .withLastName("lastName")
                .withUpdatedAt(updatedAt)
                .build();
        when(jsonObjectToObjectConverter.convert(payload, DefendantNameUpdateRequested.class)).thenReturn(defendantNameUpdateRequested);
        when(defendantNameUpdateRequested.getCaseId()).thenReturn(caseId);
        when(defendantDetailUpdateRequestRepository.findBy(caseId)).thenReturn(null);
        when(defendantNameUpdateRequested.getUpdatedAt()).thenReturn(updatedAt);
        when(defendantNameUpdateRequested.getCaseId()).thenReturn(caseId);
        when(defendantNameUpdateRequested.getNewPersonalName()).thenReturn(personalName);
        when(personalName.getFirstName()).thenReturn("firstName");
        when(personalName.getLastName()).thenReturn("lastName");
        defendantPersonalDetailsChangesListener.defendantNameUpdateRequested(envelope);

        verify(defendantDetailUpdateRequestRepository, times(1)).save(captor.capture());

        final DefendantDetailUpdateRequest request = captor.getValue();
        assertThat(request.getFirstName(), equalTo(detailUpdateRequest.getFirstName()));
        assertThat(request.getLastName(), equalTo(detailUpdateRequest.getLastName()));
    }

    @Test
    public void shouldSaveDefendantDetailUpdateRequested() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-detail-update-requested"),
                payload);
        UUID caseId = UUID.randomUUID();

        when(jsonObjectToObjectConverter.convert(payload, DefendantDetailUpdateRequested.class)).thenReturn(defendantDetailUpdateRequested);
        when(defendantDetailUpdateRequested.getNameUpdated()).thenReturn(true);
        when(defendantDetailUpdateRequested.getAddressUpdated()).thenReturn(true);
        when(defendantDetailUpdateRequested.getDobUpdated()).thenReturn(true);

        defendantPersonalDetailsChangesListener.defendantDetailUpdateRequested(envelope);

        verify(defendantDetailUpdateRequestRepository, times(1)).save(captor.capture());

        final DefendantDetailUpdateRequest request = captor.getValue();
        assertTrue(request.isNameUpdated());
        assertTrue(request.isAddressUpdated());
        assertTrue(request.isDobUpdated());
    }

    @Test
    public void shouldSaveDefendantAddressUpdateRequested() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-address-update-requested"),
                payload);
        UUID caseId = UUID.randomUUID();
        ZonedDateTime updatedAt = ZonedDateTime.now(UTC);
        DefendantDetailUpdateRequest detailUpdateRequest = new DefendantDetailUpdateRequest.Builder()
                .withCaseId(caseId)
                .withStatus(DefendantDetailUpdateRequest.Status.PENDING)
                .withAddress1("address1")
                .withAddress2("address2")
                .withAddress3("address3")
                .withAddress4("address4")
                .withAddress5("address5")
                .withPostcode("postcode")
                .withUpdatedAt(updatedAt)
                .build();
        when(jsonObjectToObjectConverter.convert(payload, DefendantAddressUpdateRequested.class)).thenReturn(defendantAddressUpdateRequested);
        when(defendantAddressUpdateRequested.getCaseId()).thenReturn(caseId);
        when(defendantDetailUpdateRequestRepository.findBy(caseId)).thenReturn(null);
        when(defendantAddressUpdateRequested.getUpdatedAt()).thenReturn(updatedAt);
        when(defendantAddressUpdateRequested.getCaseId()).thenReturn(caseId);
        when(defendantAddressUpdateRequested.getNewAddress()).thenReturn(address);
        when(address.getAddress1()).thenReturn("address1");
        when(address.getAddress2()).thenReturn("address2");
        when(address.getAddress3()).thenReturn("address3");
        when(address.getAddress4()).thenReturn("address4");
        when(address.getAddress5()).thenReturn("address5");
        when(address.getPostcode()).thenReturn("postcode");
        defendantPersonalDetailsChangesListener.defendantAddressUpdateRequested(envelope);

        verify(defendantDetailUpdateRequestRepository, times(1)).save(captor.capture());

        final DefendantDetailUpdateRequest request = captor.getValue();
        assertThat(request.getAddress1(), equalTo(detailUpdateRequest.getAddress1()));
        assertThat(request.getAddress2(), equalTo(detailUpdateRequest.getAddress2()));
        assertThat(request.getAddress3(), equalTo(detailUpdateRequest.getAddress3()));
        assertThat(request.getAddress4(), equalTo(detailUpdateRequest.getAddress4()));
        assertThat(request.getAddress5(), equalTo(detailUpdateRequest.getAddress5()));
        assertThat(request.getPostcode(), equalTo(detailUpdateRequest.getPostcode()));
    }

    @Test
    public void shouldSaveDefendantAddressUpdateRequested_fromApplication() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-address-update-requested"),
                payload);
        UUID caseId = UUID.randomUUID();
        ZonedDateTime updatedAt = ZonedDateTime.now(UTC);
        Address address = new Address("address1", "address2", "address3", "address4", "address5", "postcode");
        DefendantAddressUpdateRequested defendantAddressUpdateRequested = new DefendantAddressUpdateRequested(caseId,address,updatedAt,true);
        when(jsonObjectToObjectConverter.convert(payload, DefendantAddressUpdateRequested.class)).thenReturn(defendantAddressUpdateRequested);
        defendantPersonalDetailsChangesListener.defendantAddressUpdateRequested(envelope);
        verify(defendantDetailUpdateRequestRepository, times(1)).save(captor.capture());

        final DefendantDetailUpdateRequest request = captor.getValue();
        assertThat(request.getAddress1(), equalTo("address1"));
        assertThat(request.getAddress2(), equalTo("address2"));
        assertThat(request.getAddress3(), equalTo("address3"));
        assertThat(request.getAddress4(), equalTo("address4"));
        assertThat(request.getAddress5(), equalTo("address5"));
        assertThat(request.getPostcode(), equalTo("postcode"));
        assertThat(request.getStatus(), equalTo(DefendantDetailUpdateRequest.Status.UPDATED));
    }

    @Test
    public void shouldSaveDefendantDOBUpdateRequested() {
        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.defendant-date-of-birth-update-requested"),
                payload);
        UUID caseId = UUID.randomUUID();
        ZonedDateTime updatedAt = ZonedDateTime.now(UTC);
        DefendantDetailUpdateRequest detailUpdateRequest = new DefendantDetailUpdateRequest.Builder()
                .withCaseId(caseId)
                .withStatus(DefendantDetailUpdateRequest.Status.PENDING)
                .withDateOfBirth(LocalDate.now())
                .withUpdatedAt(updatedAt)
                .build();
        when(jsonObjectToObjectConverter.convert(payload, DefendantDateOfBirthUpdateRequested.class)).thenReturn(defendantDateOfBirthUpdateRequested);
        when(defendantDateOfBirthUpdateRequested.getCaseId()).thenReturn(caseId);
        when(defendantDetailUpdateRequestRepository.findBy(caseId)).thenReturn(null);
        when(defendantDateOfBirthUpdateRequested.getUpdatedAt()).thenReturn(updatedAt);
        when(defendantDateOfBirthUpdateRequested.getCaseId()).thenReturn(caseId);
        when(defendantDateOfBirthUpdateRequested.getNewDateOfBirth()).thenReturn(LocalDate.now());
        defendantPersonalDetailsChangesListener.defendantDateOfBirthUpdateRequested(envelope);

        verify(defendantDetailUpdateRequestRepository, times(1)).save(captor.capture());

        final DefendantDetailUpdateRequest request = captor.getValue();
        assertThat(request.getDateOfBirth(), equalTo(detailUpdateRequest.getDateOfBirth()));
    }
}
