package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "online_plea")
public class OnlinePlea {
    @Id
    @Column(name = "case_id")
    private UUID caseId;
    @ManyToOne
    @JoinColumn(name = "defendant_id")
    @JsonIgnore
    private DefendantDetail defendantDetail;
    @Column(name = "submitted_on")
    private ZonedDateTime submittedOn;
    @Embedded
    private OnlinePleaPersonalDetails personalDetails;
    @Embedded
    private PleaDetails pleaDetails;
    @Embedded
    private OnlinePlea.Employment employment;
    @Embedded
    private OnlinePlea.Employer employer;
    @Embedded
    private Outgoings outgoings;

    public OnlinePlea() { }

    public OnlinePlea(final UUID caseId, final FinancialMeansUpdated financialMeansUpdated, final String employmentStatus,
                      final String employmentStatusDetails, final Outgoings outgoings) {
        this.caseId = caseId;
        this.defendantDetail = new DefendantDetail(financialMeansUpdated.getDefendantId());
        this.employment = new Employment(financialMeansUpdated, employmentStatus, employmentStatusDetails);
        this.outgoings = outgoings;
        this.submittedOn = financialMeansUpdated.getUpdatedDate();
    }

    public OnlinePlea(final UUID caseId, final EmployerUpdated employerUpdated) {
        this.caseId = caseId;
        this.defendantDetail = new DefendantDetail(employerUpdated.getDefendantId());
        this.employer = new Employer(employerUpdated);
        this.submittedOn = employerUpdated.getUpdatedDate();
    }

    public OnlinePlea(TrialRequested trialRequested) {
        this.caseId = trialRequested.getCaseId();
        this.pleaDetails = new PleaDetails(trialRequested);
        this.submittedOn = trialRequested.getUpdatedDate();
    }

