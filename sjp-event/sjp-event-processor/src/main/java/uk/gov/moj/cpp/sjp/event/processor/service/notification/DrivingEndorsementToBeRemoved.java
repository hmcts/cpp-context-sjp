package uk.gov.moj.cpp.sjp.event.processor.service.notification;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DrivingEndorsementToBeRemoved {

    private final String offenceTitle;
    private final String originalCourtCode;
    private final String originalConvictionDate;
    private final String dvlaEndorsementCode;
    private final String originalOffenceDate;

    public DrivingEndorsementToBeRemoved(final String offenceTitle,
                                         final String originalCourtCode,
                                         final String originalConvictionDate,
                                         final String dvlaEndorsementCode,
                                         final String originalOffenceDate) {
        this.offenceTitle = offenceTitle;
        this.originalCourtCode = originalCourtCode;
        this.originalConvictionDate = originalConvictionDate;
        this.dvlaEndorsementCode = dvlaEndorsementCode;
        this.originalOffenceDate = originalOffenceDate;
    }

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public String getOriginalCourtCode() {
        return originalCourtCode;
    }

    public String getOriginalConvictionDate() {
        return originalConvictionDate;
    }

    public String getDvlaEndorsementCode() {
        return dvlaEndorsementCode;
    }

    public String getOriginalOffenceDate() {
        return originalOffenceDate;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static class Builder {

        private String offenceTitle;
        private String originalCourtCode;
        private String originalConvictionDate;
        private String dvlaEndorsementCode;
        private String originalOffenceDate;

        public Builder withOffenceTitle(final String offenceTitle) {
            this.offenceTitle = offenceTitle;
            return this;
        }

        public Builder withOriginalCourtCode(final String originalCourtCode) {
            this.originalCourtCode = originalCourtCode;
            return this;
        }

        public Builder withOriginalConvictionDate(final String originalConvictionDate) {
            this.originalConvictionDate = originalConvictionDate;
            return this;
        }

        public Builder withDvlaEndorsementCode(final String dvlaEndorsementCode) {
            this.dvlaEndorsementCode = dvlaEndorsementCode;
            return this;
        }

        public Builder withOriginalOffenceDate(final String originalOffenceDate) {
            this.originalOffenceDate = originalOffenceDate;
            return this;
        }

        public DrivingEndorsementToBeRemoved build() {
            return new DrivingEndorsementToBeRemoved(
                    this.offenceTitle,
                    this.originalCourtCode,
                    this.originalConvictionDate,
                    this.dvlaEndorsementCode,
                    this.originalOffenceDate
            );
        }
    }
}
