package uk.gov.moj.cpp.sjp.domain.aggregate.state;

import java.util.UUID;

public class WithdrawalRequestsStatus implements AggregateState {

    private UUID offenceId;

    private UUID withdrawalRequestReasonId;

    public WithdrawalRequestsStatus(final UUID offenceId, final UUID withdrawalRequestReasonId) {
        this.offenceId = offenceId;
        this.withdrawalRequestReasonId = withdrawalRequestReasonId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public void setOffenceId(UUID offenceId) {
        this.offenceId = offenceId;
    }

    public UUID getWithdrawalRequestReasonId() {
        return withdrawalRequestReasonId;
    }

    public void setWithdrawalRequestReasonId(UUID withdrawalRequestReasonId) {
        this.withdrawalRequestReasonId = withdrawalRequestReasonId;
    }

    public static uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus.Builder withdrawalRequestsStatus() {
        return new uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus.Builder();
    }

    public static class Builder {
        private UUID offenceId;

        private UUID withdrawalRequestReasonId;

        public uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus.Builder with(final uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus withdrawalRequestsStatus) {
            this.offenceId = withdrawalRequestsStatus.getOffenceId();
            this.withdrawalRequestReasonId = withdrawalRequestsStatus.getWithdrawalRequestReasonId();
            return this;
        }

        public uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus.Builder withOffenceId(final UUID offenceId) {
            this.offenceId = offenceId;
            return this;
        }

        public uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus.Builder withWithdrawalRequestReasonId(final UUID withdrawalRequestReasonId) {
            this.withdrawalRequestReasonId = withdrawalRequestReasonId;
            return this;
        }

        public uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus build() {
            return new uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId);
        }

    }

    @Override
    public String toString() {
        return "WithdrawalRequestsStatus{" +
                "offenceId='" + offenceId + "'," +
                "withdrawalRequestReasonId='" + withdrawalRequestReasonId + "'" +
                "}";
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus that = (uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus) obj;

        return java.util.Objects.equals(this.offenceId, that.offenceId) &&
                java.util.Objects.equals(this.withdrawalRequestReasonId, that.withdrawalRequestReasonId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(offenceId, withdrawalRequestReasonId);
    }

}
