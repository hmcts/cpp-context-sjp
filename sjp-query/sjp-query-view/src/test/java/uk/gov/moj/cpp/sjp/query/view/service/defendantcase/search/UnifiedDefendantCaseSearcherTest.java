package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)

public class UnifiedDefendantCaseSearcherTest {

    @Mock
    private Enveloper enveloper;

    @Mock
    private Envelope<?> envelope;

    @Mock
    private Metadata metadata;

    @InjectMocks
    private UnifiedDefendantCaseSearcher unifiedDefendantCaseSearcher;

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Class<DefendantCaseQueryResult>> anycapture;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;




    @Test
    public void shouldDefendantQueryDetailsSubmittedToUnifiedSearch() {
        final DefendantDetail defendantDetail = new DefendantDetail();
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName("FirstName");
        personalDetails.setLastName("LastName");
        personalDetails.setDateOfBirth(LocalDate.now());
        Address address = new Address();
        address.setAddress1("Address1");
        address.setPostcode("IG5 6NZ");
        defendantDetail.setAddress(address);
        defendantDetail.setPersonalDetails(personalDetails);
        Metadata metadata1 = Envelope.metadataBuilder().createdAt(ZonedDateTime.now()).withId(UUID.randomUUID()).withName("test").withClientCorrelationId(UUID.randomUUID().toString()).build();
        when(envelope.metadata()).thenReturn(metadata1);
        when(requester.requestAsAdmin(envelopeArgumentCaptor.capture(), anycapture.capture())).thenReturn(null);
        unifiedDefendantCaseSearcher.searchDefendantCases(envelope, defendantDetail);
        verify(requester).requestAsAdmin(envelopeArgumentCaptor.getValue(), anycapture.getValue());
    }



    @Test
    public void shouldDefendantQueryDetailsBeNotSubmittedToUnifiedSearchWhenDOBMissing() {
        final DefendantDetail defendantDetail = new DefendantDetail();
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName("FirstName");
        personalDetails.setLastName("LastName");

        Address address = new Address();
        address.setAddress1("Address1");
        address.setPostcode("IG5 6NZ");
        defendantDetail.setAddress(address);
        defendantDetail.setPersonalDetails(personalDetails);
        Metadata metadata1 = Envelope.metadataBuilder().createdAt(ZonedDateTime.now()).withId(UUID.randomUUID()).withName("test").withClientCorrelationId(UUID.randomUUID().toString()).build();
        when(envelope.metadata()).thenReturn(metadata1);
        when(requester.requestAsAdmin(envelopeArgumentCaptor.capture(), anycapture.capture())).thenReturn(null);
        unifiedDefendantCaseSearcher.searchDefendantCases(envelope, defendantDetail);
        verifyNoMoreInteractions(requester);
    }


    @Test
    public void shouldDefendantQueryDetailsBeNotSubmittedToUnifiedSearchWhenPostCodeMissing() {
        final DefendantDetail defendantDetail = new DefendantDetail();
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName("FirstName");
        personalDetails.setLastName("LastName");
        personalDetails.setDateOfBirth(LocalDate.now());
        Address address = new Address();
        address.setAddress1("Address1");
        defendantDetail.setAddress(address);
        defendantDetail.setPersonalDetails(personalDetails);
        Metadata metadata1 = Envelope.metadataBuilder().createdAt(ZonedDateTime.now()).withId(UUID.randomUUID()).withName("test").withClientCorrelationId(UUID.randomUUID().toString()).build();
        when(envelope.metadata()).thenReturn(metadata1);
        when(requester.requestAsAdmin(envelopeArgumentCaptor.capture(), anycapture.capture())).thenReturn(null);
        unifiedDefendantCaseSearcher.searchDefendantCases(envelope, defendantDetail);
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void shouldDefendantQueryDetailsBeNotSubmittedToUnifiedSearchWhenAddressLineMissing() {
        final DefendantDetail defendantDetail = new DefendantDetail();
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName("FirstName");
        personalDetails.setLastName("LastName");
        personalDetails.setDateOfBirth(LocalDate.now());
        Address address = new Address();
        address.setPostcode("IG5 6NZ");
        defendantDetail.setAddress(address);
        defendantDetail.setPersonalDetails(personalDetails);
        Metadata metadata1 = Envelope.metadataBuilder().createdAt(ZonedDateTime.now()).withId(UUID.randomUUID()).withName("test").withClientCorrelationId(UUID.randomUUID().toString()).build();
        when(envelope.metadata()).thenReturn(metadata1);
        when(requester.requestAsAdmin(envelopeArgumentCaptor.capture(), anycapture.capture())).thenReturn(null);
        unifiedDefendantCaseSearcher.searchDefendantCases(envelope, defendantDetail);
        verifyNoMoreInteractions(requester);
    }







}
