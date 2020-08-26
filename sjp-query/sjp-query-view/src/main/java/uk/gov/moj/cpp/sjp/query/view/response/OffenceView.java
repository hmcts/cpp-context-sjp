package uk.gov.moj.cpp.sjp.query.view.response;


import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class OffenceView {

    private final UUID id;
    private final String offenceCode;
    private final PleaType plea;
    private final PleaMethod pleaMethod;
    private final ZonedDateTime pleaDate;
    private final Integer offenceSequenceNumber;
    private final String wording;
    private final String wordingWelsh;
    private final String cjsCode;
    private final Integer sequenceNumber;
    private final LocalDate startDate;
    private final LocalDate chargeDate;
    private final BigDecimal backDuty;
    private final LocalDate backDutyDateFrom;
    private final LocalDate backDutyDateTo;
    private final String vehicleMake;
    private final String vehicleRegistrationMark;
    private BigDecimal compensation;
    private String prosecutionFacts;
    private UUID withdrawalRequestReasonId;
    private VerdictType conviction;
    private LocalDate convictionDate;
    private Boolean endorsable;
    private Boolean pressRestrictable;
    private PressRestriction pressRestriction;
    private Boolean completed;
    private Integer offenceDateCode;

    public OffenceView(final OffenceDetail offence) {

        this.id = offence.getId();

        this.offenceCode = offence.getCode();
        this.cjsCode = offenceCode;

        this.offenceSequenceNumber = firstNonNull(offence.getSequenceNumber(), 0);
        this.sequenceNumber = offenceSequenceNumber;

        this.plea = offence.getPlea();
        this.pleaMethod = offence.getPleaMethod();
        this.pleaDate = offence.getPleaDate();
        this.wording = offence.getWording();
        this.wordingWelsh = offence.getWordingWelsh();
        this.startDate = offence.getStartDate();
        this.chargeDate = offence.getChargeDate();
        this.compensation = offence.getCompensation();
        this.prosecutionFacts = offence.getProsecutionFacts();
        this.withdrawalRequestReasonId = offence.getWithdrawalRequestReasonId();
        this.backDuty = offence.getBackDuty();
        this.backDutyDateFrom = offence.getBackDutyDateFrom();
        this.backDutyDateTo = offence.getBackDutyDateTo();
        this.vehicleMake = offence.getVehicleMake();
        this.vehicleRegistrationMark = offence.getVehicleRegistrationMark();
        this.conviction = offence.getConviction();
        this.convictionDate = offence.getConvictionDate();
        this.endorsable = offence.getEndorsable();
        this.pressRestrictable = offence.getPressRestrictable();
        this.pressRestriction = mapPressRestriction(offence.getPressRestriction());
        this.completed = offence.getCompleted();
        this.offenceDateCode = offence.getLibraOffenceDateCode();
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

    public ZonedDateTime getPleaDate() { return pleaDate; }

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

    public BigDecimal getCompensation() {
        return compensation;
    }

    public String getProsecutionFacts() {
        return prosecutionFacts;
    }

    public UUID getWithdrawalRequestReasonId() {
        return withdrawalRequestReasonId;
    }

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public LocalDate getBackDutyDateFrom() {
        return backDutyDateFrom;
    }

    public LocalDate getBackDutyDateTo() {
        return backDutyDateTo;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public String getVehicleRegistrationMark() {
        return vehicleRegistrationMark;
    }

    public VerdictType getConviction() {
        return conviction;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public Boolean getEndorsable() {
        return endorsable;
    }

    public Boolean getPressRestrictable() { return pressRestrictable; }

    public PressRestriction getPressRestriction() {
        return pressRestriction;
    }

    public Boolean getCompleted() {
        return completed;
    }

    private PressRestriction mapPressRestriction(final uk.gov.moj.cpp.sjp.persistence.entity.PressRestriction pressRestriction) {
        if (Objects.isNull(pressRestriction)) {
            return null;
        }
        return pressRestriction.getRequested() ?
                PressRestriction.requested(pressRestriction.getName()) :
                PressRestriction.revoked();

    }

    public Integer getOffenceDateCode() {
        return offenceDateCode;
    }
}
