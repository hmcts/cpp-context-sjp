package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(value = { "crownCourt",
                                "applicationTypeName",
                                "magistrateCourt",
                                "sjpNoticeServed", "caseType", "parties", "applications"})
public class DefendantCase {

    private UUID caseId;
    private String caseReference;
    private boolean sjp;
    private String caseStatus;
    private List<Hearing> hearings = new LinkedList<>();

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(final String caseReference) {
        this.caseReference = caseReference;
    }

    public boolean isSjp() {
        return sjp;
    }

    public void setSjp(final boolean sjp) {
        this.sjp = sjp;
    }

    public List<Hearing> getHearings() {
        return new LinkedList<>(hearings);
    }

    public void setHearings(final List<Hearing> hearings) {
        this.hearings.clear();
        this.hearings.addAll(hearings);
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(final String caseStatus) {
        this.caseStatus = caseStatus;
    }

    @Override
    public String toString() {
        return "DefendantCase{" +
                "caseId=" + caseId +
                ", caseReference='" + caseReference + '\'' +
                ", sjp=" + sjp +
                ", hearings=" + hearings +
                ", caseStatus='" + caseStatus + '\'' +
                '}';
    }

    @JsonIgnoreProperties(value = { "courtId",
                                    "courtCentreName",
                                    "hearingTypeId",
                                    "hearingTypeLabel",
                                    "isBoxHearing",
                                    "isVirtualBoxHearing",
                                    "hearingDay",
                                    "jurisdictionType", "judiciaryTypes", "assignedTo"})
    public static class Hearing {

        private String hearingId;
        private List<String> hearingDates = new LinkedList<>();

        public String getHearingId() {
            return hearingId;
        }

        public void setHearingId(final String hearingId) {
            this.hearingId = hearingId;
        }

        public List<String> getHearingDates() {
            return new LinkedList<>(hearingDates);
        }

        public void setHearingDates(final List<String> hearingDates) {
            this.hearingDates.clear();
            this.hearingDates.addAll(hearingDates);
        }

        @Override
        public String toString() {
            return "Hearing{" +
                    "hearingId='" + hearingId + '\'' +
                    ", hearingDates=" + hearingDates +
                    '}';
        }
    }
}

