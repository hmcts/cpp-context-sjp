package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
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

    @Inject
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private Clock clock;

    private UUID caseId;

    @Before
    public void set() {
        caseId = UUID.randomUUID();
        final CaseDetail caseDetail = getCaseWithDefendant(caseId);
        caseRepository.save(caseDetail);

        final OnlinePlea insertedOnlinePlea = buildOnlinePlea(caseDetail, clock.now());
        onlinePleaRepository.save(insertedOnlinePlea);
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

        final OnlinePlea.PleaDetails pleaDetails = new OnlinePlea.PleaDetails();
        pleaDetails.setInterpreterLanguage("interpreter");
        pleaDetails.setUnavailability("unavailability");
        pleaDetails.setWitnessDetails("witnessDetails");
        pleaDetails.setWitnessDispute("witnessDispute");
        pleaDetails.setSpeakWelsh(true);

        final OnlinePlea onlinePlea = new OnlinePlea(caseDetail.getId(), pleaDetails, caseDetail.getDefendant().getId(), personalDetails, onlinePleaSubmittedOn);

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

}