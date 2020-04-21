package uk.gov.moj.cpp.sjp.domain;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Offence {

    private final UUID id;
    private final int offenceSequenceNo;
    private final String libraOffenceCode;
    private final LocalDate chargeDate;
    private final int libraOffenceDateCode;
    private final LocalDate offenceCommittedDate;
    private final String offenceWording;
    private final String prosecutionFacts;
    private final String witnessStatement;
    private final BigDecimal compensation;
    private final String offenceWordingWelsh;

    private final BigDecimal backDuty;
    private final LocalDate backDutyDateFrom;
    private final LocalDate backDutyDateTo;

    private final String vehicleMake;
    private final String vehicleRegistrationMark;

    private final Boolean endorsable;

    @SuppressWarnings("squid:S00107")
    public Offence(UUID id, int offenceSequenceNo, String libraOffenceCode, LocalDate chargeDate,
                   int libraOffenceDateCode, LocalDate offenceCommittedDate, String offenceWording,
                   String prosecutionFacts, String witnessStatement, BigDecimal compensation) {
        this(id, offenceSequenceNo, libraOffenceCode, chargeDate, libraOffenceDateCode, null, offenceCommittedDate,
                offenceWording, prosecutionFacts, witnessStatement, compensation,
                null, null, null, null, null, null, null);
    }

    @JsonCreator
    public Offence(@JsonProperty("id") UUID id,
                   @JsonProperty("offenceSequenceNo") int offenceSequenceNo,
                   @JsonProperty("libraOffenceCode") String libraOffenceCode,
                   @JsonProperty("chargeDate") LocalDate chargeDate,
                   @JsonProperty("libraOffenceDateCode") int libraOffenceDateCode,
                   @JsonProperty("offenceDate") LocalDate offenceDate, // Backward compatibility
                   @JsonProperty("offenceCommittedDate") LocalDate offenceCommittedDate,
                   @JsonProperty("offenceWording") String offenceWording,
                   @JsonProperty("prosecutionFacts") String prosecutionFacts,
                   @JsonProperty("witnessStatement") String witnessStatement,
                   @JsonProperty("compensation") BigDecimal compensation,
                   @JsonProperty("offenceWordingWelsh") String offenceWordingWelsh,
                   @JsonProperty("backDuty") BigDecimal backDuty,
                   @JsonProperty("backDutyDateFrom") LocalDate backDutyDateFrom,
                   @JsonProperty("backDutyDateTo") LocalDate backDutyDateTo,
                   @JsonProperty("vehicleMake") String vehicleMake,
                   @JsonProperty("vehicleRegistrationMark") String vehicleRegistrationMark,
                   @JsonProperty("endorsable") Boolean endorsable) {
        this.id = id;
        this.offenceSequenceNo = offenceSequenceNo;
        this.libraOffenceCode = libraOffenceCode;
        this.chargeDate = chargeDate;
        this.libraOffenceDateCode = libraOffenceDateCode;
        this.offenceCommittedDate = firstNonNull(offenceCommittedDate, offenceDate);
        this.offenceWording = offenceWording;
        this.prosecutionFacts = prosecutionFacts;
        this.witnessStatement = witnessStatement;
        this.compensation = compensation;
        this.offenceWordingWelsh = offenceWordingWelsh;
        this.backDuty = backDuty;
        this.backDutyDateFrom = backDutyDateFrom;
        this.backDutyDateTo = backDutyDateTo;
        this.vehicleMake = vehicleMake;
        this.vehicleRegistrationMark = vehicleRegistrationMark;
        this.endorsable = endorsable;
    }

    public UUID getId() {
        return id;
    }

    public int getOffenceSequenceNo() {
        return offenceSequenceNo;
    }

    public String getLibraOffenceCode() {
        return libraOffenceCode;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public LocalDate getOffenceCommittedDate() {
        return offenceCommittedDate;
    }

    public int getLibraOffenceDateCode() {
        return libraOffenceDateCode;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public String getProsecutionFacts() {
        return prosecutionFacts;
    }

    public String getWitnessStatement() {
        return witnessStatement;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public String getOffenceWordingWelsh() {
        return offenceWordingWelsh;
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

    public Boolean getEndorsable() {
        return endorsable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Offence that = (Offence) o;

        return offenceSequenceNo == that.offenceSequenceNo &&
                libraOffenceDateCode == that.libraOffenceDateCode &&
                Objects.equals(id, that.id) &&
                Objects.equals(libraOffenceCode, that.libraOffenceCode) &&
                Objects.equals(chargeDate, that.chargeDate) &&
                Objects.equals(offenceCommittedDate, that.offenceCommittedDate) &&
                Objects.equals(offenceWording, that.offenceWording) &&
                Objects.equals(prosecutionFacts, that.prosecutionFacts) &&
                Objects.equals(witnessStatement, that.witnessStatement) &&
                Objects.equals(compensation, that.compensation) &&
                Objects.equals(offenceWordingWelsh, that.offenceWordingWelsh) &&
                Objects.equals(backDuty, that.backDuty) &&
                Objects.equals(backDutyDateFrom, that.backDutyDateFrom) &&
                Objects.equals(backDutyDateTo, that.backDutyDateTo) &&
                Objects.equals(vehicleMake, that.vehicleMake) &&
                Objects.equals(vehicleRegistrationMark, that.vehicleRegistrationMark) &&
                Objects.equals(endorsable, that.endorsable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, offenceSequenceNo, libraOffenceCode, chargeDate,
                libraOffenceDateCode, offenceCommittedDate, offenceWording, prosecutionFacts,
                witnessStatement, compensation, offenceWordingWelsh, backDuty, backDutyDateFrom,
                backDutyDateTo, vehicleMake, vehicleRegistrationMark, endorsable);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
