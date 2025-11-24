package uk.gov.moj.cpp.sjp;

import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

@Transformation
public class DefendantDetailsMovedFromPeopleEventTransformer extends TopLevelAddressEventTransformer {

    @Override
    public String getEventName() {
        return "sjp.events.defendant-details-moved-from-people";
    }
}
