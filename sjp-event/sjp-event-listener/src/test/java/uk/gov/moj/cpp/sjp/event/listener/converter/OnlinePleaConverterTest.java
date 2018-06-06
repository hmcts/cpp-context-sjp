package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static uk.gov.moj.cpp.sjp.domain.IncomeFrequency.FORTNIGHTLY;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaOutgoingOption;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OnlinePleaConverterTest {

    @InjectMocks
    private OnlinePleaConverter onlinePleaConverter = new OnlinePleaConverter();

    private DefendantDetail defendantDetail;
    private UUID defendantId;
    private Income income;
    private Benefits benefits;

    private Clock clock = new UtcClock();
    private ZonedDateTime now = clock.now();

    @Before
    public void setup() {
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(UUID.randomUUID());

        defendantId = UUID.randomUUID();
        defendantDetail = new DefendantDetail();
        defendantDetail.setCaseDetail(caseDetail);

        income = new Income(FORTNIGHTLY, BigDecimal.valueOf(1.0));
        benefits = new Benefits(true, "type", true);
    }

    private void commonAssertionsForFinancialMeansFields(final OnlinePlea onlinePlea, final FinancialMeansUpdated financialMeansUpdated) {
        assertThat(onlinePlea.getCaseId(), equalTo(defendantDetail.getCaseDetail().getId()));
        assertThat(onlinePlea.getDefendantId(), equalTo(financialMeansUpdated.getDefendantId()));
        assertThat(onlinePlea.getEmployment().getIncomePaymentFrequency(), equalTo(financialMeansUpdated.getIncome().getFrequency()));
        assertThat(onlinePlea.getEmployment().getIncomePaymentAmount(), equalTo(financialMeansUpdated.getIncome().getAmount()));
        assertThat(onlinePlea.getEmployment().getBenefitsClaimed(), equalTo(financialMeansUpdated.getBenefits().getClaimed()));
        assertThat(onlinePlea.getEmployment().getBenefitsType(), equalTo(financialMeansUpdated.getBenefits().getType()));
        assertThat(onlinePlea.getSubmittedOn(), equalTo(financialMeansUpdated.getUpdatedDate()));
    }

    private List<Outgoing> generateOutgoings(final BigDecimal accommodationAmount, final BigDecimal councilTaxAmount,
                                             final BigDecimal householdBillsAmount, final BigDecimal travelAmount,
                                             final BigDecimal childMaintenanceAmount, final String otherDescription,
                                             final BigDecimal otherAmount, final String secondOtherDescription,
                                             final BigDecimal secondOtherAmount) {
        final List<Outgoing> outgoings = new ArrayList<>();
        outgoings.add(new Outgoing(OnlinePleaOutgoingOption.ACCOMMODATION.getDescription(), accommodationAmount));
        outgoings.add(new Outgoing(OnlinePleaOutgoingOption.COUNCIL_TAX.getDescription(), councilTaxAmount));
        outgoings.add(new Outgoing(OnlinePleaOutgoingOption.HOUSEHOLD_BILLS.getDescription(), householdBillsAmount));
        outgoings.add(new Outgoing(OnlinePleaOutgoingOption.TRAVEL_EXPENSES.getDescription(), travelAmount));
        outgoings.add(new Outgoing(OnlinePleaOutgoingOption.CHILD_MAINTENANCE.getDescription(), childMaintenanceAmount));
        outgoings.add(new Outgoing(otherDescription, otherAmount));
        if (secondOtherDescription != null) {
            outgoings.add(new Outgoing(secondOtherDescription, secondOtherAmount));
        }
        return outgoings;
    }

    @Test
    public void shouldConvertToOnlinePleaEntityForFinancialMeansWithoutOutgoings() {
        final ZonedDateTime now = clock.now();
        final String employmentStatus = OnlinePleaConverter.OnlinePleaEmploymentStatus.EMPLOYED.name();
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(defendantId,
                income, benefits, employmentStatus, null, now);

        final OnlinePlea onlinePlea = onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated);

        assertNull(onlinePlea.getOutgoings().getAccommodationAmount());
        assertThat(onlinePlea.getEmployment().getEmploymentStatus(), equalTo(OnlinePleaConverter.OnlinePleaEmploymentStatus.EMPLOYED.name()));
        assertNull(onlinePlea.getEmployment().getEmploymentStatusDetails());

        //common assertions
        commonAssertionsForFinancialMeansFields(onlinePlea, financialMeansUpdated);
    }

    @Test
    public void shouldConvertToOnlinePleaEntityForFinancialMeansWithUnknownEmploymentStatus() {
        final String employmentStatus = "different status";
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(defendantId,
                income, benefits, employmentStatus, null, now);
        final OnlinePlea onlinePlea = onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated);

        assertThat(onlinePlea.getEmployment().getEmploymentStatus(), equalTo(OnlinePleaConverter.OnlinePleaEmploymentStatus.OTHER.name()));
        assertThat(onlinePlea.getEmployment().getEmploymentStatusDetails(), equalTo(employmentStatus));

        //common assertions
        commonAssertionsForFinancialMeansFields(onlinePlea, financialMeansUpdated);
        assertNull(onlinePlea.getOutgoings().getAccommodationAmount());
    }

    @Test
    public void shouldConvertToOnlinePleaEntityForFinancialMeansWithOutgoings() {
        final BigDecimal accommodationAmount = BigDecimal.valueOf(2344.55);
        final BigDecimal councilTaxAmount = BigDecimal.valueOf(245.85);
        final BigDecimal householdBillsAmount = BigDecimal.valueOf(200.00);
        final BigDecimal travelAmount = BigDecimal.valueOf(100.55);
        final BigDecimal childMaintenanceAmount = BigDecimal.valueOf(500);
        final String otherDescription = "extras";
        final BigDecimal otherAmount = BigDecimal.valueOf(6000);
        final List<Outgoing> outgoings = generateOutgoings(accommodationAmount, councilTaxAmount, householdBillsAmount,
                travelAmount, childMaintenanceAmount, otherDescription, otherAmount, null, null);
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(defendantId,
                income, benefits, OnlinePleaConverter.OnlinePleaEmploymentStatus.UNEMPLOYED.name(), outgoings, now);

        final OnlinePlea onlinePlea = onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated);

        //assertions for outgoings
        assertThat(onlinePlea.getOutgoings().getAccommodationAmount(), equalTo(accommodationAmount));
        assertThat(onlinePlea.getOutgoings().getCouncilTaxAmount(), equalTo(councilTaxAmount));
        assertThat(onlinePlea.getOutgoings().getHouseholdBillsAmount(), equalTo(householdBillsAmount));
        assertThat(onlinePlea.getOutgoings().getTravelExpensesAmount(), equalTo(travelAmount));
        assertThat(onlinePlea.getOutgoings().getChildMaintenanceAmount(), equalTo(childMaintenanceAmount));
        assertThat(onlinePlea.getOutgoings().getOtherDescription(), equalTo(otherDescription));
        assertThat(onlinePlea.getOutgoings().getOtherAmount(), equalTo(otherAmount));
        assertThat(onlinePlea.getOutgoings().getMonthlyAmount(), equalTo(BigDecimal.valueOf(9390.95)));

        //common assertions
        commonAssertionsForFinancialMeansFields(onlinePlea, financialMeansUpdated);
        assertThat(onlinePlea.getEmployment().getEmploymentStatus(), equalTo(OnlinePleaConverter.OnlinePleaEmploymentStatus.UNEMPLOYED.name()));
        assertNull(onlinePlea.getEmployment().getEmploymentStatusDetails());
    }

    @Test
    public void shouldConvertToOnlinePleaEntityForFinancialMeansWithOneOtherOutgoingWithoutDescription() {
        final BigDecimal otherAmount = BigDecimal.valueOf(6000);
        final List<Outgoing> outgoings = generateOutgoings(null, null, null,
                null, null, null, otherAmount, null, null);
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(defendantId,
                income, benefits, OnlinePleaConverter.OnlinePleaEmploymentStatus.UNEMPLOYED.name(), outgoings, now);

        final OnlinePlea onlinePlea = onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated);

        //assertions for outgoings
        assertThat(onlinePlea.getOutgoings().getAccommodationAmount(), equalTo(null));
        assertThat(onlinePlea.getOutgoings().getCouncilTaxAmount(), equalTo(null));
        assertThat(onlinePlea.getOutgoings().getHouseholdBillsAmount(), equalTo(null));
        assertThat(onlinePlea.getOutgoings().getTravelExpensesAmount(), equalTo(null));
        assertThat(onlinePlea.getOutgoings().getChildMaintenanceAmount(), equalTo(null));
        assertThat(onlinePlea.getOutgoings().getOtherDescription(), equalTo(null));
        assertThat(onlinePlea.getOutgoings().getOtherAmount(), equalTo(otherAmount));
        assertThat(onlinePlea.getOutgoings().getMonthlyAmount(), equalTo(BigDecimal.valueOf(6000)));

        //common assertions
        commonAssertionsForFinancialMeansFields(onlinePlea, financialMeansUpdated);
        assertThat(onlinePlea.getEmployment().getEmploymentStatus(), equalTo(OnlinePleaConverter.OnlinePleaEmploymentStatus.UNEMPLOYED.name()));
        assertNull(onlinePlea.getEmployment().getEmploymentStatusDetails());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenMoreThanOneUnknownOutgoingIsSubmitted() {
        final BigDecimal accommodationAmount = BigDecimal.valueOf(2344.55);
        final BigDecimal councilTaxAmount = BigDecimal.valueOf(245.85);
        final BigDecimal householdBillsAmount = BigDecimal.valueOf(200.00);
        final BigDecimal travelAmount = BigDecimal.valueOf(100.55);
        final BigDecimal childMaintenanceAmount = BigDecimal.valueOf(500);
        final String otherDescription = "extras";
        final BigDecimal otherAmount = BigDecimal.valueOf(6000);
        final String secondOtherDescription = "extras 2";
        final BigDecimal secondOtherAmount = BigDecimal.valueOf(8000);
        final List<Outgoing> outgoings = generateOutgoings(accommodationAmount, councilTaxAmount, householdBillsAmount,
                travelAmount, childMaintenanceAmount, otherDescription, otherAmount, secondOtherDescription, secondOtherAmount);
        final FinancialMeansUpdated financialMeansUpdated = FinancialMeansUpdated.createEventForOnlinePlea(defendantId,
                income, benefits, OnlinePleaConverter.OnlinePleaEmploymentStatus.UNEMPLOYED.name(), outgoings, now);

        onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated);
    }
}