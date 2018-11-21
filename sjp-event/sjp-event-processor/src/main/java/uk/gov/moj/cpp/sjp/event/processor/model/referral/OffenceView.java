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
                       final Integer orderIndex,
                       final NotifiedPleaView notifiedPlea) {

        this.id = id;
        this.offenceDefinitionId = offenceDefinitionId;
        this.wording = wording;
        this.wordingWelsh = wordingWelsh;
        this.startDate = startDate;
        this.chargeDate = chargeDate;
        this.convictionDate = convictionDate;
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
}
