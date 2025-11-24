package uk.gov.moj.cpp.sjp.query.view.service;


import java.util.UUID;

public class ReferralReasonNotFoundException extends RuntimeException {

    private final UUID referralReasonId;

    public ReferralReasonNotFoundException(final UUID referralReasonId) {
        super(referralReasonId.toString());
        this.referralReasonId = referralReasonId;
    }

    public UUID getReferralReasonId() {
        return referralReasonId;
    }
}
