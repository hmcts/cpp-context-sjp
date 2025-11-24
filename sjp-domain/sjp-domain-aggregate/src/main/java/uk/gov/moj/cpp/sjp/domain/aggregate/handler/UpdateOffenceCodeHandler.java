package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.moj.cpp.sjp.domain.OffenceCode;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.UpdateOffenceCodeRequestReceived;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateOffenceCodeHandler {

    public static final UpdateOffenceCodeHandler INSTANCE = new UpdateOffenceCodeHandler();

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateOffenceCodeHandler.class);
    public static final String GM00001 = "GM00001";

    private UpdateOffenceCodeHandler() {
    }

    public Stream<Object> updateOffenceCode(CaseAggregateState caseAggregateState, UUID caseId, final String offenceCode) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();
        if (!caseAggregateState.isCaseCompleted()
                && caseAggregateState.isMetroLinkSubmittedWithWrongOffence()) { // just to be sure that some case is not referred to court while we are running the BDF

            final UpdateOffenceCodeRequestReceived.Builder eventBuilder = UpdateOffenceCodeRequestReceived.builder();
            final List<OffenceCode> offenceCodeList = new ArrayList<>();

            caseAggregateState.getOffenceData().forEach(offence -> {
                if (offence.getLibraOffenceCode().equals(GM00001)) {
                    offenceCodeList.add(new OffenceCode(offence.getId(), offenceCode));
                }
            });

            streamBuilder.add(eventBuilder.withUpdateOffenceCodes(offenceCodeList).withCaseId(caseId).build());
        } else {
            LOGGER.info(String.format("Case completed so not adding the UpdateOffenceCodeRequestReceived event for case id  --->> %s", caseId));
        }

        return streamBuilder.build();
    }

}