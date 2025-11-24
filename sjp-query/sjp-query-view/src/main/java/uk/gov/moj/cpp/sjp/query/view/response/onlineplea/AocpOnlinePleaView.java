package uk.gov.moj.cpp.sjp.query.view.response.onlineplea;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;

import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpOnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AocpOnlinePleaView {

    private UUID caseId;

    private UUID defendantId;

    private ZonedDateTime submittedOn;

    private List<OnlinePleaDetailView> onlinePleaDetails = new ArrayList<>();

    private OnlinePleaPersonalDetailsView personalDetails;

    private Boolean aocpAccepted;

    public AocpOnlinePleaView(){

    }


    public AocpOnlinePleaView(final AocpOnlinePlea onlinePlea) {
        this.caseId = onlinePlea.getCaseId();
        this.defendantId = onlinePlea.getDefendantId();
        this.submittedOn = onlinePlea.getSubmittedOn();
        this.aocpAccepted = onlinePlea.getAocpAccepted();

        this.personalDetails = ofNullable(onlinePlea.getPersonalDetails())
                .map(OnlinePleaPersonalDetailsView::new)
                .orElse(null);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public ZonedDateTime getSubmittedOn() {
        return submittedOn;
    }

    public Boolean getAocpAccepted() { return aocpAccepted; }

    public List<OnlinePleaDetailView> getOnlinePleaDetails() {
        return unmodifiableList(onlinePleaDetails);
    }

    public void setOnlinePleaDetails(final List<OnlinePleaDetail> onlinePleaDetails) {
        this.onlinePleaDetails = ofNullable(onlinePleaDetails)
                .map(olPleaDetails -> olPleaDetails.stream()
                        .map(OnlinePleaDetailView::new)
                        .collect(Collectors.toList())
                ).orElse(null);
    }

    public OnlinePleaPersonalDetailsView getPersonalDetails() {
        return personalDetails;
    }

    public static class PleaDetailsView {

        private Boolean comeToCourt;

        private String interpreterLanguage;

        private String witnessDispute;

        private String witnessDetails;

        private String unavailability;

        private Boolean speakWelsh;

        private Boolean outstandingFines;

        private DisabilityNeeds disabilityNeeds;

        public PleaDetailsView(final AocpOnlinePlea.PleaDetails pleaDetails) {
            this.comeToCourt = pleaDetails.getComeToCourt();
            this.interpreterLanguage = pleaDetails.getInterpreterLanguage();
            this.witnessDispute = pleaDetails.getWitnessDispute();
            this.witnessDetails = pleaDetails.getWitnessDetails();
            this.unavailability = pleaDetails.getUnavailability();
            this.speakWelsh = pleaDetails.getSpeakWelsh();
            this.outstandingFines = pleaDetails.getOutstandingFines();
            this.disabilityNeeds = disabilityNeedsOf(pleaDetails.getDisabilityNeeds());
        }

        public Boolean getComeToCourt() {
            return comeToCourt;
        }

        public String getInterpreterLanguage() {
            return interpreterLanguage;
        }

        public boolean isInterpreterRequired() {
            return Interpreter.isNeeded(interpreterLanguage);
        }

        public String getWitnessDispute() {
            return witnessDispute;
        }

        public String getWitnessDetails() {
            return witnessDetails;
        }

        public String getUnavailability() {
            return unavailability;
        }

        public Boolean getSpeakWelsh() {
            return speakWelsh;
        }

        public Boolean getOutstandingFines() {
            return outstandingFines;
        }

        public DisabilityNeeds getDisabilityNeeds() {
            return disabilityNeeds;
        }
    }

    public static class Employment {

        private IncomeFrequency incomePaymentFrequency;

        private BigDecimal incomePaymentAmount;

        private String employmentStatus;

        private String employmentStatusDetails;

        private Boolean benefitsClaimed;

        private String benefitsType;

        private Boolean benefitsDeductPenaltyPreference;

        public Employment(final AocpOnlinePlea.Employment employment) {
            this.incomePaymentFrequency = employment.getIncomePaymentFrequency();
            this.incomePaymentAmount = employment.getIncomePaymentAmount();
            this.employmentStatus = employment.getEmploymentStatus();
            this.employmentStatusDetails = employment.getEmploymentStatusDetails();
            this.benefitsClaimed = employment.getBenefitsClaimed();
            this.benefitsType = employment.getBenefitsType();
            this.benefitsDeductPenaltyPreference = employment.getBenefitsDeductPenaltyPreference();
        }

        public IncomeFrequency getIncomePaymentFrequency() {
            return incomePaymentFrequency;
        }

        public BigDecimal getIncomePaymentAmount() {
            return incomePaymentAmount;
        }

        public String getEmploymentStatus() {
            return employmentStatus;
        }

        public String getEmploymentStatusDetails() {
            return employmentStatusDetails;
        }

        public Boolean getBenefitsClaimed() {
            return benefitsClaimed;
        }

        public String getBenefitsType() {
            return benefitsType;
        }

        public Boolean getBenefitsDeductPenaltyPreference() {
            return benefitsDeductPenaltyPreference;
        }
    }

    public static class Employer {

        private String employeeReference;

        private String name;

        private String phone;

        private Address address;

        public Employer(final AocpOnlinePlea.Employer employer) {
            this.employeeReference = employer.getEmployeeReference();
            this.name = employer.getName();
            this.phone = employer.getPhone();
            this.address = employer.getAddress();
        }

        public String getEmployeeReference() {
            return employeeReference;
        }

        public String getName() {
            return name;
        }

        public String getPhone() {
            return phone;
        }

        public Address getAddress() {
            return address;
        }
    }

    public static class Outgoings {

        private BigDecimal accommodationAmount;

        private BigDecimal councilTaxAmount;

        private BigDecimal householdBillsAmount;

        private BigDecimal travelExpensesAmount;

        private BigDecimal childMaintenanceAmount;

        private String otherDescription;

        private BigDecimal otherAmount;

        public Outgoings(final AocpOnlinePlea.Outgoings outgoings) {
            this.accommodationAmount = outgoings.getAccommodationAmount();
            this.councilTaxAmount = outgoings.getCouncilTaxAmount();
            this.householdBillsAmount = outgoings.getHouseholdBillsAmount();
            this.travelExpensesAmount = outgoings.getTravelExpensesAmount();
            this.childMaintenanceAmount = outgoings.getChildMaintenanceAmount();
            this.otherDescription = outgoings.getOtherDescription();
            this.otherAmount = outgoings.getOtherAmount();
        }

        public BigDecimal getMonthlyAmount() {
            return Stream.of(accommodationAmount,
                    councilTaxAmount,
                    householdBillsAmount,
                    travelExpensesAmount,
                    childMaintenanceAmount,
                    otherAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public BigDecimal getAccommodationAmount() {
            return accommodationAmount;
        }

        public BigDecimal getCouncilTaxAmount() {
            return councilTaxAmount;
        }

        public BigDecimal getHouseholdBillsAmount() {
            return householdBillsAmount;
        }

        public BigDecimal getTravelExpensesAmount() {
            return travelExpensesAmount;
        }

        public BigDecimal getChildMaintenanceAmount() {
            return childMaintenanceAmount;
        }

        public String getOtherDescription() {
            return otherDescription;
        }

        public BigDecimal getOtherAmount() {
            return otherAmount;
        }
    }
}
