package uk.gov.moj.cpp.sjp.model.prosecution;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OffenceView {

    private static final int DEFAULT_SJP_OFFENCE_VIEW_COUNT = 1;

    private final UUID id;
    private final UUID offenceDefinitionId;
    private final String wording;
    private final String wordingWelsh;
    private final LocalDate startDate;
    private final LocalDate chargeDate;
    private final LocalDate convictionDate;
    private final LocalDate endDate;
    private final Integer orderIndex;
    private final NotifiedPleaView notifiedPlea;
    private final OffenceFactsView offenceFacts;
    private final Integer offenceDateCode;
    private final List<ReportingRestrictionView> reportingRestrictions;
    private final String offenceLegislation;
    private final String offenceTitle;
    private final String offenceCode;
    private final String maxPenalty;

    @SuppressWarnings("squid:S00107")
    public OffenceView(final UUID id,
                       final UUID offenceDefinitionId,
                       final String wording,
                       final String wordingWelsh,
                       final LocalDate startDate,
                       final LocalDate chargeDate,
                       final LocalDate convictionDate,
                       final LocalDate endDate,
                       final Integer orderIndex,
                       final NotifiedPleaView notifiedPlea,
                       final OffenceFactsView offenceFacts,
                       final Integer offenceDateCode,
                       final List<ReportingRestrictionView> reportingRestrictions,
                       final String offenceLegislation,
                       final String offenceTitle,
                       final String offenceCode,
                       final String maxPenalty) {

        this.id = id;
        this.offenceDefinitionId = offenceDefinitionId;
        this.wording = wording;
        this.wordingWelsh = wordingWelsh;
        this.startDate = startDate;
        this.chargeDate = chargeDate;
        this.convictionDate = convictionDate;
        this.endDate = endDate;
        this.orderIndex = orderIndex;
        this.notifiedPlea = notifiedPlea;
        this.offenceFacts = offenceFacts;
        this.offenceDateCode = offenceDateCode;
        this.reportingRestrictions = ofNullable(reportingRestrictions)
                .filter(reportingRes -> !reportingRes.isEmpty())
                .map(ArrayList::new)
                .orElse(null);
        this.maxPenalty = maxPenalty;
        this.offenceLegislation = offenceLegislation;
        this.offenceTitle = offenceTitle;
        this.offenceCode = offenceCode;
    }


    public UUID getId() {
        return id;
    }

    public UUID getOffenceDefinitionId() {
        return offenceDefinitionId;
    }

    public String getWording() {
        return wording;
    }

    public String getWordingWelsh() {
        return wordingWelsh;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public int getCount() {
        return DEFAULT_SJP_OFFENCE_VIEW_COUNT;
    }

    public NotifiedPleaView getNotifiedPlea() {
        return notifiedPlea;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public OffenceFactsView getOffenceFacts() {
        return offenceFacts;
    }

    public List<ReportingRestrictionView> getReportingRestrictions() {
        return reportingRestrictions != null ? unmodifiableList(reportingRestrictions) : null;
    }

    public String getMaxPenalty() {
        return maxPenalty;
    }

    public static Builder builder() {
        return new Builder();
    }


    public Integer getOffenceDateCode() {
        return offenceDateCode;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getOffenceLegislation() {
        return offenceLegislation;
    }

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public static class Builder {
        private UUID id;
        private UUID offenceDefinitionId;
        private String wording;
        private String wordingWelsh;
        private LocalDate startDate;
        private LocalDate chargeDate;
        private LocalDate convictionDate;
        private LocalDate endDate;
        private Integer orderIndex;
        private NotifiedPleaView notifiedPlea;
        private OffenceFactsView offenceFacts;
        private Integer offenceDateCode;
        private List<ReportingRestrictionView> reportingRestrictions;
        private String offenceLegislation;
        private String offenceTitle;
        private String offenceCode;
        private String maxPenalty;

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withOffenceDefinitionId(final UUID offenceDefinitionId) {
            this.offenceDefinitionId = offenceDefinitionId;
            return this;
        }

        public Builder withWording(final String wording) {
            this.wording = wording;
            return this;
        }

        public Builder withWordingWelsh(final String wordingWelsh) {
            this.wordingWelsh = wordingWelsh;
            return this;
        }

        public Builder withStartDate(final LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withChargeDate(final LocalDate chargeDate) {
            this.chargeDate = chargeDate;
            return this;
        }

        public Builder withConvictionDate(final LocalDate convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }

        public Builder withEndDate(final LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public Builder withOrderIndex(final Integer orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public Builder withNotifiedPlea(final NotifiedPleaView notifiedPlea) {
            this.notifiedPlea = notifiedPlea;
            return this;
        }

        public Builder withOffenceFacts(final OffenceFactsView offenceFacts) {
            this.offenceFacts = offenceFacts;
            return this;
        }

        public Builder withOffenceDateCode(final Integer offenceDateCode) {
            this.offenceDateCode = offenceDateCode;
            return this;
        }

        public Builder withReportingRestrictions(final List<ReportingRestrictionView> reportingRestrictions) {
            this.reportingRestrictions = ofNullable(reportingRestrictions)
                    .filter(reportingRes -> !reportingRes.isEmpty())
                    .map(ArrayList::new)
                    .orElse(null);
            return this;
        }

        public Builder withOffenceLegislation(final String offenceLegislation) {
            this.offenceLegislation = offenceLegislation;
            return this;
        }

        public Builder withOffenceTitle(final String offenceTitle) {
            this.offenceTitle = offenceTitle;
            return this;
        }

        public Builder withOffenceCode(final String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Builder withMaxPenalty(final String maxPenalty) {
            this.maxPenalty = maxPenalty;
            return this;
        }

        public OffenceView build() {
            return new OffenceView(
                    id,
                    offenceDefinitionId,
                    wording,
                    wordingWelsh,
                    startDate,
                    chargeDate,
                    convictionDate,
                    endDate,
                    orderIndex,
                    notifiedPlea,
                    offenceFacts,
                    offenceDateCode,
                    reportingRestrictions,
                    offenceLegislation,
                    offenceTitle,
                    offenceCode,
                    maxPenalty);
        }
    }
}
