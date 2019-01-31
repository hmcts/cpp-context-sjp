package uk.gov.moj.cpp.sjp;

import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

@Transformation
public class DefendantDetailsUpdatedEventTransformer extends TopLevelAddressEventTransformer {

    @Override
    public String getEventName() {
        return "sjp.events.defendant-details-updated";
    }

}
