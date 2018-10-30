package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.util.Arrays.asList;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequested;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "online_plea")
public class OnlinePlea {
    @Id
    @Column(name = "case_id", updatable = false, nullable = false)
    private UUID caseId;

    @Column(name = "defendant_id", updatable = false, nullable = false)
    private UUID defendantId;

    @Column(name = "submitted_on", updatable = false, nullable = false)
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
        this(caseId, financialMeansUpdated.getDefendantId(), financialMeansUpdated.getUpdatedDate());
        this.employment = new Employment(financialMeansUpdated, employmentStatus, employmentStatusDetails);
        this.outgoings = outgoings;
    }

    public OnlinePlea(final UUID caseId, final EmployerUpdated employerUpdated) {
        this(caseId, employerUpdated.getDefendantId(), employerUpdated.getUpdatedDate());
        this.employer = new Employer(employerUpdated);
    }

    public OnlinePlea(final TrialRequested trialRequested) {
        this(trialRequested.getCaseId(), new PleaDetails(trialRequested), trialRequested.getUpdatedDate());
    }

    public OnlinePlea(final PleaUpdated pleaUpdated) {
        this(pleaUpdated.getCaseId(), new PleaDetails(pleaUpdated), pleaUpdated.getUpdatedDate());
    }

    public OnlinePlea(final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant) {
        this(interpreterUpdatedForDefendant.getCaseId(), new PleaDetails(interpreterUpdatedForDefendant), interpreterUpdatedForDefendant.getUpdatedDate());
    }

    public OnlinePlea(final HearingLanguagePreferenceUpdatedForDefendant hearingLanguageUpdated) {
        this(hearingLanguageUpdated.getCaseId(), new PleaDetails(hearingLanguageUpdated), hearingLanguageUpdated.getUpdatedDate());
    }

    public OnlinePlea(final DefendantDetailsUpdated defendantDetailsUpdated) {
        this(defendantDetailsUpdated.getCaseId(), defendantDetailsUpdated.getDefendantId(), defendantDetailsUpdated.getUpdatedDate());
        this.personalDetails = new OnlinePleaPersonalDetails(defendantDetailsUpdated);
    }

    /**
     * Used in {@link OnlinePleaRepository#findOnlinePleaWithoutFinances} to filter finances
     * It must include every field apart finances (employment, employer, outgoings)
     */
    public OnlinePlea(final UUID caseId, final PleaDetails pleaDetails, final UUID defendantId, final OnlinePleaPersonalDetails personalDetails, final ZonedDateTime submittedOn) {
        this(caseId, pleaDetails, submittedOn);
        this.defendantId = defendantId;
        this.personalDetails = personalDetails;
    }

    private OnlinePlea(final UUID caseId, final UUID defendantId, final ZonedDateTime submittedOn) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.submittedOn = submittedOn;
    }

    private OnlinePlea(final UUID caseId, final PleaDetails pleaDetails, final ZonedDateTime submittedOn) {
        this.caseId = caseId;
        this.pleaDetails = pleaDetails;
        this.submittedOn = submittedOn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public ZonedDateTime getSubmittedOn() {
        return submittedOn;
    }

    public void setSubmittedOn(final ZonedDateTime submittedOn) {
        this.submittedOn = submittedOn;
    }

    public OnlinePleaPersonalDetails getPersonalDetails() {
        return personalDetails;
    }

    public void setPersonalDetails(final OnlinePleaPersonalDetails personalDetails) {
        this.personalDetails = personalDetails;
    }

    public PleaDetails getPleaDetails() {
        return pleaDetails;
    }

    public void setPleaDetails(final PleaDetails pleaDetails) {
        this.pleaDetails = pleaDetails;
    }

    public Employment getEmployment() {
        return employment;
    }

    public void setEmployment(final Employment employment) {
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

    public void setOutgoings(final Outgoings outgoings) {
        this.outgoings = outgoings;
    }

    @Embeddable
    public static class PleaDetails {
        @Enumerated(EnumType.STRING)
        @Column(name = "plea")
        private PleaType plea;
        @Column(name = "come_to_court")
        private Boolean comeToCourt;
        @Column(name = "mitigation")
        private String mitigation;
        @Column(name = "not_guilty_because")
        private String notGuiltyBecause;
        @Column(name = "interpreter_language")
        private String interpreterLanguage;
        @Column(name = "witness_dispute")
        private String witnessDispute;
        @Column(name = "witness_details")
        private String witnessDetails;
        @Column(name = "unavailability")
        private String unavailability;
        @Column(name = "speak_welsh")
        private Boolean speakWelsh;

        public PleaDetails() {}

        public PleaDetails(final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant) {
            this.interpreterLanguage = Optional.ofNullable(interpreterUpdatedForDefendant.getInterpreter())
                    .map(Interpreter::getLanguage)
                    .orElse(null);
        }

        public PleaDetails(final HearingLanguagePreferenceUpdatedForDefendant hearingLanguageUpdated) {
            this.speakWelsh = hearingLanguageUpdated.getSpeakWelsh();
        }

        public PleaDetails(final TrialRequested trialRequested) {
            this.witnessDispute = trialRequested.getWitnessDispute();
            this.witnessDetails = trialRequested.getWitnessDetails();
            this.unavailability = trialRequested.getUnavailability();
        }

        public PleaDetails(final PleaUpdated pleaUpdated) {
            this.plea =  pleaUpdated.getPlea();
            this.mitigation = pleaUpdated.getMitigation();
            this.notGuiltyBecause = pleaUpdated.getNotGuiltyBecause();
            this.comeToCourt = asList(GUILTY_REQUEST_HEARING, NOT_GUILTY).contains(plea);
        }

        public PleaType getPlea() {
            return plea;
        }

        public Boolean getComeToCourt() {
            return comeToCourt;
        }

        public String getMitigation() {
            return mitigation;
        }

        public String getNotGuiltyBecause() {
            return notGuiltyBecause;
        }

        public String getInterpreterLanguage() {
            return interpreterLanguage;
        }

        public void setInterpreterLanguage(final String interpreterLanguage) {
            this.interpreterLanguage = interpreterLanguage;
        }

        @Transient
        public boolean isInterpreterRequired() {
            return Interpreter.isNeeded(interpreterLanguage);
        }

        public Boolean getSpeakWelsh() {
            return speakWelsh;
        }

        public void setSpeakWelsh(final Boolean speakWelsh) {
            this.speakWelsh = speakWelsh;
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

        public void setWitnessDetails(final String witnessDetails) {
            this.witnessDetails = witnessDetails;
        }

        public String getUnavailability() {
            return unavailability;
        }

        public void setUnavailability(final String unavailability) {
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

        public Employment(final FinancialMeansUpdated financialMeansUpdated, final String employmentStatus, final String employmentStatusDetails) {
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

        public void setIncomePaymentFrequency(final IncomeFrequency incomePaymentFrequency) {
            this.incomePaymentFrequency = incomePaymentFrequency;
        }

        public BigDecimal getIncomePaymentAmount() {
            return incomePaymentAmount;
        }

        public void setIncomePaymentAmount(final BigDecimal incomePaymentAmount) {
            this.incomePaymentAmount = incomePaymentAmount;
        }

        public String getEmploymentStatus() {
            return employmentStatus;
        }

        public void setEmploymentStatus(final String employmentStatus) {
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

        public void setBenefitsClaimed(final Boolean benefitsClaimed) {
            this.benefitsClaimed = benefitsClaimed;
        }

        public String getBenefitsType() {
            return benefitsType;
        }

        public void setBenefitsType(final String benefitsType) {
            this.benefitsType = benefitsType;
        }

        public Boolean getBenefitsDeductPenaltyPreference() {
            return benefitsDeductPenaltyPreference;
        }

        public void setBenefitsDeductPenaltyPreference(final Boolean benefitsDeductPenaltyPreference) {
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
                @AttributeOverride(name="address5", column=@Column(name="employer_address_5")),
                @AttributeOverride(name="postcode", column=@Column(name="employer_postcode"))
        })
        private Address address;

        public Employer() {}

        public Employer(final EmployerUpdated employerUpdated) {
            this.employeeReference = employerUpdated.getEmployeeReference();
            this.name = employerUpdated.getName();
            this.phone = employerUpdated.getPhone();
            this.address = Optional.ofNullable(employerUpdated.getAddress())
                    .map(employerAddress -> new Address(
                            employerAddress.getAddress1(),
                            employerAddress.getAddress2(),
                            employerAddress.getAddress3(),
                            employerAddress.getAddress4(),
                            employerAddress.getAddress5(),
                            employerAddress.getPostcode()))
                    .orElseGet(Address::new);
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
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
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
