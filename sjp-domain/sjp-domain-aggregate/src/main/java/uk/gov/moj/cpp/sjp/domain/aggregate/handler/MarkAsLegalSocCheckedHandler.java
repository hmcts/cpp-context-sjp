package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.MarkedAsLegalSocChecked;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkAsLegalSocCheckedHandler {

    public static final MarkAsLegalSocCheckedHandler INSTANCE = new MarkAsLegalSocCheckedHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkAsLegalSocCheckedHandler.class);

    private MarkAsLegalSocCheckedHandler() {
    }

    public Stream<Object> markAsLegalSocChecked(final UUID caseId, final UUID checkedBy, final ZonedDateTime checkedAt, CaseAggregateState state) {
        if(state.isCaseCompleted()){
            return this.createLegalSocCheckedEvent(caseId, checkedBy, checkedAt);
        } else {
            LOGGER.warn("Case is not complete cannot mark as legal SOC checked");
            return Stream.empty();
        }
    }

    private Stream<Object> createLegalSocCheckedEvent(final UUID caseId,  final UUID checkedBy, final ZonedDateTime checkedAt) {
        return Stream.of(new MarkedAsLegalSocChecked(caseId, checkedBy, checkedAt));
    }
}