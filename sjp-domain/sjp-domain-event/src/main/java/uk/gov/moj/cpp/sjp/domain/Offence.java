package uk.gov.moj.cpp.sjp.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Offence {

    private final UUID id;
    private final int offenceSequenceNo;
    private final String libraOffenceCode;
    private final LocalDate chargeDate;
    private final int libraOffenceDateCode;
    private final LocalDate offenceDate;
    private final String offenceWording;
    private final String prosecutionFacts;
    private final String witnessStatement;
    private final BigDecimal compensation;

    @JsonCreator
    public Offence(@JsonProperty("id") UUID id,
                   @JsonProperty("offenceSequenceNo") int offenceSequenceNo,
                   @JsonProperty("libraOffenceCode") String libraOffenceCode,
                   @JsonProperty("chargeDate") LocalDate chargeDate,
                   @JsonProperty("libraOffenceDateCode") int libraOffenceDateCode,
                   @JsonProperty("offenceDate") LocalDate offenceDate,
                   @JsonProperty("offenceWording") String offenceWording,
                   @JsonProperty("prosecutionFacts") String prosecutionFacts,
                   @JsonProperty("witnessStatement") String witnessStatement,
                   @JsonProperty("compensation") BigDecimal compensation) {
        this.id = id;
        this.offenceSequenceNo = offenceSequenceNo;
        this.libraOffenceCode = libraOffenceCode;
        this.chargeDate = chargeDate;
        this.libraOffenceDateCode = libraOffenceDateCode;
        this.offenceDate = offenceDate;
        this.offenceWording = offenceWording;
        this.prosecutionFacts = prosecutionFacts;
        this.witnessStatement = witnessStatement;
        this.compensation = compensation;
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

    public LocalDate getOffenceDate() {
        return offenceDate;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Offence that = (Offence) o;

        return offenceSequenceNo == that.offenceSequenceNo &&
                libraOffenceDateCode == that.libraOffenceDateCode &&
                Objects.equals(id, that.id) &&
                Objects.equals(libraOffenceCode, that.libraOffenceCode) &&
                Objects.equals(chargeDate, that.chargeDate) &&
                Objects.equals(offenceDate, that.offenceDate) &&
                Objects.equals(offenceWording, that.offenceWording) &&
                Objects.equals(prosecutionFacts, that.prosecutionFacts) &&
                Objects.equals(witnessStatement, that.witnessStatement) &&
                Objects.equals(compensation, that.compensation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, offenceSequenceNo, libraOffenceCode, chargeDate, libraOffenceDateCode, offenceDate, offenceWording, prosecutionFacts, witnessStatement, compensation);
    }
}
