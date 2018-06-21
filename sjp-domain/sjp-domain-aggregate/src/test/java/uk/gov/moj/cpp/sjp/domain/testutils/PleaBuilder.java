package uk.gov.moj.cpp.sjp.domain.testutils;

import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.util.DefaultTestData.CASE_ID;

import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.UUID;

public class PleaBuilder {

    public static UpdatePlea defaultUpdatePlea(final UUID offenceId) {
        return updatePleaGuilty(offenceId);
    }

    public static UpdatePlea updatePleaGuilty(final UUID offenceId) {
        return updatePlea(offenceId, GUILTY);
    }

    public static UpdatePlea updatePleaGuiltyRequestHearing(final UUID offenceId) {
        return updatePlea(offenceId, GUILTY_REQUEST_HEARING);
    }

    public static UpdatePlea updatePleaNotGuilty(final UUID offenceId) {
        return updatePlea(offenceId, NOT_GUILTY);
    }

    private static UpdatePlea updatePlea(final UUID offenceId, final PleaType pleaType) {
        return new UpdatePlea(CASE_ID, offenceId, pleaType);
    }

}
