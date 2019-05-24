package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Event("sjp.events.financial-means-deleted")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinancialMeansDeleted {

    private UUID defendantId;

    private List<UUID> materialIds;

    public FinancialMeansDeleted(final UUID defendantId, final List<UUID> materialIds) {
        this.defendantId = defendantId;
        this.materialIds = Collections.unmodifiableList(new ArrayList<>(materialIds));
    }

    public static FinancialMeansDeleted createEvent(final UUID defendantId, final List<UUID> materialIds) {
        return new FinancialMeansDeleted(defendantId, materialIds);
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<UUID> getMaterialIds() {
        if (this.materialIds == null) {
            return new ArrayList<>();
        }
        return materialIds;
    }

}
