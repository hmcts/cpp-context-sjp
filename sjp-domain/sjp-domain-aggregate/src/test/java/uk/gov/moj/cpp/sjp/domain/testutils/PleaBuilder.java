package uk.gov.moj.cpp.sjp.domain.testutils;

import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;

import java.util.UUID;

public class PleaBuilder {

    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final String PLEA = "GUILTY";
    private static final String INDICATED_PLEA = "NOT GUILTY";
    private static final String SECTION = "Section 51";

    public static UpdatePlea defaultUpdatePlea(UUID offenceId) {
        return new UpdatePlea(DefaultTestData.CASE_ID, offenceId, PLEA);
    }
}
