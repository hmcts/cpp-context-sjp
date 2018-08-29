package uk.gov.moj.cpp.sjp.query.view.response;


import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class OffenceView {

    private final UUID id;
    private final String offenceCode;
    private final PleaType plea;
    private final PleaMethod pleaMethod;
    private final Integer offenceSequenceNumber;
    private final String wording;
    private final String wordingWelsh;
    private final String cjsCode;
    private final Integer sequenceNumber;
    private final LocalDate startDate;
    private final LocalDate chargeDate;
    private final Boolean pendingWithdrawal;
    private BigDecimal compensation;
    private String prosecutionFacts;

    public OffenceView(final OffenceDetail offence) {

        this.id = offence.getId();

        this.offenceCode = offence.getCode();
        this.cjsCode = offenceCode;

        this.offenceSequenceNumber = firstNonNull(offence.getSequenceNumber(), 0);
        this.sequenceNumber = offenceSequenceNumber;

        this.plea = offence.getPlea();
        this.pleaMethod = offence.getPleaMethod();
        this.wording = offence.getWording();
        this.wordingWelsh = offence.getWordingWelsh();
        this.startDate = offence.getStartDate();
        this.chargeDate = offence.getChargeDate();
        this.pendingWithdrawal = offence.getPendingWithdrawal();
        this.compensation = offence.getCompensation();
        this.prosecutionFacts = offence.getProsecutionFacts();
    }

    public UUID getId() {
        return id;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public PleaType getPlea() {
        return plea;
    }

    public PleaMethod getPleaMethod() {
        return pleaMethod;
    }

    public Integer getOffenceSequenceNumber() {
        return offenceSequenceNumber;
    }

    public String getWording() {
        return wording;
    }

    public String getWordingWelsh() {
        return wordingWelsh;
    }

    public String getCjsCode() {
        return cjsCode;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public Boolean getPendingWithdrawal() {
        return pendingWithdrawal;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public String getProsecutionFacts() {
        return prosecutionFacts;
    }
}
