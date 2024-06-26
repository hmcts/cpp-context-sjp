package uk.gov.moj.cpp.sjp.query.view.service.defendantcase;

import static java.util.Objects.nonNull;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class CaseOffenceDetails {

    private final UUID caseId;
    private final String caseRef;
    private final LocalDate postingOrHearingDate;
    private final List<String> offenceTitles = new LinkedList<>();
    private final String prosecutorName;
    private final String expiryDate;

    public CaseOffenceDetails(UUID caseId,
                              String caseRef,
                              LocalDate postingOrHearingDate, List<String> offenceTitles,
                              final String prosecutorName, final String expiryDate) {
        this.caseId = caseId;
        this.caseRef = caseRef;
        this.postingOrHearingDate = postingOrHearingDate;
        this.prosecutorName = prosecutorName;
        this.expiryDate = expiryDate;
        if (nonNull(offenceTitles)) {
            this.offenceTitles.addAll(offenceTitles);
        }
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseRef() {
        return caseRef;
    }

    public LocalDate getPostingOrHearingDate() {
        return postingOrHearingDate;
    }

    public List<String> getOffenceTitles() {
        return new LinkedList<>(offenceTitles);
    }

    public String getProsecutorName() {
        return prosecutorName;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public static CaseOffenceDetailsBuilder createBuilder() {
        return new CaseOffenceDetailsBuilder();
    }

    @Override
    public String toString() {
        return "CaseOffenceDetails{" +
                "caseId=" + caseId +
                ", caseRef='" + caseRef + '\'' +
                ", prosecutorName='" + prosecutorName + '\'' +
                ", postingOrHearingDate=" + postingOrHearingDate +
                ", expiryDate=" + expiryDate +
                ", offenceTitles=" + offenceTitles +
                '}';
    }

    public static class CaseOffenceDetailsBuilder {

        private UUID caseId;
        private String caseRef;
        private LocalDate postingOrHearingDate;
        private List<String> offenceTitles = new LinkedList<>();
        private String prosecutorName;
        private String expiryDate;

        public CaseOffenceDetailsBuilder withCaseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public CaseOffenceDetailsBuilder withCaseRef(String caseRef) {
            this.caseRef = caseRef;
            return this;
        }

        public CaseOffenceDetailsBuilder withPostingOrHearingDate(LocalDate postingOrHearingDate) {
            this.postingOrHearingDate = postingOrHearingDate;
            return this;
        }

        public CaseOffenceDetailsBuilder withProsecutorName(String prosecutorName) {
            this.prosecutorName = prosecutorName;
            return this;
        }

        public CaseOffenceDetailsBuilder withExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public CaseOffenceDetailsBuilder withOffenceTitles(List<String> offenceTitles) {
            if (nonNull(offenceTitles)) {
                this.offenceTitles.addAll(offenceTitles);
            }

            return this;
        }

        public CaseOffenceDetails build() {
            return new CaseOffenceDetails(caseId, caseRef, postingOrHearingDate,
                    offenceTitles, prosecutorName, expiryDate);
        }
    }
}
