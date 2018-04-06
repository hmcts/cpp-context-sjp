package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaPersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

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

    private OnlinePlea insertedOnlinePlea;

    @Inject
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Inject
    private CaseRepository caseRepository;

    private CaseDetail caseDetail;

    @Before
    public void set() {
        caseDetail = getCaseWithDefendantOffences();
        caseRepository.save(caseDetail);

        insertedOnlinePlea = buildOnlinePlea(caseDetail);
        onlinePleaRepository.save(insertedOnlinePlea);
    }

    @Test
    public void shouldFindOnlinePleaWithoutFinances() {
        // ASSUME:
        assertThat(insertedOnlinePlea.getPleaDetails(), notNullValue());
        assertThat(insertedOnlinePlea.getOutgoings(), notNullValue());
        assertThat(insertedOnlinePlea.getEmployment(), notNullValue());
        assertThat(insertedOnlinePlea.getEmployer(), notNullValue());

        // WHEN
        OnlinePlea actualOnlinePlea = onlinePleaRepository.findOnlinePleaWithoutFinances(insertedOnlinePlea.getCaseId());

        // THEN
        assertThat(actualOnlinePlea.getPleaDetails(), notNullValue());
        assertThat(actualOnlinePlea.getOutgoings(), nullValue());
        assertThat(actualOnlinePlea.getEmployment(), nullValue());
        assertThat(actualOnlinePlea.getEmployer(), nullValue());

        List<String> excludedFields = asList("pleaDetails", "personalDetails", "outgoings", "employment", "employer");
        assertTrue(reflectionEquals(actualOnlinePlea, insertedOnlinePlea, excludedFields));
        assertTrue(reflectionEquals(actualOnlinePlea.getPleaDetails(), insertedOnlinePlea.getPleaDetails()));
        assertTrue(reflectionEquals(actualOnlinePlea.getPersonalDetails(), insertedOnlinePlea.getPersonalDetails()));
    }

    private static CaseDetail getCaseWithDefendantOffences() {
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(UUID.randomUUID());
        caseDetail.setOnlinePleaReceived(true);
        caseDetail.setDefendant(
                new DefendantDetail(UUID.randomUUID(), new PersonalDetails(), emptySet(), 1));

        return caseDetail;
    }

    private static OnlinePlea buildOnlinePlea(CaseDetail caseDetail) {
        OnlinePleaPersonalDetails personalDetails = new OnlinePleaPersonalDetails();
        personalDetails.setFirstName("first_name");
        personalDetails.setLastName("last_name");
        personalDetails.setAddress(new Address("address1", "address2", "address3", "address4", "postcode"));
        personalDetails.setEmail("email@email.email");
        personalDetails.setNationalInsuranceNumber("national_number");
        personalDetails.setHomeTelephone("123456789");
        personalDetails.setMobile("987654321");

        OnlinePlea.PleaDetails pleaDetails = new OnlinePlea.PleaDetails();
        pleaDetails.setInterpreterLanguage("interpreter");
        pleaDetails.setUnavailability("unavailability");
        pleaDetails.setWitnessDetails("witnessDetails");
        pleaDetails.setWitnessDispute("witnessDispute");

        OnlinePlea onlinePlea = new OnlinePlea(caseDetail.getId(), pleaDetails, caseDetail.getDefendant(), personalDetails, ZonedDateTime.now());

        OnlinePlea.Employer employer = new OnlinePlea.Employer();
        employer.setName("employer_name");
        onlinePlea.setEmployer(employer);

        OnlinePlea.Employment employment = new OnlinePlea.Employment();
        employment.setEmploymentStatus("employment_status");
        onlinePlea.setEmployment(employment);

        OnlinePlea.Outgoings outgoings = new OnlinePlea.Outgoings();
        outgoings.setAccommodationAmount(BigDecimal.TEN);
        onlinePlea.setOutgoings(outgoings);

        return onlinePlea;
    }

}