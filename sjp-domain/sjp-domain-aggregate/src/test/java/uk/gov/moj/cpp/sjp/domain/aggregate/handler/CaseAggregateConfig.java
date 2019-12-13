package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import java.time.LocalDate;

public class CaseAggregateConfig {
    private LocalDate postingDateExpirationDate;
    private LocalDate adjournedTo;
    private LocalDate datesToAvoidExpirationDate;
    private boolean isNotGuiltyPleaPresent;
    private boolean isGuiltyPleaPresent;

    private CaseAggregateConfig(final LocalDate postingDateExpirationDate, final LocalDate adjournedTo,
                                final LocalDate datesToAvoidExpirationDate,
                                final boolean isNotGuiltyPleaPresent, final boolean isGuiltyPleaPresent) {
        this.postingDateExpirationDate = postingDateExpirationDate;
        this.adjournedTo = adjournedTo;
        this.datesToAvoidExpirationDate = datesToAvoidExpirationDate;
        this.isNotGuiltyPleaPresent = isNotGuiltyPleaPresent;
        this.isGuiltyPleaPresent = isGuiltyPleaPresent;
    }

    LocalDate getAdjournedTo() {
        return adjournedTo;
    }

    LocalDate getDatesToAvoidExpirationDate() {
        return datesToAvoidExpirationDate;
    }

    LocalDate getPostingDateExpirationDate() {
        return postingDateExpirationDate;
    }

    boolean isNotGuiltyPleaPresent() {
        return isNotGuiltyPleaPresent;
    }

    public boolean isGuiltyPleaPresent() {
        return isGuiltyPleaPresent;
    }

    static class Builder {
        private LocalDate postingDateExpirationDate;
        private LocalDate adjournedTo;
        private LocalDate datesToAvoidExpirationDate;
        private boolean isNotGuiltyPleaPresent = false;
        private boolean isGuiltyPleaPresent = false;

        static Builder caseAggregateConfigBuilder() {
            return new Builder();
        }

        Builder withPostingDateExpirationDate(final LocalDate postingDate) {
            this.postingDateExpirationDate = postingDate;
            return this;
        }


        Builder withAdjournedTo(final LocalDate adjournedTo) {
            this.adjournedTo = adjournedTo;
            return this;
        }

        Builder withDatesToAvoidExpirationDate(final LocalDate datesToAvoidExpirationDate) {
            this.datesToAvoidExpirationDate = datesToAvoidExpirationDate;
            return this;
        }

        Builder withNotGuiltyPlea() {
            this.isNotGuiltyPleaPresent = true;
            return this;
        }

        Builder withGuiltyPlea() {
            this.isGuiltyPleaPresent = true;
            return this;
        }

        public CaseAggregateConfig build() {
            return new CaseAggregateConfig(postingDateExpirationDate, adjournedTo, datesToAvoidExpirationDate, isNotGuiltyPleaPresent, isGuiltyPleaPresent);
        }
    }

    @Override
    public String toString() {
        return "CaseAggregateConfig{" +
                "postingDateExpirationDate=" + postingDateExpirationDate +
                ", adjournedTo=" + adjournedTo +
                ", datesToAvoidExpirationDate=" + datesToAvoidExpirationDate +
                ", isNotGuiltyPleaPresent=" + isNotGuiltyPleaPresent +
                ", isGuiltyPleaPresent=" + isGuiltyPleaPresent +
                '}';
    }
}
