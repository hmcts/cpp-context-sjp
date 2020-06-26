package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.util.Objects.isNull;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "offence")
public class OffenceDetail implements Serializable, Comparable<OffenceDetail> {

    private static final long serialVersionUID = 1L;

    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "code")
    private String code;

    @Column(name = "plea")
    private String plea;

    @Column(name = "plea_method")
    @Enumerated(EnumType.STRING)
    private PleaMethod pleaMethod;

    @Column(name = "plea_date")
    private ZonedDateTime pleaDate;

    @Column(name = "seq_no")
    private Integer sequenceNumber;

    @Column(name = "wording")
    private String wording;

    @Column(name = "wording_welsh")
    private String wordingWelsh;

    @Convert(converter = LocalDatePersistenceConverter.class)
    @Column(name = "start_date")
    private LocalDate startDate;

    @Convert(converter = LocalDatePersistenceConverter.class)
    @Column(name = "charge_date")
    private LocalDate chargeDate;

    @ManyToOne
    @JoinColumn(name = "defendant_id")
    private DefendantDetail defendantDetail;

    @Column(name = "libra_offence_date_code")
    private int libraOffenceDateCode;

    @Column(name = "witness_statement")
    private String witnessStatement;

    @Column(name = "prosecution_facts")
    private String prosecutionFacts;

    @Column(name = "compensation")
    private BigDecimal compensation;

    @Column(name = "order_index")
    private int orderIndex;

    @Column(name = "withdrawal_request_reason_id")
    private UUID withdrawalRequestReasonId;

    @Column(name = "vehicle_make")
    private String vehicleMake;

    @Column(name = "vehicle_registration_mark")
    private String vehicleRegistrationMark;

    @Column(name = "back_duty")
    private BigDecimal backDuty;

    @Convert(converter = LocalDatePersistenceConverter.class)
    @Column(name = "back_duty_date_from")
    private LocalDate backDutyDateFrom;

    @Convert(converter = LocalDatePersistenceConverter.class)
    @Column(name = "back_duty_date_to")
    private LocalDate backDutyDateTo;

    @Column(name = "conviction")
    @Enumerated(EnumType.STRING)
    private VerdictType conviction;

    @Column(name = "conviction_date")
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate convictionDate;

    @Column(name = "endorsable")
    private Boolean endorsable;

    @Column(name = "press_restrictable")
    private Boolean pressRestrictable;

    @Embedded
    private PressRestriction pressRestriction;

    @Column(name = "completed")
    private Boolean completed;

    public OffenceDetail() {
        super();
    }

    private OffenceDetail(final OffenceDetailBuilder builder) {
        this.id = builder.id;
        this.code = builder.code;
        this.plea = builder.plea;
        this.pleaMethod = builder.pleaMethod;
        this.pleaDate = builder.pleaDate;
        this.sequenceNumber = builder.sequenceNumber;
        this.wording = builder.wording;
        this.wordingWelsh = builder.wordingWelsh;
        this.startDate = builder.startDate;
        this.chargeDate = builder.chargeDate;
        this.witnessStatement = builder.witnessStatement;
        this.prosecutionFacts = builder.prosecutionFacts;
        this.libraOffenceDateCode = builder.libraOffenceDateCode;
        this.compensation = builder.compensation;
        this.orderIndex = builder.orderIndex;
        this.withdrawalRequestReasonId = builder.withdrawalRequestReasonId;
        this.backDuty = builder.backDuty;
        this.backDutyDateFrom = builder.backDutyDateFrom;
        this.backDutyDateTo = builder.backDutyDateTo;
        this.vehicleMake = builder.vehicleMake;
        this.vehicleRegistrationMark = builder.vehicleRegistrationMark;
        this.endorsable = builder.endorsable;
        this.pressRestrictable = builder.pressRestrictable;
        this.pressRestriction = builder.pressRestriction;
        this.completed = builder.completed;
    }

    public static OffenceDetailBuilder builder() {
        return new OffenceDetailBuilder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PleaType getPlea() {
        return Optional.ofNullable(this.plea)
                .map(PleaType::valueOf)
                .orElse(null);
    }

    public void setPlea(PleaType plea) {
        this.plea = plea == null ? null : plea.name();
    }

    public ZonedDateTime getPleaDate() {
        return pleaDate;
    }

    public void setPleaDate(ZonedDateTime pleaDate) {
        this.pleaDate = pleaDate;
    }

    public PleaMethod getPleaMethod() {
        return pleaMethod;
    }

    public void setPleaMethod(PleaMethod pleaMethod) {
        this.pleaMethod = pleaMethod;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getWording() {
        return wording;
    }

    public void setWording(String wording) {
        this.wording = wording;
    }

    public String getWordingWelsh() {
        return wordingWelsh;
    }

    public void setWordingWelsh(String wordingWelsh) {
        this.wordingWelsh = wordingWelsh;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }

    public DefendantDetail getDefendantDetail() {
        return defendantDetail;
    }

    public void setDefendantDetail(DefendantDetail defendantDetail) {
        this.defendantDetail = defendantDetail;
    }

    public String getWitnessStatement() {
        return witnessStatement;
    }

    public void setWitnessStatement(String witnessStatement) {
        this.witnessStatement = witnessStatement;
    }

    public String getProsecutionFacts() {
        return prosecutionFacts;
    }

    public void setProsecutionFacts(String prosecutionFacts) {
        this.prosecutionFacts = prosecutionFacts;
    }

    public int getLibraOffenceDateCode() {
        return libraOffenceDateCode;
    }

    public void setLibraOffenceDateCode(int libraOffenceDateCode) {
        this.libraOffenceDateCode = libraOffenceDateCode;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public void setCompensation(BigDecimal compensation) {
        this.compensation = compensation;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public UUID getWithdrawalRequestReasonId() {
        return withdrawalRequestReasonId;
    }

    public void setWithdrawalRequestReasonId(final UUID withdrawalRequestReasonId) {
        this.withdrawalRequestReasonId = withdrawalRequestReasonId;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public void setVehicleMake(String vehicleMake) {
        this.vehicleMake = vehicleMake;
    }

    public String getVehicleRegistrationMark() {
        return vehicleRegistrationMark;
    }

    public void setVehicleRegistrationMark(String vehicleRegistrationMark) {
        this.vehicleRegistrationMark = vehicleRegistrationMark;
    }

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public void setBackDuty(BigDecimal backDuty) {
        this.backDuty = backDuty;
    }

    public LocalDate getBackDutyDateFrom() {
        return backDutyDateFrom;
    }

    public void setBackDutyDateFrom(LocalDate backDutyDateFrom) {
        this.backDutyDateFrom = backDutyDateFrom;
    }

    public LocalDate getBackDutyDateTo() {
        return backDutyDateTo;
    }

    public void setBackDutyDateTo(LocalDate backDutyDateTo) {
        this.backDutyDateTo = backDutyDateTo;
    }

    public VerdictType getConviction() {
        return conviction;
    }

    public void setConviction(final VerdictType conviction) {
        this.conviction = conviction;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }

    public Boolean getEndorsable() {
        return endorsable;
    }

    public void setEndorsable(final Boolean endorsable) {
        this.endorsable = endorsable;
    }

    public Boolean getPressRestrictable() {
        return pressRestrictable;
    }

    public void setPressRestrictable(final Boolean pressRestrictable) {
        this.pressRestrictable = pressRestrictable;
    }

    public PressRestriction getPressRestriction() {
        return pressRestriction;
    }

    public void setPressRestriction(final PressRestriction pressRestriction) {
        this.pressRestriction = pressRestriction;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(final Boolean completed) {
        this.completed = completed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final OffenceDetail that = (OffenceDetail) o;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public int compareTo(OffenceDetail other) {
        return isNull(other) ? 1 : sequenceNumber - other.sequenceNumber;
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class OffenceDetailBuilder {

        private UUID id;
        private String code;
        private String plea;
        private PleaMethod pleaMethod;
        private ZonedDateTime pleaDate;
        private Integer sequenceNumber;
        private String wording;
        private String wordingWelsh;
        private LocalDate startDate;
        private LocalDate chargeDate;
        private String witnessStatement;
        private String prosecutionFacts;
        private int libraOffenceDateCode;
        private BigDecimal compensation;
        private int orderIndex;
        private UUID withdrawalRequestReasonId;
        private BigDecimal backDuty;
        private LocalDate backDutyDateFrom;
        private LocalDate backDutyDateTo;
        private String vehicleMake;
        private String vehicleRegistrationMark;
        private Boolean endorsable;
        private Boolean pressRestrictable;
        private PressRestriction pressRestriction;
        private Boolean completed;

        public OffenceDetail build() {
            return new OffenceDetail(this);
        }

        public OffenceDetailBuilder setId(UUID id) {
            this.id = id;
            return this;
        }

        public OffenceDetailBuilder setCode(String code) {
            this.code = code;
            return this;
        }

        public OffenceDetailBuilder setPlea(PleaType plea) {
            if (plea != null) {
                this.plea = plea.name();
            }

            return this;
        }

        public OffenceDetailBuilder setPleaMethod(PleaMethod pleaMethod) {
            this.pleaMethod = pleaMethod;
            return this;
        }

        public OffenceDetailBuilder setPleaDate(ZonedDateTime pleaDate) {
            this.pleaDate = pleaDate;
            return this;
        }

        public OffenceDetailBuilder setSequenceNumber(Integer sequenceNumber) {
            this.sequenceNumber = sequenceNumber;
            return this;
        }

        public OffenceDetailBuilder setWording(String wording) {
            this.wording = wording;
            return this;
        }

        public OffenceDetailBuilder setWordingWelsh(String wordingWelsh) {
            this.wordingWelsh = wordingWelsh;
            return this;
        }

        public OffenceDetailBuilder setStartDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public OffenceDetailBuilder setChargeDate(LocalDate chargeDate) {
            this.chargeDate = chargeDate;
            return this;
        }

        public OffenceDetailBuilder withWitnessStatement(String witnessStatement) {
            this.witnessStatement = witnessStatement;
            return this;
        }

        public OffenceDetailBuilder withProsecutionFacts(String prosecutionFacts) {
            this.prosecutionFacts = prosecutionFacts;
            return this;
        }

        public OffenceDetailBuilder withLibraOffenceDateCode(int libraOffenceDateCode) {
            this.libraOffenceDateCode = libraOffenceDateCode;
            return this;
        }

        public OffenceDetailBuilder withCompensation(BigDecimal compensation) {
            this.compensation = compensation;
            return this;
        }

        public OffenceDetailBuilder withWithdrawalRequestReason(UUID withdrawalRequestReasonId){
            this.withdrawalRequestReasonId = withdrawalRequestReasonId;
            return this;
        }

        public OffenceDetailBuilder withBackDuty(BigDecimal backDuty){
            this.backDuty = backDuty;
            return this;
        }

        public OffenceDetailBuilder withBackDutyDateFrom(LocalDate backDutyDateFrom){
            this.backDutyDateFrom = backDutyDateFrom;
            return this;
        }

        public OffenceDetailBuilder withBackDutyDateTo(LocalDate backDutyDateTo){
            this.backDutyDateTo = backDutyDateTo;
            return this;
        }

        public OffenceDetailBuilder withVehicleMake(String vehicleMake) {
            this.vehicleMake = vehicleMake;
            return this;
        }

        public OffenceDetailBuilder withVehicleRegistrationMark(String vehicleRegistrationMark) {
            this.vehicleRegistrationMark = vehicleRegistrationMark;
            return this;
        }

        public OffenceDetailBuilder withEndorsable(final Boolean endorsable) {
            this.endorsable = endorsable;
            return this;
        }

        public OffenceDetailBuilder withPressRestrictable(final Boolean pressRestrictable) {
            this.pressRestrictable = pressRestrictable;
            return this;
        }

        public OffenceDetailBuilder withPressRestriction(final PressRestriction pressRestriction) {
            this.pressRestriction = pressRestriction;
            return this;
        }

        public OffenceDetailBuilder withCompleted(final Boolean completed) {
            this.completed = completed;
            return this;
        }
    }

}
