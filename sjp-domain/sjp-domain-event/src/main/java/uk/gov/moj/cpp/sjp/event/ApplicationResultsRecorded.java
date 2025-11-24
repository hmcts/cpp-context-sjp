package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S2384")
@Event(ApplicationResultsRecorded.EVENT_NAME)
public class ApplicationResultsRecorded implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String EVENT_NAME = "sjp.events.application-results-recorded";
    private final Hearing hearing;

    private final String hearingDay;

    private final Boolean isReshare;

    private final List<UUID> shadowListedOffences;

    private final ZonedDateTime sharedTime;

    public ApplicationResultsRecorded(final Hearing hearing, final String hearingDay, final Boolean isReshare, final List<UUID> shadowListedOffences, final ZonedDateTime sharedTime) {
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

    public static Builder applicationResultsRecorded() {
        return new uk.gov.moj.cpp.sjp.event.ApplicationResultsRecorded.Builder();
    }

    @Override
    public String toString() {
        return "ApplicationResultsRecorded{" +
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
        final uk.gov.moj.cpp.sjp.event.ApplicationResultsRecorded that = (uk.gov.moj.cpp.sjp.event.ApplicationResultsRecorded) obj;

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

        public Builder withValuesFrom(final ApplicationResultsRecorded applicationResultsRecorded) {
            this.hearing = applicationResultsRecorded.getHearing();
            this.hearingDay = applicationResultsRecorded.getHearingDay();
            this.isReshare = applicationResultsRecorded.getIsReshare();
            this.shadowListedOffences = applicationResultsRecorded.getShadowListedOffences();
            this.sharedTime = applicationResultsRecorded.getSharedTime();
            return this;
        }

        public ApplicationResultsRecorded build() {
            return new uk.gov.moj.cpp.sjp.event.ApplicationResultsRecorded(hearing, hearingDay, isReshare, shadowListedOffences, sharedTime);
        }
    }
}