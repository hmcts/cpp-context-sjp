package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
@SuppressWarnings("squid:S2384")
@Event(ApplicationOffenceResultsSaved.EVENT_NAME)
public class ApplicationOffenceResultsSaved {

    public static final String EVENT_NAME = "sjp.events.application-offence-results-saved";
    private final Hearing hearing;

    private final String hearingDay;

    private final Boolean isReshare;

    private final List<UUID> shadowListedOffences;

    private final ZonedDateTime sharedTime;

    public ApplicationOffenceResultsSaved(final Hearing hearing, final String hearingDay, final Boolean isReshare, final List<UUID> shadowListedOffences, final ZonedDateTime sharedTime) {
        this.hearing = hearing;
        this.hearingDay = hearingDay;
        this.isReshare = isReshare;
        this.shadowListedOffences = shadowListedOffences;
        this.sharedTime = sharedTime;
    }

    public Hearing getHearing() {
        return hearing;
    }

    public String getHearingDay() {
        return hearingDay;
    }

    public Boolean getIsReshare() {
        return isReshare;
    }

    public List<UUID> getShadowListedOffences() {
        return shadowListedOffences;
    }

    public ZonedDateTime getSharedTime() {
        return sharedTime;
    }

    public static Builder applicationOffenceResultsSaved() {
        return new ApplicationOffenceResultsSaved.Builder();
    }

    @Override
    public String toString() {
        return "ApplicationOffenceResultsSaved{" +
                "hearing='" + hearing + "'," +
                "hearingDay='" + hearingDay + "'," +
                "isReshare='" + isReshare + "'," +
                "shadowListedOffences='" + shadowListedOffences + "'," +
                "sharedTime='" + sharedTime + "'" +
                "}";
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj){
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ApplicationOffenceResultsSaved that = (ApplicationOffenceResultsSaved) obj;

        return java.util.Objects.equals(this.hearing, that.hearing) &&
                java.util.Objects.equals(this.hearingDay, that.hearingDay) &&
                java.util.Objects.equals(this.isReshare, that.isReshare) &&
                java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences) &&
                java.util.Objects.equals(this.sharedTime, that.sharedTime);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(hearing, hearingDay, isReshare, shadowListedOffences, sharedTime);
    }

    public static class Builder {
        private Hearing hearing;

        private String hearingDay;

        private Boolean isReshare;

        private List<UUID> shadowListedOffences;

        private ZonedDateTime sharedTime;

        public Builder withHearing(final Hearing hearing) {
            this.hearing = hearing;
            return this;
        }

        public Builder withHearingDay(final String hearingDay) {
            this.hearingDay = hearingDay;
            return this;
        }

        public Builder withIsReshare(final Boolean isReshare) {
            this.isReshare = isReshare;
            return this;
        }

        public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
            this.shadowListedOffences = shadowListedOffences;
            return this;
        }

        public Builder withSharedTime(final ZonedDateTime sharedTime) {
            this.sharedTime = sharedTime;
            return this;
        }

        public Builder withValuesFrom(final ApplicationOffenceResultsSaved applicationOffenceResultsSaved) {
            this.hearing = applicationOffenceResultsSaved.getHearing();
            this.hearingDay = applicationOffenceResultsSaved.getHearingDay();
            this.isReshare = applicationOffenceResultsSaved.getIsReshare();
            this.shadowListedOffences = applicationOffenceResultsSaved.getShadowListedOffences();
            this.sharedTime = applicationOffenceResultsSaved.getSharedTime();
            return this;
        }

        public ApplicationOffenceResultsSaved build() {
            return new ApplicationOffenceResultsSaved(hearing, hearingDay, isReshare, shadowListedOffences, sharedTime);
        }
    }
}