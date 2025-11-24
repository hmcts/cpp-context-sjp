package uk.gov.moj.cpp.sjp.query.controller.response;

import java.math.BigDecimal;

@SuppressWarnings("squid:S00107")
public class OutstandingFine {
    private String defendantName;
    private String dateOfBirth;
    private String accountNumber;
    private String address;
    private BigDecimal outstandingBalance;
    private Boolean isCollectionOrderMade;
    private String paymentRate;
    private BigDecimal amountImposed;
    private BigDecimal amountPaid;
    private Integer defaultDays;
    private Boolean isConsolidated;
    private String accountLocation;
    private Boolean parentGuardianToPay;

    public OutstandingFine(String defendantName, String dateOfBirth, String accountNumber, String address,
                           BigDecimal outstandingBalance, Boolean isCollectionOrderMade, String paymentRate,
                           BigDecimal amountImposed, BigDecimal amountPaid, Integer defaultDays, Boolean isConsolidated,
                           String accountLocation, Boolean parentGuardianToPay) {
        this.defendantName = defendantName;
        this.dateOfBirth = dateOfBirth;
        this.accountNumber = accountNumber;
        this.address = address;
        this.outstandingBalance = outstandingBalance;
        this.isCollectionOrderMade = isCollectionOrderMade;
        this.paymentRate = paymentRate;
        this.amountImposed = amountImposed;
        this.amountPaid = amountPaid;
        this.defaultDays = defaultDays;
        this.isConsolidated = isConsolidated;
        this.accountLocation = accountLocation;
        this.parentGuardianToPay = parentGuardianToPay;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAddress() {
        return address;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public Boolean getCollectionOrderMade() {
        return isCollectionOrderMade;
    }

    public String getPaymentRate() {
        return paymentRate;
    }

    public BigDecimal getAmountImposed() {
        return amountImposed;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public Integer getDefaultDays() {
        return defaultDays;
    }

    public Boolean getConsolidated() {
        return isConsolidated;
    }

    public String getAccountLocation() {
        return accountLocation;
    }

    public Boolean getParentGuardianToPay() {
        return parentGuardianToPay;
    }
}