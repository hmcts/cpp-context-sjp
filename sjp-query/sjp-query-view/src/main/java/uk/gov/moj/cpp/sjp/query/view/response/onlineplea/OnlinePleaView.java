package uk.gov.moj.cpp.sjp.query.view.response.onlineplea;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;

import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityFinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OnlinePleaView {

    private UUID caseId;

    private UUID defendantId;

    private ZonedDateTime submittedOn;

    private List<OnlinePleaDetailView> onlinePleaDetails = new ArrayList<>();

    private OnlinePleaPersonalDetailsView personalDetails;

    private PleaDetailsView pleaDetails;

    private Employment employment;

    private Employer employer;

    private Outgoings outgoings;

    private OnlinePleaLegalEntityDetails onlinePleaLegalEntityDetails;

    public OnlinePleaView(){
    }

    public OnlinePleaView(final OnlinePlea onlinePlea) {
        this.caseId = onlinePlea.getCaseId();
        this.defendantId = onlinePlea.getDefendantId();
        this.submittedOn = onlinePlea.getSubmittedOn();

        this.personalDetails = ofNullable(onlinePlea.getPersonalDetails())
                .map(OnlinePleaPersonalDetailsView::new)
                .orElse(null);

        this.pleaDetails = ofNullable(onlinePlea.getPleaDetails())
                .map(PleaDetailsView::new)
                .orElse(null);

        this.employment = ofNullable(onlinePlea.getEmployment())
                .map(Employment::new)
                .orElse(null);

        this.employer = ofNullable(onlinePlea.getEmployer())
                .map(Employer::new)
                .orElse(null);

        this.outgoings = ofNullable(onlinePlea.getOutgoings())
                .map(Outgoings::new)
                .orElse(null);

        this.onlinePleaLegalEntityDetails = ofNullable(onlinePlea.getLegalEntityDetails())
                .map(OnlinePleaLegalEntityDetails::new)
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

    public List<OnlinePleaDetailView> getOnlinePleaDetails() {
        return unmodifiableList(onlinePleaDetails);
    }

    public OnlinePleaLegalEntityDetails getOnlinePleaLegalEntityDetails() {
        return onlinePleaLegalEntityDetails;
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

    public PleaDetailsView getPleaDetails() {
        return pleaDetails;
    }

    public Employment getEmployment() {
        return employment;
    }

    public Employer getEmployer() {
        return employer;
    }

    public Outgoings getOutgoings() {
        return outgoings;
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

        public PleaDetailsView(final OnlinePlea.PleaDetails pleaDetails) {
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

        public Employment(final OnlinePlea.Employment employment) {
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

        public Employer(final OnlinePlea.Employer employer) {
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

        public Outgoings(final OnlinePlea.Outgoings outgoings) {
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

    public static class OnlinePleaLegalEntityDetails {

        private String legalEntityName;
        private String positionOfRepresentative;

        private Address address;

        private String homeTelephone;

        private String mobile;

        private String email;

        private LegalEntityFinancialMeans legalEntityFinancialMeans;

        public String getLegalEntityName() {
            return legalEntityName;
        }

        public String getPositionOfRepresentative() {
            return positionOfRepresentative;
        }

        public Address getAddress() {
            return address;
        }

        public String getHomeTelephone() {
            return homeTelephone;
        }

        public String getMobile() {
            return mobile;
        }

        public String getEmail() {
            return email;
        }

        public LegalEntityFinancialMeans getLegalEntityFinancialMeans() {
            return legalEntityFinancialMeans;
        }

        public OnlinePleaLegalEntityDetails(final uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaLegalEntityDetails onlinePleaLegalEntityDetails) {
            this.legalEntityName = onlinePleaLegalEntityDetails.getLegalEntityName();
            this.address = onlinePleaLegalEntityDetails.getAddress();
            this.homeTelephone = onlinePleaLegalEntityDetails.getHomeTelephone();
            this.mobile  = onlinePleaLegalEntityDetails.getMobile();
            this.email = onlinePleaLegalEntityDetails.getEmail();
            this.positionOfRepresentative = onlinePleaLegalEntityDetails.getPositionOfRepresentative();
            this.legalEntityFinancialMeans  = new LegalEntityFinancialMeans();
            if (nonNull(onlinePleaLegalEntityDetails.getLegalEntityFinancialMeans())) {
                this.legalEntityFinancialMeans.setGrossTurnover(onlinePleaLegalEntityDetails.getLegalEntityFinancialMeans().getGrossTurnover());
                this.legalEntityFinancialMeans.setNetTurnover(onlinePleaLegalEntityDetails.getLegalEntityFinancialMeans().getNetTurnover());
                this.legalEntityFinancialMeans.setTradingMoreThan12Months(onlinePleaLegalEntityDetails.getLegalEntityFinancialMeans().getTradingMoreThan12Months());
                this.legalEntityFinancialMeans.setNumberOfEmployees(onlinePleaLegalEntityDetails.getLegalEntityFinancialMeans().getNumberOfEmployees());
            }
        }
    }
}
