package uk.gov.moj.cpp.sjp.domain.testutils;

import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;

import java.util.UUID;

public class PleaBuilder {

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final String GUILTY_PLEA = "GUILTY";
    private static final String GUILTY_REQUEST_HEARING_PLEA = "GUILTY_REQUEST_HEARING";
    private static final String NOT_GUILTY_PLEA = "NOT_GUILTY";
    private static final String SECTION = "Section 51";

    public static UpdatePlea defaultUpdatePlea(UUID offenceId) {
        return updatePleaGuilty(offenceId);
    }

    public static UpdatePlea updatePleaGuilty(UUID offenceId) {
        return new UpdatePlea(DefaultTestData.CASE_ID, offenceId, GUILTY_PLEA);
    }

    public static UpdatePlea updatePleaGuiltyRequestHearing(UUID offenceId) {
        return new UpdatePlea(DefaultTestData.CASE_ID, offenceId, GUILTY_REQUEST_HEARING_PLEA);
    }

    public static UpdatePlea updatePleaNotGuilty(UUID offenceId) {
        return new UpdatePlea(DefaultTestData.CASE_ID, offenceId, NOT_GUILTY_PLEA);
    }
}
