package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CaseSearchResultsView {

    private final boolean foundCasesWithOutdatedDefendantsName;

    private final List<CaseSearchResultView> results;

    public CaseSearchResultsView(List<CaseSearchResult> results) {
        this.foundCasesWithOutdatedDefendantsName = results.stream().anyMatch(
                CaseSearchResult::isDeprecated
        );
        this.results = results.stream()
                .map(CaseSearchResultView::new)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
    }

    public boolean isFoundCasesWithOutdatedDefendantsName() {
        return foundCasesWithOutdatedDefendantsName;
    }

    public List<CaseSearchResultView> getResults() {
        return results;
    }

    public static class CaseSearchResultView {

        private final UUID caseId;
        private final String urn;
        private final boolean assigned;
        private final boolean completed;
        private final String enterpriseId;
        private final String prosecutingAuthority;
        private final LocalDate postingDate;
        private final LocalDate reopenedDate;
        private final LocalDate pleaDate;
        private final LocalDate withdrawalRequestedDate;
        private final CaseSearchResultDefendantView defendant;

        public CaseSearchResultView(CaseSearchResult caseSearchResult) {
            this.caseId = caseSearchResult.getCaseId();
            this.urn = caseSearchResult.getCaseSummary().getUrn();
            this.assigned = caseSearchResult.getAssigned();
            this.completed = caseSearchResult.getCaseSummary().isCompleted();
            this.enterpriseId = caseSearchResult.getCaseSummary().getEnterpriseId();
            this.prosecutingAuthority = caseSearchResult.getCaseSummary().getProsecutingAuthority();
            this.postingDate = caseSearchResult.getCaseSummary().getPostingDate();
            this.reopenedDate = caseSearchResult.getCaseSummary().getReopenedDate();
            this.pleaDate = caseSearchResult.getPleaDate();
            this.withdrawalRequestedDate = caseSearchResult.getWithdrawalRequestedDate();
            this.defendant = new CaseSearchResultDefendantView(caseSearchResult);
        }

        public UUID getCaseId() {
            return caseId;
        }

        public String getUrn() {
            return urn;
        }

        public boolean isAssigned() {
            return assigned;
        }

        public boolean isCompleted() {
            return completed;
        }

        public String getEnterpriseId() {
            return enterpriseId;
        }

        public String getProsecutingAuthority() {
            return prosecutingAuthority;
        }

        public LocalDate getPostingDate() {
            return postingDate;
        }

        public LocalDate getReopenedDate() {
            return reopenedDate;
        }

        public LocalDate getPleaDate() {
            return pleaDate;
        }

        public LocalDate getWithdrawalRequestedDate() {
            return withdrawalRequestedDate;
        }

        public CaseSearchResultDefendantView getDefendant() {
            return defendant;
        }

        public static class CaseSearchResultDefendantView {
            private final String firstName;
            private final String lastName;
            private final LocalDate dateOfBirth;
            private final boolean outdated;

            public CaseSearchResultDefendantView(CaseSearchResult caseSearchResult) {
                this.firstName = caseSearchResult.getCurrentFirstName();
                this.lastName = caseSearchResult.getCurrentLastName();
                this.dateOfBirth = caseSearchResult.getDateOfBirth();
                this.outdated = caseSearchResult.isDeprecated();
            }

            public String getFirstName() {
                return firstName;
            }

            public String getLastName() {
                return lastName;
            }

            public LocalDate getDateOfBirth() {
                return dateOfBirth;
            }

            public boolean isOutdated() {
                return outdated;
            }
        }
    }
}
