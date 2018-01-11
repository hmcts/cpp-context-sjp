package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated.DefendantDetailsUpdatedBuilder.defendantDetailsUpdated;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactNumber;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantUpdatedListenerTest {

    @InjectMocks
    private DefendantUpdatedListener defendantUpdatedListener;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseSearchResultRepository caseSearchResultRepository;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private CaseDetail caseDetail;

    @Mock
    private JsonEnvelope jsonEnvelope;

    private DefendantDetailsUpdated defendantDetailsUpdated = defendantDetailsUpdated()
            .withCaseId(UUID.randomUUID())
            .withContactNumber(new ContactNumber("123", "456"))
            .withFirstName("Mark")
            .withLastName("Smith")
            .withDateOfBirth(LocalDate.of(1960, 1, 1))
            .withAddress(new Address("address1", "address2", "address3", "address4", "postcode"))
            .build();

    @Captor
    private ArgumentCaptor<CaseDetail> actualPersonalDetailsCaptor;

    @Captor
    private ArgumentCaptor<CaseSearchResult> actualSearchResultsCaptor;

    @Before
    public void setupMocks() throws JsonProcessingException {
        when(caseRepository.findBy(defendantDetailsUpdated.getCaseId())).thenReturn(caseDetail);
        when(caseDetail.getDefendant().getPersonalDetails()).thenReturn(new PersonalDetails());
        when(caseSearchResultRepository.findByCaseId(defendantDetailsUpdated.getCaseId())).thenReturn(asList(new CaseSearchResult(), new CaseSearchResult()));

        mockConverter(defendantDetailsUpdated);
    }

    @Test
    public void shouldListenerUpdateDefendant() {
        PersonalDetails expectedPersonalDetails = buildPersonalDetails();
        List<CaseSearchResult> expectedSearchResults = new LinkedList<>(asList(buildCaseSearchResult(), buildCaseSearchResult()));

        // WHEN
        defendantUpdatedListener.defendantDetailsUpdated(jsonEnvelope);

        verify(caseRepository).save(actualPersonalDetailsCaptor.capture());
        verify(caseSearchResultRepository, times(expectedSearchResults.size())).save(actualSearchResultsCaptor.capture());

        DefendantDetail defendant = actualPersonalDetailsCaptor.getValue().getDefendant();
        assertTrue(reflectionEquals(expectedPersonalDetails, defendant.getPersonalDetails(),
                "contactDetails", "address"));
        assertTrue(reflectionEquals(expectedPersonalDetails.getContactDetails(), defendant.getPersonalDetails().getContactDetails()));
        assertTrue(reflectionEquals(expectedPersonalDetails.getAddress(), defendant.getPersonalDetails().getAddress()));

        assertThat(actualSearchResultsCaptor.getAllValues(), hasSize(expectedSearchResults.size()));
        for (int i = 0; i < actualSearchResultsCaptor.getAllValues().size(); i++) {
            assertTrue(reflectionEquals(expectedSearchResults.get(i), actualSearchResultsCaptor.getAllValues().get(i)));
        }
    }

    private void mockConverter(DefendantDetailsUpdated defendantDetailsUpdated) throws JsonProcessingException {
        String serializedDefendantDetailsUpdated = new ObjectMapper().writeValueAsString(defendantDetailsUpdated);
        JsonObject jsonObject = Json.createReader(new StringReader(serializedDefendantDetailsUpdated)).readObject();

        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonObjectToObjectConverter.convert(jsonObject, DefendantDetailsUpdated.class)).thenReturn(defendantDetailsUpdated);
    }

    private PersonalDetails buildPersonalDetails() {
        return new PersonalDetails(defendantDetailsUpdated.getTitle(), defendantDetailsUpdated.getFirstName(),
                defendantDetailsUpdated.getLastName(), defendantDetailsUpdated.getDateOfBirth(), defendantDetailsUpdated.getGender(),
                defendantDetailsUpdated.getNationalInsuranceNumber(),
                new uk.gov.moj.cpp.sjp.persistence.entity.Address(
                        defendantDetailsUpdated.getAddress().getAddress1(),
                        defendantDetailsUpdated.getAddress().getAddress2(),
                        defendantDetailsUpdated.getAddress().getAddress3(),
                        defendantDetailsUpdated.getAddress().getAddress4(),
                        defendantDetailsUpdated.getAddress().getPostcode()
                ),
                new ContactDetails(
                        defendantDetailsUpdated.getEmail(),
                        defendantDetailsUpdated.getContactNumber().getHome(),
                        defendantDetailsUpdated.getContactNumber().getMobile()));
    }

    private CaseSearchResult buildCaseSearchResult() {
        CaseSearchResult caseSearchResult = new CaseSearchResult();
        caseSearchResult.setFirstName(defendantDetailsUpdated.getFirstName());
        caseSearchResult.setLastName(defendantDetailsUpdated.getLastName());
        caseSearchResult.setDateOfBirth(defendantDetailsUpdated.getDateOfBirth());
        caseSearchResult.setPostCode(defendantDetailsUpdated.getAddress().getPostcode());

        return caseSearchResult;
    }

}