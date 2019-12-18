package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.junit.Assert.*;
import static uk.gov.moj.cpp.sjp.query.api.util.FileUtil.getFileContentAsJson;

import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Test;


public class OffenceHelperTest {

    private OffenceHelper offenceHelper = new OffenceHelper();


    final static String offenceId_1 = UUID.randomUUID().toString();
    final static String offenceId_2 = UUID.randomUUID().toString();
    final static String offenceId_3 = UUID.randomUUID().toString();

    final static String WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE = "030d4335-f9fe-39e0-ad7e-d01a0791ff87";
    final static String WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED = "a11670b7-681a-39dc-951e-f2f1c24fb4c9";

    @Test
    public void shouldGetMaxFineLevel() {
        final String offence1Code = "CA03010";
        final String offence2Code = "RT88461";
        final JsonObject offenceDefinition1 =  getFileContentAsJson(format("offence-reference-data/%s.json",offence1Code));
        assertThat("3", Matchers.equalTo(offenceHelper.getMaxFineLevel(offenceDefinition1)));

        final JsonObject offenceDefinition2 =  getFileContentAsJson(format("offence-reference-data/%s.json",offence2Code));
        assertThat("", Matchers.equalTo(offenceHelper.getMaxFineLevel(offenceDefinition2)));

    }

    @Test
    public void hasFinalDecision() {

        final String offence1Code = "CA03010";
        final String offence2Code = "PS00001";
        final String offence3Code = "RT88461";

        final LocalDate offence1CommittedDate = now().minusDays(1);
        final LocalDate offence2CommittedDate = now().minusDays(2);
        final LocalDate offence3CommittedDate = now().minusDays(3);

        final JsonObject offence1 = createObjectBuilder()
                .add("id", offenceId_1)
                .add("offenceCode", offence1Code)
                .add("startDate", offence1CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)
                .build();

        final JsonObject offence2 = createObjectBuilder()
                .add("id", offenceId_2)
                .add("offenceCode", offence2Code)
                .add("startDate", offence2CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)
                .build();

        final JsonObject offence3 = createObjectBuilder()
                .add("id", offenceId_3)
                .add("offenceCode", offence3Code)
                .add("startDate", offence3CommittedDate.toString())
                .build();

        assertTrue(offenceHelper.hasFinalDecision(offence1, getCaseDecisions()));
        assertTrue(offenceHelper.hasFinalDecision(offence2, getCaseDecisions()));

        assertFalse(offenceHelper.hasFinalDecision(offence3, getCaseDecisions()));
    }

    @Test
    public void caseHasNoDecisionHasFinalDecisionReturnsFalse(){
        final String offence1Code = "CA03010";
        final String offence2Code = "PS00001";
        final String offence3Code = "RT88461";

        final LocalDate offence1CommittedDate = now().minusDays(1);
        final LocalDate offence2CommittedDate = now().minusDays(2);
        final LocalDate offence3CommittedDate = now().minusDays(3);

        final JsonObject offence1 = createObjectBuilder()
                .add("id", offenceId_1)
                .add("offenceCode", offence1Code)
                .add("startDate", offence1CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)
                .build();

        final JsonObject offence2 = createObjectBuilder()
                .add("id", offenceId_2)
                .add("offenceCode", offence2Code)
                .add("startDate", offence2CommittedDate.toString())
                .add("withdrawalRequestReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)
                .build();

        final JsonObject offence3 = createObjectBuilder()
                .add("id", offenceId_3)
                .add("offenceCode", offence3Code)
                .add("startDate", offence3CommittedDate.toString())
                .build();

        assertFalse(offenceHelper.hasFinalDecision(offence1, createArrayBuilder().build()));
    }



    private static JsonArray getCaseDecisions() {


        JsonArray offenceDecisions = createArrayBuilder().add(createObjectBuilder()
                .add("offenceDecisions", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("offenceId", offenceId_1)
                                .add("decisionType", "WITHDRAW")
                                .add("withdrawalReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_INSUFFICIENT_EVIDENCE)

                                .add("verdict", "NO_VERDICT"))
                        .add(createObjectBuilder().add("offenceId", offenceId_2)
                                .add("decisionType", "WITHDRAW")
                                .add("withdrawalReasonId", WITHDRAWAL_REQUEST_REASON_ID_FOR_NOT_IN_PUBLIC_INTEREST_TO_PROCEED)

                                .add("verdict", "NO_VERDICT"))
                        .add(createObjectBuilder().add("offenceId", offenceId_3)
                                .add("decisionType", "ADJOURN")
                                .add("verdict", "NO_VERDICT")))).build();

        return offenceDecisions;
    }
}