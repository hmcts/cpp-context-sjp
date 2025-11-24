package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;

import java.time.LocalDate;

public class CaseAggregateConfig {
    private LocalDate postingDateExpirationDate;
    private LocalDate adjournedTo;
    private LocalDate datesToAvoidExpirationDate;
    private boolean isNotGuiltyPleaPresent;
    private boolean isGuiltyPleaPresent;
    private Application application;
    private final Boolean isCaseReserved;
    private final CaseReadinessReason caseReadinessReason;

    private CaseAggregateConfig(final LocalDate postingDateExpirationDate, final LocalDate adjournedTo,
                                final LocalDate datesToAvoidExpirationDate,
                                final boolean isNotGuiltyPleaPresent, final boolean isGuiltyPleaPresent,
                                final Application application, final Boolean isCaseReserved, final CaseReadinessReason caseReadinessReason) {
        this.postingDateExpirationDate = postingDateExpirationDate;
        this.adjournedTo = adjournedTo;
        this.datesToAvoidExpirationDate = datesToAvoidExpirationDate;
        this.isNotGuiltyPleaPresent = isNotGuiltyPleaPresent;
        this.isGuiltyPleaPresent = isGuiltyPleaPresent;
        this.application = application;
        this.isCaseReserved = isCaseReserved;
        this.caseReadinessReason = caseReadinessReason;
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

    public Application getApplication() {
        return application;
    }

    public Boolean getCaseReserved() {
        return isCaseReserved;
    }
    public CaseReadinessReason getCaseReadinessReason() {
        return caseReadinessReason;
    }

    static class Builder {
        private LocalDate postingDateExpirationDate;
        private LocalDate adjournedTo;
        private LocalDate datesToAvoidExpirationDate;
        private boolean isNotGuiltyPleaPresent = false;
        private boolean isGuiltyPleaPresent = false;
        private Application application;
        private boolean isCaseReserved;
        private CaseReadinessReason caseReadinessReason = null;

        Builder withCaseReadinessReason(final CaseReadinessReason caseReadinessReason) {
            this.caseReadinessReason = caseReadinessReason;
            return this;
        }

        Builder withCaseReserved(final Boolean caseReserved) {
            this.isCaseReserved = caseReserved;
            return this;
        }

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

        Builder withApplication(final Application application) {
            this.application = application;
            return this;
        }

        public CaseAggregateConfig build() {
            return new CaseAggregateConfig(
                    postingDateExpirationDate, adjournedTo,
                    datesToAvoidExpirationDate, isNotGuiltyPleaPresent,
                    isGuiltyPleaPresent,
                    application,
                    isCaseReserved,
                    caseReadinessReason
            );
        }
    }

    static class ApplicationBuilder {

        private Application application = new Application(null);

        public static ApplicationBuilder application(final ApplicationType type) {
            final ApplicationBuilder builder = new ApplicationBuilder();
            builder.withType(type);
            return builder;
        }

        public ApplicationBuilder withType(final ApplicationType type) {
            this.application.setType(type);
            return this;
        }

        public ApplicationBuilder withApplicationStatus(final ApplicationStatus status) {
            this.application.setStatus(status);
            return this;
        }

        public Application build() {
            return application;
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
