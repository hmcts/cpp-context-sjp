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

    private List<UUID> listOfMC100MaterialId;

    public FinancialMeansDeleted(final UUID defendantId,final List<UUID> listOfMC100MaterialId) {
        this.defendantId = defendantId;
        this.listOfMC100MaterialId= Collections.unmodifiableList(new ArrayList<>(listOfMC100MaterialId));
    }

    public static FinancialMeansDeleted createEvent(final UUID defendantId, final List<UUID> listOfMC100MaterialId) {
        return new FinancialMeansDeleted(defendantId,listOfMC100MaterialId);
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public List<UUID> getListOfMC100MaterialId() {
        if(this.listOfMC100MaterialId==null){
            return new ArrayList<>();
        }
        return listOfMC100MaterialId;
    }

}
