package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.PleasSet.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class PleasSet {

    public static final String EVENT_NAME = "sjp.events.pleas-set";

    private final UUID caseId;

    /**
     * Court hearing options (interpreter needed, welsh hearing)
     */
    private final DefendantCourtOptions defendantCourtOptions;

    private final List<Plea> pleas;

    @JsonCreator
    public PleasSet(@JsonProperty("caseId") final UUID caseId,
                    @JsonProperty("defendantCourtOptions") final DefendantCourtOptions defendantCourtOptions,
                    @JsonProperty("pleas") final List<Plea> pleas) {
        this.caseId = caseId;
        this.defendantCourtOptions = defendantCourtOptions;
        this.pleas = new ArrayList<>(pleas);
    }

    public DefendantCourtOptions getDefendantCourtOptions() {
        return defendantCourtOptions;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<Plea> getPleas() {
        return Collections.unmodifiableList(pleas);
    }

    public Optional<Plea> getPlea(final PleaKey pleaKey){
        return this.pleas.stream().
                filter(plea ->
                        plea.getDefendantId().equals(pleaKey.defendantId) &&
                        plea.getOffenceId().equals(pleaKey.offenceId))
                .findFirst();
    }

    public static class PleaKey {

        private UUID defendantId;

        private UUID offenceId;

        public PleaKey(final UUID defendantId, final UUID offenceId) {
            this.defendantId = defendantId;
            this.offenceId = offenceId;
        }

        public UUID getDefendantId() {
            return defendantId;
        }

        public UUID getOffenceId() {
            return offenceId;
        }

    }

    @Override
    @SuppressWarnings("squid:S00122")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PleasSet pleasSet = (PleasSet) o;
        return caseId.equals(pleasSet.caseId) &&
                Objects.equals(defendantCourtOptions, pleasSet.defendantCourtOptions) &&
                pleas.equals(pleasSet.pleas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, defendantCourtOptions, pleas);
    }

    @Override
    public String toString() {
        return "PleasSet{" +
                "caseId=" + caseId +
                ", defendantCourtOptions=" + defendantCourtOptions +
                ", pleas=" + pleas +
                '}';
    }
}
