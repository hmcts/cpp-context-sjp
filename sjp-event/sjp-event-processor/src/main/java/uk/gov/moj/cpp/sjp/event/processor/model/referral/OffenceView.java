package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.time.LocalDate;
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
                       final NotifiedPleaView notifiedPlea) {

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


    public static Builder builder() {
        return new Builder();
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
                    notifiedPlea);
        }
    }
}
