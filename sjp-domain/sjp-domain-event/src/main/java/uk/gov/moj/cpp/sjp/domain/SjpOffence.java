package uk.gov.moj.cpp.sjp.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SjpOffence {

    private UUID id;
    private String prosecutorCaseId;
    private int offenceSequenceNo;
    private String libraOffenceCode;
    private LocalDate chargeDate;
    private int libraOffenceDateCode;
    private LocalDate offenceDate;
    private String offenceWording;
    private String prosecutionFacts;
    private String witnessStatement;
    private BigDecimal compensation;

    public SjpOffence(UUID id,
                      String prosecutorCaseId,
                      int offenceSequenceNo,
                      String libraOffenceCode,
                      LocalDate chargeDate,
                      int libraOffenceDateCode,
                      LocalDate offenceDate,
                      String offenceWording,
                      String prosecutionFacts,
                      String witnessStatement,
                      BigDecimal compensation) {
        this.id = id;
        this.prosecutorCaseId = prosecutorCaseId;
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

    public SjpOffence() {
        //default constructor
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProsecutorCaseId() {
        return prosecutorCaseId;
    }

    public void setProsecutorCaseId(String prosecutorCaseId) {
        this.prosecutorCaseId = prosecutorCaseId;
    }

    public int getOffenceSequenceNo() {
        return offenceSequenceNo;
    }

    public void setOffenceSequenceNo(int offenceSequenceNo) {
        this.offenceSequenceNo = offenceSequenceNo;
    }

    public String getLibraOffenceCode() {
        return libraOffenceCode;
    }

    public void setLibraOffenceCode(String libraOffenceCode) {
        this.libraOffenceCode = libraOffenceCode;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }

    public LocalDate getOffenceDate() {
        return offenceDate;
    }

    public void setOffenceDate(LocalDate offenceDate) {
        this.offenceDate = offenceDate;
    }

    public int getLibraOffenceDateCode() {
        return libraOffenceDateCode;
    }

    public void setLibraOffenceDateCode(int libraOffenceDateCode) {
        this.libraOffenceDateCode = libraOffenceDateCode;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public void setOffenceWording(String offenceWording) {
        this.offenceWording = offenceWording;
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

    public void setCompensation(BigDecimal compensation) {
        this.compensation = compensation;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
