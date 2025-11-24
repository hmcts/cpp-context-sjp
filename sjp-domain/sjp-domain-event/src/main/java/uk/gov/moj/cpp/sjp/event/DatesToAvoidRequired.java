package uk.gov.moj.cpp.sjp.event;


import static uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@Event(EVENT_NAME)
public class DatesToAvoidRequired {
    public static final String EVENT_NAME = "sjp.events.dates-to-avoid-required";

    private final UUID caseId;
    private final LocalDate datesToAvoidExpirationDate;

    @JsonCreator
    public DatesToAvoidRequired(final @JsonProperty("caseId") UUID caseId,
                                final @JsonProperty("datesToAvoidExpirationDate") LocalDate datesToAvoidExpirationDate) {
        this.caseId = caseId;
        this.datesToAvoidExpirationDate = datesToAvoidExpirationDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDate getDatesToAvoidExpirationDate() {
        return datesToAvoidExpirationDate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DatesToAvoidRequired that = (DatesToAvoidRequired) o;
        return Objects.equal(caseId, that.caseId) &&
                Objects.equal(datesToAvoidExpirationDate, that.datesToAvoidExpirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(caseId, datesToAvoidExpirationDate);
    }
}
