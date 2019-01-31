package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.Objects;
import java.util.UUID;

public class HearingTypeView {

    private final UUID id;

    public HearingTypeView(final UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HearingTypeView that = (HearingTypeView) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
