package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.time.LocalDate;
import java.util.UUID;

import com.google.common.base.Objects;

public class NotifiedPleaView {

    private final UUID offenceId;
    private final LocalDate notifiedPleaDate;
    private final String notifiedPleaValue;

    public NotifiedPleaView(
            final UUID offenceId,
            final LocalDate notifiedPleaDate,
            final String notifiedPleaValue) {

        this.offenceId = offenceId;
        this.notifiedPleaDate = notifiedPleaDate;
        this.notifiedPleaValue = notifiedPleaValue;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public LocalDate getNotifiedPleaDate() {
        return notifiedPleaDate;
    }

    public String getNotifiedPleaValue() {
        return notifiedPleaValue;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final NotifiedPleaView that = (NotifiedPleaView) o;

        return Objects.equal(offenceId, that.offenceId) &&
                Objects.equal(notifiedPleaDate, that.notifiedPleaDate) &&
                Objects.equal(notifiedPleaValue, that.notifiedPleaValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(offenceId, notifiedPleaDate, notifiedPleaValue);
    }
}
