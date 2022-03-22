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

    public CaseOffenceDetails(UUID caseId,
                              String caseRef,
                              LocalDate postingOrHearingDate, List<String> offenceTitles) {
        this.caseId = caseId;
        this.caseRef = caseRef;
        this.postingOrHearingDate = postingOrHearingDate;
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

    public static CaseOffenceDetailsBuilder createBuilder() {
        return new CaseOffenceDetailsBuilder();
    }

    @Override
    public String toString() {
        return "CaseOffenceDetails{" +
                "caseId=" + caseId +
                ", caseRef='" + caseRef + '\'' +
                ", postingOrHearingDate=" + postingOrHearingDate +
                ", offenceTitles=" + offenceTitles +
                '}';
    }

    public static class CaseOffenceDetailsBuilder {

        private UUID caseId;
        private String caseRef;
        private LocalDate postingOrHearingDate;
        private List<String> offenceTitles = new LinkedList<>();

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

        public CaseOffenceDetailsBuilder withOffenceTitles(List<String> offenceTitles) {
            if (nonNull(offenceTitles)) {
                this.offenceTitles.addAll(offenceTitles);
            }

            return this;
        }

        public CaseOffenceDetails build() {
            return new CaseOffenceDetails(caseId, caseRef, postingOrHearingDate, offenceTitles);
        }
    }
}
