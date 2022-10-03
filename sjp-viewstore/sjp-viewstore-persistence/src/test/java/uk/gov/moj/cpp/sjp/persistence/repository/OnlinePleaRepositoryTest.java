package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityFinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaLegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaPersonalDetails;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public class OnlinePleaRepositoryTest extends BaseTransactionalTest {

    public static final String HOME = "123131231";
    public static final String MOBILE = "12313131";
    public static final String EMAIL = "test@test.com";
    @Inject
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private Clock clock;

    private UUID caseId;

    private UUID caseId2;

    @Before
    public void set() {
        caseId = UUID.randomUUID();
        final CaseDetail caseDetail = getCaseWithDefendant(caseId);
        caseRepository.save(caseDetail);

        final OnlinePlea insertedOnlinePlea = buildOnlinePlea(caseDetail, clock.now());

        caseId2 = UUID.randomUUID();
        final CaseDetail caseDetail1 = getCaseWithDefendant(caseId2);
        caseRepository.save(caseDetail1);

        final OnlinePlea insertedOnlinePleaWithCompany = buildOnlinePleaWithLegalEntity(caseDetail1, clock.now(), HOME, MOBILE, EMAIL);
        onlinePleaRepository.save(insertedOnlinePlea);
        onlinePleaRepository.save(insertedOnlinePleaWithCompany);
    }

    @Test
    public void shouldFindOnlinePleaGivenCaseIdAndDefendantId() {

        final OnlinePlea onlinePlea = onlinePleaRepository.findBy(caseId);
        final UUID defendantId = onlinePlea.getDefendantId();
        final OnlinePlea onlinePleaByDefendantIdAndCaseId = onlinePleaRepository.findOnlinePleaByDefendantIdAndCaseId(caseId, defendantId);

        assertThat("The online plea object obtained by case and defendant Id should be equal to original onlinepea object created with that case and defendant Id",
                reflectionEquals(onlinePleaByDefendantIdAndCaseId, onlinePlea));
        assertThat(onlinePlea.getOutgoings().getAccommodationAmount().doubleValue(), closeTo(10.0, 0.0));
    }

    @Test
    public void shouldDeleteOutgoingDataOnSettingItToNullInOnlinePleaData() {
        final OnlinePlea onlinePlea = onlinePleaRepository.findBy(caseId);
        assertThat("Outgoing financial means data should present", onlinePlea.getOutgoings() != null);
        onlinePlea.setOutgoings(null);
        onlinePleaRepository.save(onlinePlea);
        final OnlinePlea onlinePleaAfterModification = onlinePleaRepository.findBy(caseId);
        assertThat("Outgoing financial means data should be null after deleting it", onlinePleaAfterModification.getOutgoings() == null);
    }

    @Test
    public void shouldRetrieveLegalEntityDetails() {
        final OnlinePlea onlinePlea =  onlinePleaRepository.findBy(caseId2);
        assertNotNull(onlinePlea.getLegalEntityDetails());
        assertEquals("companyLegal", onlinePlea.getLegalEntityDetails().getLegalEntityName());
        assertEquals("Director", onlinePlea.getLegalEntityDetails().getPositionOfRepresentative());
        assertEquals( BigDecimal.valueOf(1000), onlinePlea.getLegalEntityDetails().getLegalEntityFinancialMeans().getGrossTurnover());
        assertEquals(BigDecimal.valueOf(100), onlinePlea.getLegalEntityDetails().getLegalEntityFinancialMeans().getNetTurnover());
        assertEquals(100, onlinePlea.getLegalEntityDetails().getLegalEntityFinancialMeans().getNumberOfEmployees().intValue());
        assertEquals(true, onlinePlea.getLegalEntityDetails().getLegalEntityFinancialMeans().getTradingMoreThan12Months());
        assertEquals(HOME, onlinePlea.getLegalEntityDetails().getHomeTelephone());
        assertEquals(MOBILE, onlinePlea.getLegalEntityDetails().getMobile());
        assertEquals(EMAIL, onlinePlea.getLegalEntityDetails().getEmail());
    }

    @Test
    public void shouldFindOnlinePleaWithoutFinances() {
        // WHEN
        final OnlinePlea actualFullOnlinePlea = onlinePleaRepository.findBy(caseId);
        final OnlinePlea actualOnlinePleaWithoutFinances = onlinePleaRepository.findOnlinePleaWithoutFinances(caseId);

        // ASSUME: finances present on the stored object
        assertThat(actualFullOnlinePlea.getPleaDetails(), notNullValue());
        assertThat(actualFullOnlinePlea.getOutgoings(), notNullValue());
        assertThat(actualFullOnlinePlea.getEmployment(), notNullValue());
        assertThat(actualFullOnlinePlea.getEmployer(), notNullValue());

        // THEN
        assertThat(actualOnlinePleaWithoutFinances.getPleaDetails(), notNullValue());
        assertThat(actualOnlinePleaWithoutFinances.getOutgoings(), nullValue());
        assertThat(actualOnlinePleaWithoutFinances.getEmployment(), nullValue());
        assertThat(actualOnlinePleaWithoutFinances.getEmployer(), nullValue());

        final List<String> excludedFields = asList("pleaDetails", "personalDetails", "outgoings", "employment", "employer");
        assertTrue(reflectionEquals(actualOnlinePleaWithoutFinances, actualFullOnlinePlea, excludedFields));
        assertTrue(reflectionEquals(actualOnlinePleaWithoutFinances.getPleaDetails(), actualFullOnlinePlea.getPleaDetails()));
        assertTrue(reflectionEquals(actualOnlinePleaWithoutFinances.getPersonalDetails(), actualFullOnlinePlea.getPersonalDetails()));

        // obfuscated fields
        assertThat(actualOnlinePleaWithoutFinances.getOutgoings(), nullValue());
        assertThat(actualOnlinePleaWithoutFinances.getEmployment(), nullValue());
        assertThat(actualOnlinePleaWithoutFinances.getEmployer(), nullValue());
    }

    private static CaseDetail getCaseWithDefendant(final UUID caseId) {
        final CaseDetail caseDetail = new CaseDetail(caseId);
        caseDetail.setOnlinePleaReceived(true);

        return caseDetail;
    }

    private static OnlinePlea buildOnlinePlea(final CaseDetail caseDetail, final ZonedDateTime onlinePleaSubmittedOn) {
        final OnlinePleaPersonalDetails personalDetails = new OnlinePleaPersonalDetails();
        personalDetails.setFirstName("first_name");
        personalDetails.setLastName("last_name");
        personalDetails.setAddress(new Address("address1", "address2", "address3", "address4", "address5", "postcode"));
        personalDetails.setEmail("email@email.email");
        personalDetails.setNationalInsuranceNumber("national_number");
        personalDetails.setHomeTelephone("123456789");
        personalDetails.setMobile("987654321");
        personalDetails.setDriverNumber("MORGA753116SM9IJ");
        personalDetails.setDriverLicenceDetails("driveer_licence_details");

        final OnlinePlea.PleaDetails pleaDetails = new OnlinePlea.PleaDetails();
        pleaDetails.setInterpreterLanguage("interpreter");
        pleaDetails.setUnavailability("unavailability");
        pleaDetails.setWitnessDetails("witnessDetails");
        pleaDetails.setWitnessDispute("witnessDispute");
        pleaDetails.setSpeakWelsh(true);

        final OnlinePlea onlinePlea = new OnlinePlea(caseDetail.getId(), pleaDetails, caseDetail.getDefendant().getId(), personalDetails, onlinePleaSubmittedOn, null);

        final OnlinePlea.Employer employer = new OnlinePlea.Employer();
        employer.setName("employer_name");
        onlinePlea.setEmployer(employer);

        final OnlinePlea.Employment employment = new OnlinePlea.Employment();
        employment.setEmploymentStatus("employment_status");
        onlinePlea.setEmployment(employment);

        final OnlinePlea.Outgoings outgoings = new OnlinePlea.Outgoings();
        outgoings.setAccommodationAmount(BigDecimal.TEN);
        onlinePlea.setOutgoings(outgoings);

        return onlinePlea;
    }

    private OnlinePlea buildOnlinePleaWithLegalEntity(final CaseDetail caseDetail1, final ZonedDateTime onlinePleaSubmittedOn, String home, String mobile, String email) {
        final LegalEntityFinancialMeans legalEntityFinancialMeans = new LegalEntityFinancialMeans(true, 100, BigDecimal.valueOf(1000), BigDecimal.valueOf(100));
        final Address address = new Address();
        address.setAddress1("test");
        address.setPostcode("MK9 1AN");
        final OnlinePlea.PleaDetails pleaDetails = new OnlinePlea.PleaDetails();
        pleaDetails.setInterpreterLanguage("interpreter");
        pleaDetails.setUnavailability("unavailability");
        pleaDetails.setWitnessDetails("witnessDetails");
        pleaDetails.setWitnessDispute("witnessDispute");
        pleaDetails.setSpeakWelsh(true);
        final OnlinePleaLegalEntityDetails legalEntityDetails = new OnlinePleaLegalEntityDetails("companyLegal", "Director", legalEntityFinancialMeans, address, home, mobile, email);
        final OnlinePlea onlinePlea = new OnlinePlea(caseDetail1.getId(), pleaDetails, caseDetail1.getDefendant().getId(), null, onlinePleaSubmittedOn, legalEntityDetails);
        return onlinePlea;
    }

}