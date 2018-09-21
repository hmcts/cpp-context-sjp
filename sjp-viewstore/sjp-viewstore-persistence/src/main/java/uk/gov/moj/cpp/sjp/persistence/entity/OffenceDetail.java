package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "offence")
public class OffenceDetail implements Serializable {

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

    @Column(name = "pending_withdrawal")
    private Boolean pendingWithdrawal;

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

    @Column(name = "mitigation")
    private String mitigation;

    @Column(name = "not_guilty_because")
    private String notGuiltyBecause;

    public OffenceDetail() {
        super();
    }

    private OffenceDetail(final OffenceDetailBuilder builder) {
        this.id = builder.id;
        this.code = builder.code;
        this.plea = builder.plea;
        this.pleaMethod = builder.pleaMethod;
        this.sequenceNumber = builder.sequenceNumber;
        this.wording = builder.wording;
        this.wordingWelsh = builder.wordingWelsh;
        this.startDate = builder.startDate;
        this.chargeDate = builder.chargeDate;
        this.pendingWithdrawal = builder.pendingWithdrawal;
        this.witnessStatement = builder.witnessStatement;
        this.prosecutionFacts = builder.prosecutionFacts;
        this.libraOffenceDateCode = builder.libraOffenceDateCode;
        this.compensation = builder.compensation;
        this.orderIndex = builder.orderIndex;
        this.mitigation = builder.mitigation;
        this.notGuiltyBecause = builder.notGuiltyBecause;
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

    public Boolean getPendingWithdrawal() {
        return pendingWithdrawal;
    }

    public void setPendingWithdrawal(Boolean pendingWithdrawal) {
        this.pendingWithdrawal = pendingWithdrawal;
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

    public String getMitigation() {
        return mitigation;
    }

    public void setMitigation(final String mitigation) {
        this.mitigation = mitigation;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

    public void setNotGuiltyBecause(final String notGuiltyBecause) {
        this.notGuiltyBecause = notGuiltyBecause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OffenceDetail)) {
            return false;
        }

        OffenceDetail that = (OffenceDetail) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class OffenceDetailBuilder {

        private UUID id;
        private String code;
        private String plea;
        private PleaMethod pleaMethod;
        private Integer sequenceNumber;
        private String wording;
        private String wordingWelsh;
        private LocalDate startDate;
        private LocalDate chargeDate;
        private Boolean pendingWithdrawal;
        private String witnessStatement;
        private String prosecutionFacts;
        private int libraOffenceDateCode;
        private BigDecimal compensation;
        private int orderIndex;
        private String mitigation;
        private String notGuiltyBecause;

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
            if(plea != null) {
                this.plea = plea.name();
            }

            return this;
        }

        public OffenceDetailBuilder setPleaMethod(PleaMethod pleaMethod) {
            this.pleaMethod = pleaMethod;
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

        public OffenceDetailBuilder setPendingWithdrawal(boolean pendingWithdrawal) {
            this.pendingWithdrawal = pendingWithdrawal;
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

        public OffenceDetailBuilder withOrderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public OffenceDetailBuilder withMitigation(String mitigation) {
            this.mitigation = mitigation;
            return this;
        }

        public OffenceDetailBuilder withNotGuiltyBecause(String notGuiltyBecause) {
            this.notGuiltyBecause = notGuiltyBecause;
            return this;
        }
    }

}
