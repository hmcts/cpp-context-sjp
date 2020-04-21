package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.Language;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantToDefendantDetailsTest {

    @Mock
    private PersonToPersonalDetailsEntity<Defendant> personToPersonalDetailsEntity;

    @Mock
    private OffenceToOffenceDetail offenceToOffenceDetailConverter;

    @Spy
    private SpeaksWelshConverter speaksWelshConverter = new SpeaksWelshConverter();

    @InjectMocks
    private DefendantToDefendantDetails converterUnderTest = new DefendantToDefendantDetails();

    @Test
    public void shouldConvertDefendantToDefendantDetails() {
        // GIVEN
        final Defendant inputDefendant = new Defendant(UUID.randomUUID(), "title", "firstName", "lastName",
                LocalDate.of(1980, 1,1), Gender.MALE, RandomStringUtils.random(10),
                RandomStringUtils.random(16), RandomStringUtils.random(50),
                new uk.gov.moj.cpp.sjp.domain.Address("l1", "l2", "l3", "l4", "l5", "p"),
                new uk.gov.moj.cpp.sjp.domain.ContactDetails("home", "mobile", "business" , "email1@abc.com", "email2@abc.com"),
                3, asList(mock(Offence.class), mock(Offence.class)), Language.W, "languageNeeds", "region");

        final PersonalDetails mockedPersonalDetails = mock(PersonalDetails.class);
        when(personToPersonalDetailsEntity.convert(inputDefendant)).thenReturn(mockedPersonalDetails);

        final List<OffenceDetail> mockedOffenceDetails = inputDefendant.getOffences().stream()
                .map(offence -> {
                    OffenceDetail mockedOffenceDetail = mock(OffenceDetail.class);
                    when(offenceToOffenceDetailConverter.convert(offence)).thenReturn(mockedOffenceDetail);

                    return mockedOffenceDetail;
                })
                .collect(toList());

        // WHEN
        final DefendantDetail outputDefendant = converterUnderTest.convert(inputDefendant);

        // THEN
        final DefendantDetail expectedDefendant = new DefendantDetail(
                inputDefendant.getId(),
                mockedPersonalDetails,
                mockedOffenceDetails,
                inputDefendant.getNumPreviousConvictions(),
                true);

        assertTrue(reflectionEquals(outputDefendant, expectedDefendant));
    }

}