    public OnlinePlea(final UUID caseId, final String interpreterLanguage, final ZonedDateTime updatedDate) {
        this.caseId = caseId;
        this.pleaDetails = new PleaDetails(interpreterLanguage);
        this.submittedOn = updatedDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getDefendantId() {
        return defendantDetail.getId();
    }

    public void setDefendantDetail(final DefendantDetail defendantDetail) {
        this.defendantDetail = defendantDetail;
    }

    public ZonedDateTime getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(ZonedDateTime submittedOn) {
        this.submittedOn = submittedOn;
    }

    public OnlinePleaPersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public void setPersonalDetails(OnlinePleaPersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public PleaDetails getPleaDetails() {
        return pleaDetails;
    }

    public void setPleaDetails(PleaDetails pleaDetails) {
        this.pleaDetails = pleaDetails;
    }

    public Employment getEmployment() {
        return employment;
    }

    public void setEmployment(Employment employment) {
        this.employment = employment;
    }

    public Employer getEmployer() {
        return employer;
    }

    public void setEmployer(final Employer employer) {
        this.employer = employer;
    }

    public Outgoings getOutgoings() {
        return outgoings;
    }

    public void setOutgoings(Outgoings outgoings) {
        this.outgoings = outgoings;
    }

    @Transient
    public List<Offence> getOffences() {
        return this.defendantDetail.getOffences()
                .stream()
                .map(offence ->
                    new Offence(offence.getId().toString(),
                            PleaType.valueOf(offence.getPlea()),
                            offence.getMitigation(),
                            offence.getNotGuiltyBecause()))
                .collect(Collectors.toList());
    }

    @Embeddable
    public static class PleaDetails {
        @Column(name = "interpreter_language")
        private String interpreterLanguage;
        @Column(name = "witness_dispute")
        private String witnessDispute;
        @Column(name = "witness_details")
        private String witnessDetails;
        @Column(name = "unavailability")
        private String unavailability;

        public PleaDetails() {}

        public PleaDetails(final String interpreterLanguage) {
            this.interpreterLanguage = interpreterLanguage;
        }

        public PleaDetails(final TrialRequested trialRequested) {
            this.witnessDispute = trialRequested.getWitnessDispute();
            this.witnessDetails = trialRequested.getWitnessDetails();
            this.unavailability = trialRequested.getUnavailability();
        }

        public String getInterpreterLanguage() {
            return interpreterLanguage;
        }

        public void setInterpreterLanguage(String interpreterLanguage) {
            this.interpreterLanguage = interpreterLanguage;
        }

        @Transient
        public boolean isInterpreterRequired() {
            return !StringUtils.isEmpty(interpreterLanguage);
        }

        public String getWitnessDispute() {
            return witnessDispute;
        }

        public void setWitnessDispute(final String witnessDispute) {
            this.witnessDispute = witnessDispute;
        }

        public String getWitnessDetails() {
            return witnessDetails;
        }

        public void setWitnessDetails(String witnessDetails) {
            this.witnessDetails = witnessDetails;
        }

        public String getUnavailability() {
            return unavailability;
        }

        public void setUnavailability(String unavailability) {
            this.unavailability = unavailability;
        }
    }

    @Embeddable
    public static class Employment {
        @Column(name = "income_payment_frequency")
        @Enumerated(value = EnumType.STRING)
        private IncomeFrequency incomePaymentFrequency;
        @Column(name = "income_payment_amount")
        private BigDecimal incomePaymentAmount;
        @Column(name = "employment_status")
        private String employmentStatus;
        @Column(name = "employment_status_details")
        private String employmentStatusDetails;
        @Column(name = "benefits_claimed")
        private Boolean benefitsClaimed;
        @Column(name = "benefits_type")
        private String benefitsType;
        @Column(name = "benefits_deduct_penalty_preference")
        private Boolean benefitsDeductPenaltyPreference;

        public Employment() {}

        public Employment(final FinancialMeansUpdated financialMeansUpdated, String employmentStatus, String employmentStatusDetails) {
            this.incomePaymentFrequency = financialMeansUpdated.getIncome().getFrequency();
            this.incomePaymentAmount = financialMeansUpdated.getIncome().getAmount();
            this.employmentStatus = employmentStatus;
            this.employmentStatusDetails = employmentStatusDetails;
            this.benefitsClaimed = financialMeansUpdated.getBenefits().getClaimed();
            this.benefitsType = financialMeansUpdated.getBenefits().getType();
            this.benefitsDeductPenaltyPreference = financialMeansUpdated.getBenefits().getDeductPenaltyPreference();
        }

        public IncomeFrequency getIncomePaymentFrequency() {
            return incomePaymentFrequency;
        }

        public void setIncomePaymentFrequency(IncomeFrequency incomePaymentFrequency) {
            this.incomePaymentFrequency = incomePaymentFrequency;
        }

        public BigDecimal getIncomePaymentAmount() {
            return incomePaymentAmount;
        }

        public void setIncomePaymentAmount(BigDecimal incomePaymentAmount) {
            this.incomePaymentAmount = incomePaymentAmount;
        }

        public String getEmploymentStatus() {
            return employmentStatus;
        }

        public void setEmploymentStatus(String employmentStatus) {
            this.employmentStatus = employmentStatus;
        }

        public String getEmploymentStatusDetails() {
            return employmentStatusDetails;
        }

        public void setEmploymentStatusDetails(final String employmentStatusDetails) {
            this.employmentStatusDetails = employmentStatusDetails;
        }

        public Boolean getBenefitsClaimed() {
            return benefitsClaimed;
        }

        public void setBenefitsClaimed(Boolean benefitsClaimed) {
            this.benefitsClaimed = benefitsClaimed;
        }

        public String getBenefitsType() {
            return benefitsType;
        }

        public void setBenefitsType(String benefitsType) {
            this.benefitsType = benefitsType;
        }

        public Boolean getBenefitsDeductPenaltyPreference() {
            return benefitsDeductPenaltyPreference;
        }

        public void setBenefitsDeductPenaltyPreference(Boolean benefitsDeductPenaltyPreference) {
            this.benefitsDeductPenaltyPreference = benefitsDeductPenaltyPreference;
        }
    }

    @Embeddable
    public static class Employer {
        @Column(name = "employee_reference")
        private String employeeReference;
        @Column(name = "employer_name")
        private String name;
        @Column(name = "employer_phone")
        private String phone;

        @AttributeOverrides({
                @AttributeOverride(name="address1", column=@Column(name="employer_address_1")),
                @AttributeOverride(name="address2", column=@Column(name="employer_address_2")),
                @AttributeOverride(name="address3", column=@Column(name="employer_address_3")),
                @AttributeOverride(name="address4", column=@Column(name="employer_address_4")),
                @AttributeOverride(name="postcode", column=@Column(name="employer_postcode"))
        })
        private Address address;

        public Employer() {}

        public Employer(final EmployerUpdated employerUpdated) {
            this.employeeReference = employerUpdated.getEmployeeReference();
            this.name = employerUpdated.getName();
            this.phone = employerUpdated.getPhone();
            if (employerUpdated.getAddress() != null) {
                this.address = new Address(employerUpdated.getAddress().getAddress1(), employerUpdated.getAddress().getAddress2(), employerUpdated.getAddress().getAddress3(),
                        employerUpdated.getAddress().getAddress4(), employerUpdated.getAddress().getPostcode());
            }
            else {
                this.address = new Address();
            }
        }

        public String getEmployeeReference() {
            return employeeReference;
        }

        public void setEmployeeReference(String employeeReference) {
            this.employeeReference = employeeReference;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }


    }

    @Embeddable
    public static class Outgoings {
        @Column(name = "outgoing_accommodation_amount")
        private BigDecimal accommodationAmount;
        @Column(name = "outgoing_council_tax_amount")
        private BigDecimal councilTaxAmount;
        @Column(name = "outgoing_household_bills_amount")
        private BigDecimal householdBillsAmount;
        @Column(name = "outgoing_travel_expenses_amount")
        private BigDecimal travelExpensesAmount;
        @Column(name = "outgoing_child_maintenance_amount")
        private BigDecimal childMaintenanceAmount;
        @Column(name = "outgoing_other_description")
        private String otherDescription;
        @Column(name = "outgoing_other_amount")
        private BigDecimal otherAmount;

        public BigDecimal getMonthlyAmount() {
            return Stream.of(accommodationAmount,
                    councilTaxAmount,
                    householdBillsAmount,
                    travelExpensesAmount,
                    childMaintenanceAmount,
                    otherAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.valueOf(0), BigDecimal::add);
        }

        public BigDecimal getAccommodationAmount() {
            return accommodationAmount;
        }

        public void setAccommodationAmount(BigDecimal accommodationAmount) {
            this.accommodationAmount = accommodationAmount;
        }

        public BigDecimal getCouncilTaxAmount() {
            return councilTaxAmount;
        }

        public void setCouncilTaxAmount(BigDecimal councilTaxAmount) {
            this.councilTaxAmount = councilTaxAmount;
        }

        public BigDecimal getHouseholdBillsAmount() {
            return householdBillsAmount;
        }

        public void setHouseholdBillsAmount(BigDecimal householdBillsAmount) {
            this.householdBillsAmount = householdBillsAmount;
        }

        public BigDecimal getTravelExpensesAmount() {
            return travelExpensesAmount;
        }

        public void setTravelExpensesAmount(BigDecimal travelExpensesAmount) {
            this.travelExpensesAmount = travelExpensesAmount;
        }

        public BigDecimal getChildMaintenanceAmount() {
            return childMaintenanceAmount;
        }

        public void setChildMaintenanceAmount(BigDecimal childMaintenanceAmount) {
            this.childMaintenanceAmount = childMaintenanceAmount;
        }

        public String getOtherDescription() {
            return otherDescription;
        }

        public void setOtherDescription(String otherDescription) {
            this.otherDescription = otherDescription;
        }

        public BigDecimal getOtherAmount() {
            return otherAmount;
        }

        public void setOtherAmount(BigDecimal otherAmount) {
            this.otherAmount = otherAmount;
        }
    }
}
