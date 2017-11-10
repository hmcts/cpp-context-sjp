package uk.gov.moj.cpp.sjp.domain.plea;

import java.io.Serializable;

public class HouseholdExpenses implements Serializable {

    private final Money accommodation;
    private final Money utilityBills;
    private final Money insurance;
    private final Money councilTax;
    private final boolean otherContributors;

    public HouseholdExpenses(Money accommodation, Money utilityBills, Money insurance, Money councilTax,
                             boolean otherContributors) {
        this.accommodation = accommodation;
        this.utilityBills = utilityBills;
        this.insurance = insurance;
        this.councilTax = councilTax;
        this.otherContributors = otherContributors;
    }

    public Money getAccommodation() {
        return accommodation;
    }

    public Money getUtilityBills() {
        return utilityBills;
    }

    public Money getInsurance() {
        return insurance;
    }

    public Money getCouncilTax() {
        return councilTax;
    }

    public boolean isOtherContributors() {
        return otherContributors;
    }
}
