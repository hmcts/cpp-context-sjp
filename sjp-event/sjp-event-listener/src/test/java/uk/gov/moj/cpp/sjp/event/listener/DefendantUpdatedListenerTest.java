package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.AddressToAddressEntity;
import uk.gov.moj.cpp.sjp.event.listener.converter.ContactDetailsToContactDetailsEntity;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantUpdatedListenerTest {

    @InjectMocks
    private DefendantUpdatedListener defendantUpdatedListener;

    @Spy
    @InjectMocks
    private CaseSearchResultService caseSearchResultService = new CaseSearchResultService();

    @Spy
    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseSearchResultRepository caseSearchResultRepository;

    @Mock
    private OnlinePleaRepository.PersonDetailsOnlinePleaRepository onlinePleaRepository;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AddressToAddressEntity addressToAddressEntity;

    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaCaptor;

    private CaseDetail caseDetail = new CaseDetail(UUID.randomUUID());

    private final Clock clock = new UtcClock();
    private final ZonedDateTime now = clock.now();

    private final String previousTitle = "previously set Title";
    private final String previousGender = "previously set gender";
    private final String previousNiNumber = "previously set NI-number";

    private DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder defendantDetailsUpdatedBuilder = defendantDetailsUpdated()
            .withCaseId(caseDetail.getId())
            .withContactDetails(new ContactDetails("123", "456", "test@test.com"))
            .withTitle("Mr")
            .withFirstName("Mark")
            .withLastName("Smith")
            .withGender("Male")
            .withUpdatedDate(clock.now())
            .withDateOfBirth(LocalDate.of(1960, 1, 1))
            .withAddress(new Address("address1", "address2", "address3", "address4", "postcode"));

    @Captor
    private ArgumentCaptor<CaseDetail> actualPersonalDetailsCaptor;

    @Captor
    private ArgumentCaptor<CaseSearchResult> actualSearchResultsCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private void setupMocks(UUID caseId) {
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);

        caseDetail.getDefendant().setPersonalDetails(
                new PersonalDetails(
                        previousTitle, "Joe", "Blogs", LocalDate.of(1965, 8, 6),
                        previousGender, previousNiNumber,
                        new uk.gov.moj.cpp.sjp.persistence.entity.Address(
                                "address1", "address2", "address3", "address4", "postcode"
                        ),
                        new uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails("test@test.com", "0207 886432", "07563 489883")
                )
        );

        when(caseRepository.findCaseDefendant(caseId)).thenReturn(caseDetail.getDefendant());
        when(caseSearchResultRepository.findByCaseId(caseId)).thenReturn(Lists.newArrayList(buildCaseSearchResult(caseDetail)));
    }

    private DefendantDetailsUpdated commonSetup(final boolean updateByOnlinePlea, final boolean nationalInsuranceNumberSuppliedInRequest) {
        //WHEN
        defendantDetailsUpdatedBuilder = defendantDetailsUpdatedBuilder.withUpdateByOnlinePlea(updateByOnlinePlea);
        if (updateByOnlinePlea) {
            defendantDetailsUpdatedBuilder.withUpdatedDate(now);
        }
        if (nationalInsuranceNumberSuppliedInRequest) {
            defendantDetailsUpdatedBuilder = defendantDetailsUpdatedBuilder.withNationalInsuranceNumber("NH42 1568G");
        }
        final DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdatedBuilder.build();
        setupMocks(defendantDetailsUpdated.getCaseId());

        return defendantDetailsUpdated;
    }

    // FIXME commonAssertions to be refactored
    private void commonAssertions(DefendantDetailsUpdated defendantDetailsUpdated, boolean updateByOnlinePlea, boolean nationalInsuranceNumberSupplied) {
        final PersonalDetails expectedPersonalDetails = buildPersonalDetails(defendantDetailsUpdated, updateByOnlinePlea, nationalInsuranceNumberSupplied);
        final List<CaseSearchResult> expectedSearchResults = singletonList(buildCaseSearchResult(defendantDetailsUpdated));

        final int expectedSaveInvocations = expectedSearchResults.size() + 1; // Save invocations + 1. Update each old case search entry and create a new one (+1)
        verify(caseRepository).save(actualPersonalDetailsCaptor.capture());

        verify(caseSearchResultRepository, times(expectedSaveInvocations)).save(actualSearchResultsCaptor.capture());

        if (updateByOnlinePlea) {
            verify(onlinePleaRepository).saveOnlinePlea(onlinePleaCaptor.capture());
        } else {
            verify(onlinePleaRepository, never()).saveOnlinePlea(onlinePleaCaptor.capture());
        }

        final DefendantDetail defendant = actualPersonalDetailsCaptor.getValue().getDefendant();
        assertTrue(reflectionEquals(expectedPersonalDetails, defendant.getPersonalDetails(),
                "contactDetails", "address"));
        assertTrue(reflectionEquals(expectedPersonalDetails.getContactDetails(), defendant.getPersonalDetails().getContactDetails()));
        assertTrue(reflectionEquals(expectedPersonalDetails.getAddress(), defendant.getPersonalDetails().getAddress()));

        assertThat(actualSearchResultsCaptor.getAllValues(), hasSize(expectedSaveInvocations));
        for (int i = 0; i < expectedSearchResults.size(); i++) {
            CaseSearchResult actualResult = actualSearchResultsCaptor.getAllValues().get(i);
            CaseSearchResult expectedResult = expectedSearchResults.get(i);
            assertThat(actualResult.getCurrentFirstName(), equalTo(expectedResult.getCurrentFirstName()));
            assertThat(actualResult.getCurrentLastName(), equalTo(expectedResult.getCurrentLastName()));
            assertThat(actualResult.getDateOfBirth(), equalTo(expectedResult.getDateOfBirth()));
            assertTrue(actualResult.isDeprecated());
        }
        CaseSearchResult actualRecordAdded = actualSearchResultsCaptor.getAllValues().get(expectedSearchResults.size());
        assertThat(actualRecordAdded.getFirstName(), equalTo(defendantDetailsUpdated.getFirstName()));
        assertThat(actualRecordAdded.getCurrentFirstName(), equalTo(defendantDetailsUpdated.getFirstName()));
        assertThat(actualRecordAdded.getLastName(), equalTo(defendantDetailsUpdated.getLastName()));
        assertThat(actualRecordAdded.getCurrentLastName(), equalTo(defendantDetailsUpdated.getLastName()));
        assertThat(actualRecordAdded.getDateOfBirth(), equalTo(defendantDetailsUpdated.getDateOfBirth()));
        assertThat(actualRecordAdded.getCaseId(), equalTo(defendantDetailsUpdated.getCaseId()));
        assertFalse(actualRecordAdded.isDeprecated());

        if (updateByOnlinePlea) {
            assertThat(onlinePleaCaptor.getValue().getCaseId(), equalTo(defendantDetailsUpdated.getCaseId()));
            assertThat(onlinePleaCaptor.getValue().getDefendantId(), equalTo(defendantDetailsUpdated.getDefendantId()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getFirstName(), equalTo(defendantDetailsUpdated.getFirstName()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getLastName(), equalTo(defendantDetailsUpdated.getLastName()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getDateOfBirth(), equalTo(defendantDetailsUpdated.getDateOfBirth()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getEmail(), equalTo(defendantDetailsUpdated.getContactDetails().getEmail()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getNationalInsuranceNumber(), equalTo(defendantDetailsUpdated.getNationalInsuranceNumber()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getMobile(), equalTo(defendantDetailsUpdated.getContactDetails().getMobile()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getHomeTelephone(), equalTo(defendantDetailsUpdated.getContactDetails().getHome()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getAddress().getAddress1(), equalTo(defendantDetailsUpdated.getAddress().getAddress1()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getAddress().getAddress2(), equalTo(defendantDetailsUpdated.getAddress().getAddress2()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getAddress().getAddress3(), equalTo(defendantDetailsUpdated.getAddress().getAddress3()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getAddress().getAddress4(), equalTo(defendantDetailsUpdated.getAddress().getAddress4()));
            assertThat(onlinePleaCaptor.getValue().getPersonalDetails().getAddress().getPostcode(), equalTo(defendantDetailsUpdated.getAddress().getPostcode()));
            assertThat(onlinePleaCaptor.getValue().getSubmittedOn().toEpochSecond(), equalTo(defendantDetailsUpdated.getUpdatedDate().toEpochSecond()));
        }
    }

    @Test
    public void shouldUpdateDefendantNationalInsuranceNumberUpdated() throws JsonProcessingException {
        DefendantsNationalInsuranceNumberUpdated event = new DefendantsNationalInsuranceNumberUpdated(
                caseDetail.getId(),
                caseDetail.getDefendant().getId(),
                caseDetail.getDefendant().getPersonalDetails().getNationalInsuranceNumber()
        );
        setupMocks(event.getCaseId());

        defendantUpdatedListener.defendantNationalInsuranceNumberUpdated(command(event));
    }

    @Test
    public void shouldListenerUpdateDefendantNotUpdatedFromOnlinePlea() throws JsonProcessingException {
        // GIVEN
        final boolean updateByOnlinePlea = false;
        final boolean nationalInsuranceNumberSuppliedInRequest = true;
        final DefendantDetailsUpdated defendantDetailsUpdated = commonSetup(updateByOnlinePlea, nationalInsuranceNumberSuppliedInRequest);

        // WHEN
        defendantUpdatedListener.defendantDetailsUpdated(command(defendantDetailsUpdated));

        // THEN
        commonAssertions(defendantDetailsUpdated, updateByOnlinePlea, nationalInsuranceNumberSuppliedInRequest);
    }

    @Test
    public void shouldListenerUpdateDefendantUpdatedFromOnlinePlea() throws JsonProcessingException {
        // GIVEN
        final boolean updateByOnlinePlea = true;
        final boolean nationalInsuranceNumberSuppliedInRequest = true;
        final DefendantDetailsUpdated defendantDetailsUpdated = commonSetup(true, true);

        // WHEN
        defendantUpdatedListener.defendantDetailsUpdated(command(defendantDetailsUpdated));

        // THEN
        commonAssertions(defendantDetailsUpdated, updateByOnlinePlea, nationalInsuranceNumberSuppliedInRequest);
    }

    @Test
    public void shouldListenerUpdateDefendantUpdatedFromOnlinePleaWithoutNationalInsuranceNumber() throws JsonProcessingException {
        // GIVEN
        final boolean updateByOnlinePlea = true;
        final boolean nationalInsuranceNumberSuppliedInRequest = false;
        final DefendantDetailsUpdated defendantDetailsUpdated = commonSetup(true, false);

        // WHEN
        defendantUpdatedListener.defendantDetailsUpdated(command(defendantDetailsUpdated));

        // THEN
        commonAssertions(defendantDetailsUpdated, updateByOnlinePlea, nationalInsuranceNumberSuppliedInRequest);
    }

    private JsonEnvelope command(DefendantsNationalInsuranceNumberUpdated defendantsNationalInsuranceNumberUpdated) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.defendant-national-insurance-number-updated"),
                objectToJsonObjectConverter.convert(defendantsNationalInsuranceNumberUpdated)
        );
    }

    private JsonEnvelope command(DefendantDetailsUpdated defendantDetailsUpdated) {
        return JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.defendant-details-updated"),
                objectToJsonObjectConverter.convert(defendantDetailsUpdated)
        );
    }

    private PersonalDetails buildPersonalDetails(final DefendantDetailsUpdated defendantDetailsUpdated, final boolean updateByOnlinePlea, final boolean onlinePleaNiNumberSupplied) {
        String title = defendantDetailsUpdated.getTitle();
        String gender = defendantDetailsUpdated.getGender();
        String nationalInsuranceNumber = defendantDetailsUpdated.getNationalInsuranceNumber();
        if (updateByOnlinePlea) {
            title = previousTitle;
            gender = previousGender;
        }
        if (updateByOnlinePlea && !onlinePleaNiNumberSupplied) {
            nationalInsuranceNumber = previousNiNumber;
        }
        return new PersonalDetails(title, defendantDetailsUpdated.getFirstName(),
                defendantDetailsUpdated.getLastName(), defendantDetailsUpdated.getDateOfBirth(), gender,
                nationalInsuranceNumber,
                new uk.gov.moj.cpp.sjp.persistence.entity.Address(
                        defendantDetailsUpdated.getAddress().getAddress1(),
                        defendantDetailsUpdated.getAddress().getAddress2(),
                        defendantDetailsUpdated.getAddress().getAddress3(),
                        defendantDetailsUpdated.getAddress().getAddress4(),
                        defendantDetailsUpdated.getAddress().getPostcode()
                ),
                new uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails(
                        defendantDetailsUpdated.getContactDetails().getEmail(),
                        defendantDetailsUpdated.getContactDetails().getHome(),
                        defendantDetailsUpdated.getContactDetails().getMobile()));
    }

    private CaseSearchResult buildCaseSearchResult(final CaseDetail caseDetail) {
        return new CaseSearchResult(
                caseDetail.getId(),
                caseDetail.getDefendant().getPersonalDetails().getFirstName(),
                caseDetail.getDefendant().getPersonalDetails().getLastName(),
                caseDetail.getDefendant().getPersonalDetails().getDateOfBirth(),
                clock.now()
        );
    }

    private CaseSearchResult buildCaseSearchResult(final DefendantDetailsUpdated defendantDetailsUpdated) {
        return new CaseSearchResult(
                defendantDetailsUpdated.getCaseId(),
                defendantDetailsUpdated.getFirstName(),
                defendantDetailsUpdated.getLastName(),
                defendantDetailsUpdated.getDateOfBirth(),
                clock.now());
    }
}