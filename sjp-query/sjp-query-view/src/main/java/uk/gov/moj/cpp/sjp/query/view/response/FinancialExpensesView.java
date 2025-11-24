package uk.gov.moj.cpp.sjp.query.view.response;


import java.math.BigDecimal;
import java.util.UUID;

public class FinancialExpensesView {

    private UUID id;
    private BigDecimal otherSignificantExpAmount;
    private String otherSignificantExpDetails;
    private BigDecimal childMaintenance;
    private BigDecimal fines;
    private BigDecimal countyCourtOrders;
    private BigDecimal loanRepayments;
    private BigDecimal telephone;
    private BigDecimal accommodation;
    private BigDecimal utilityBills;
    private BigDecimal insurance;
    private BigDecimal councilTax;
    private Boolean otherContributors;
    private BigDecimal tvSubscriptions;
    private BigDecimal travelExpenses;


    public FinancialExpensesView(FinancialExpensesDetailsBuilder builder) {
        this.id = builder.id;
        this.accommodation = builder.accommodation;
        this.utilityBills = builder.utilityBills;
        this.insurance = builder.insurance;
        this.councilTax = builder.councilTax;
        this.otherContributors = builder.otherContributors;
        this.tvSubscriptions = builder.tvSubscriptions;
        this.travelExpenses = builder.travelExpenses;
        this.telephone = builder.telephone;
        this.loanRepayments = builder.loanRepayments;
        this.countyCourtOrders = builder.countyCourtOrders;
        this.fines = builder.fines;
        this.childMaintenance = builder.childMaintenance;
        this.otherSignificantExpDetails = builder.otherSignificantExpDetails;
        this.otherSignificantExpAmount = builder.otherSignificantExpAmt;
    }

    public static class FinancialExpensesDetailsBuilder {

        private UUID id;
        private BigDecimal otherSignificantExpAmt;
        private String otherSignificantExpDetails;
        private BigDecimal childMaintenance;
        private BigDecimal fines;
        private BigDecimal countyCourtOrders;
        private BigDecimal loanRepayments;
        private BigDecimal telephone;
        private BigDecimal accommodation;
        private BigDecimal utilityBills;
        private BigDecimal insurance;
        private BigDecimal councilTax;
        private Boolean otherContributors;
        private BigDecimal tvSubscriptions;
        private BigDecimal travelExpenses;


        public FinancialExpensesView build() {
            return new FinancialExpensesView(this);
        }

        public FinancialExpensesDetailsBuilder setId(UUID id) {
            this.id = id;
            return this;
        }

        public FinancialExpensesDetailsBuilder setOtherSignificantExpAmt(BigDecimal otherSignificantExpAmt) {
            this.otherSignificantExpAmt = otherSignificantExpAmt;
            return this;
        }

        public FinancialExpensesDetailsBuilder setOtherSignificantExpDetails(String otherSignificantExpDetails) {
            this.otherSignificantExpDetails = otherSignificantExpDetails;
            return this;
        }

        public FinancialExpensesDetailsBuilder setChildMaintenance(BigDecimal childMaintenance) {
            this.childMaintenance = childMaintenance;
            return this;
        }

        public FinancialExpensesDetailsBuilder setFines(BigDecimal fines) {
            this.fines = fines;
            return this;
        }

        public FinancialExpensesDetailsBuilder setCountyCourtOrders(BigDecimal countyCourtOrders) {
            this.countyCourtOrders = countyCourtOrders;
            return this;
        }

        public FinancialExpensesDetailsBuilder setLoanRepayments(BigDecimal loanRepayments) {
            this.loanRepayments = loanRepayments;
            return this;
        }

        public FinancialExpensesDetailsBuilder setTelephone(BigDecimal telephone) {
            this.telephone = telephone;
            return this;
        }


        public FinancialExpensesDetailsBuilder setAccommodation(BigDecimal accommodation) {
            this.accommodation = accommodation;
            return this;
        }

        public FinancialExpensesDetailsBuilder setUtilityBills(BigDecimal utilityBills) {
            this.utilityBills = utilityBills;
            return this;
        }

        public FinancialExpensesDetailsBuilder setInsurance(BigDecimal insurance) {
            this.insurance = insurance;
            return this;
        }

        public FinancialExpensesDetailsBuilder setCouncilTax(BigDecimal councilTax) {
            this.councilTax = councilTax;
            return this;
        }

        public FinancialExpensesDetailsBuilder setOtherContributors(Boolean otherContributors) {
            this.otherContributors = otherContributors;
            return this;
        }

        public FinancialExpensesDetailsBuilder setTvSubscriptions(BigDecimal tvSubscriptions) {
            this.tvSubscriptions = tvSubscriptions;
            return this;
        }

        public FinancialExpensesDetailsBuilder setTravelExpenses(BigDecimal travelExpenses) {
            this.travelExpenses = travelExpenses;
            return this;
        }
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getOtherSignificantExpAmount() {
        return otherSignificantExpAmount;
    }

    public String getOtherSignificantExpDetails() {
        return otherSignificantExpDetails;
    }

    public BigDecimal getChildMaintenance() {
        return childMaintenance;
    }

    public BigDecimal getFines() {
        return fines;
    }

    public BigDecimal getCountyCourtOrders() {
        return countyCourtOrders;
    }

    public BigDecimal getLoanRepayments() {
        return loanRepayments;
    }

    public BigDecimal getTelephone() {
        return telephone;
    }

    public BigDecimal getAccommodation() {
        return accommodation;
    }

    public BigDecimal getUtilityBills() {
        return utilityBills;
    }

    public BigDecimal getInsurance() {
        return insurance;
    }

    public BigDecimal getCouncilTax() {
        return councilTax;
    }

    public Boolean getOtherContributors() {
        return otherContributors;
    }

    public BigDecimal getTvSubscriptions() {
        return tvSubscriptions;
    }

    public BigDecimal getTravelExpenses() {
        return travelExpenses;
    }
}
