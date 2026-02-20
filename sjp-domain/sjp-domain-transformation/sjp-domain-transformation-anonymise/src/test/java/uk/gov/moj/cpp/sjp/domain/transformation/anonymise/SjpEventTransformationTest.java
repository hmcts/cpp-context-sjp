package uk.gov.moj.cpp.sjp.domain.transformation.anonymise;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.tools.eventsourcing.anonymization.util.FileUtil;

import java.io.StringReader;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonReader;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

public class SjpEventTransformationTest {

    private AnonymiseUtil anonymiseUtil;
    private JsonPath inputJsonPath;
    private JsonObject anonymisedJsonObject;

    private static JsonObject jsonFromString(String jsonObjectStr) {
        JsonReader jsonReader = createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    public void initialize(final String eventName) {
        anonymiseUtil = new AnonymiseUtil().apply(eventName);
        inputJsonPath = anonymiseUtil.getInputJsonPath();
        anonymisedJsonObject = anonymiseUtil.getAnonymisedJsonObject();
    }

    @Test
    public void sjp_events_case_received() {
        initialize("sjp.events.case-received");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.urn", is(inputJsonPath.getString("urn"))),
                        withJsonPath("$.enterpriseId", is(inputJsonPath.getString("enterpriseId"))),
                        withJsonPath("$.prosecutingAuthority", is(inputJsonPath.getString("prosecutingAuthority"))),
                        withJsonPath("$.defendant.offences[*].libraOffenceCode", is(inputJsonPath.getList("defendant.offences.libraOffenceCode"))),
                        withJsonPath("$.defendant.gender", is(inputJsonPath.getString("defendant.gender"))),
                        withJsonPath("$.defendant.title", is(inputJsonPath.getString("defendant.title"))),
                        withJsonPath("$.defendant.hearingLanguage", is(inputJsonPath.getString("defendant.hearingLanguage")))
                        )
                ));
    }

    @Test
    public void sjp_events_defendant_details_moved_from_people() {
        initialize("sjp.events.defendant-details-moved-from-people");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.gender", is(inputJsonPath.getString("gender"))),
                        withJsonPath("$.title", is(inputJsonPath.getString("title")))
                        )
                ));
    }

    @Test
    public void sjp_events_financial_means_updated() {
        initialize("sjp.events.financial-means-updated");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.income.frequency", is(inputJsonPath.getString("income.frequency"))),
                        withJsonPath("$.employmentStatus", is(inputJsonPath.getString("employmentStatus"))),
                        withJsonPath("$.outgoings[*].description", is(inputJsonPath.getList("outgoings.description")))
                        )
                ));
    }

    @Test
    public void sjp_events_sjp_case_created() {
        initialize("sjp.events.sjp-case-created");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.urn", is(inputJsonPath.getString("urn"))),
                        withJsonPath("$.prosecutingAuthority", is(inputJsonPath.getString("prosecutingAuthority"))),
                        withJsonPath("$.initiationCode", is(inputJsonPath.getString("initiationCode"))),
                        withJsonPath("$.ptiUrn", is(inputJsonPath.getString("ptiUrn"))),
                        withJsonPath("$.summonsCode", is(inputJsonPath.getString("summonsCode")))
                        )
                ));
    }

    @Test
    public void sjp_events_online_plea_received() {
        initialize("sjp.events.online-plea-received");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.urn", is(inputJsonPath.getString("urn"))),
                        withJsonPath("$.interpreterLanguage", is(inputJsonPath.getString("interpreterLanguage"))),
                        withJsonPath("$.outgoings[*].description", is(inputJsonPath.getList("outgoings.description"))),
                        withJsonPath("$.financialMeans.employmentStatus", is(inputJsonPath.getString("financialMeans.employmentStatus"))),
                        withJsonPath("$.financialMeans.income.frequency", is(inputJsonPath.getString("financialMeans.income.frequency")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_marked_ready_for_decision() {
        initialize("sjp.events.case-marked-ready-for-decision");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.reason", is(inputJsonPath.getString("reason"))),
                        withJsonPath("$.markedAt", is(inputJsonPath.getString("markedAt"))),
                        withJsonPath("$.sessionType", is(inputJsonPath.getString("sessionType"))),
                        withJsonPath("$.priority", is(inputJsonPath.getString("priority")))
                        )
                ));
    }

    @Test
    public void sjp_events_decision_saved() {
        initialize("sjp.events.decision-saved");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.offenceDecisions[*].type", is(inputJsonPath.getList("offenceDecisions.type"))),
                        withJsonPath("$.offenceDecisions[*].dischargedFor.unit", is(inputJsonPath.getList("offenceDecisions.dischargedFor.unit"))),
                        withJsonPath("$.financialImposition.payment.paymentType", is(inputJsonPath.getString("financialImposition.payment.paymentType"))),
                        withJsonPath("$.financialImposition.payment.reasonWhyNotAttachedOrDeducted", is(inputJsonPath.getString("financialImposition.payment.reasonWhyNotAttachedOrDeducted"))),
                        withJsonPath("$.financialImposition.payment.reasonForDeductingFromBenefits", is(inputJsonPath.getString("financialImposition.payment.reasonForDeductingFromBenefits"))),
                        withJsonPath("$.financialImposition.payment.fineTransferredTo.nationalCourtCode", is(inputJsonPath.getString("financialImposition.payment.fineTransferredTo.nationalCourtCode"))),
                        withJsonPath("$.financialImposition.payment.fineTransferredTo.nationalCourtName", is(inputJsonPath.getString("financialImposition.payment.fineTransferredTo.nationalCourtName"))),
                        withJsonPath("$.financialImposition.payment.paymentTerms.installments.period", is(inputJsonPath.getString("financialImposition.payment.paymentTerms.installments.period")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_status_changed() {
        initialize("sjp.events.case-status-changed");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.caseStatus", is(inputJsonPath.getString("caseStatus")))
                        )
                ));
    }

    @Test
    public void sjp_events_all_financial_means_updated() {
        initialize("sjp.events.all-financial-means-updated");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.income.frequency", is(inputJsonPath.getString("income.frequency"))),
                        withJsonPath("$.employmentStatus", is(inputJsonPath.getString("employmentStatus")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_assignment_requested() {
        initialize("sjp.events.case-assignment-requested");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.session.type", is(inputJsonPath.getString("session.type")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_document_uploaded() {
        initialize("sjp.events.case-document-uploaded");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.documentType", is(inputJsonPath.getString("documentType")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_referred_for_court_hearing() {
        initialize("sjp.events.case-referred-for-court-hearing");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.urn", is(inputJsonPath.getString("urn"))),
                        withJsonPath("$.referredOffences[*].verdict", is(inputJsonPath.getList("referredOffences.verdict")))
                        )
                ));
    }

    @Test
    public void sjp_events_employment_status_updated() {
        initialize("sjp.events.employment-status-updated");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.employmentStatus", is(inputJsonPath.getString("employmentStatus")))
                        )
                ));
    }

    @Test
    public void sjp_events_interpreter_for_defendant_updated() {
        initialize("sjp.events.interpreter-for-defendant-updated");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.interpreter.language", is(inputJsonPath.getString("interpreter.language")))
                        )
                ));
    }

    @Test
    public void sjp_events_magistrate_session_started() {
        initialize("sjp.events.delegated-powers-session-started");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.courtHouseCode", is(inputJsonPath.getString("courtHouseCode"))),
                        withJsonPath("$.courtHouseName", is(inputJsonPath.getString("courtHouseName"))),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", is(inputJsonPath.getString("localJusticeAreaNationalCourtCode")))
                        )
                ));
    }

    @Test
    public void sjp_events_pleaded_guilty() {
        initialize("sjp.events.pleaded-guilty");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.method", is(inputJsonPath.getString("method")))
                        )
                ));
    }

    @Test
    public void sjp_events_pleaded_guilty_court_hearing_requested() {
        initialize("sjp.events.pleaded-guilty-court-hearing-requested");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.method", is(inputJsonPath.getString("method")))
                        )
                ));
    }

    @Test
    public void sjp_events_pleaded_not_guilty() {
        initialize("sjp.events.pleaded-not-guilty");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.method", is(inputJsonPath.getString("method")))
                        )
                ));
    }

    @Test
    public void sjp_events_pleas_set() {
        initialize("sjp.events.pleas-set");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.defendantCourtOptions.interpreter.language", is(inputJsonPath.getString("defendantCourtOptions.interpreter.language"))),
                        withJsonPath("$.pleas[*].pleaType", is(inputJsonPath.getList("pleas.pleaType")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_assignment_created() {
        initialize("sjp.events.case-assignment-created");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.caseAssignmentType", is(inputJsonPath.getString("caseAssignmentType")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_creation_failed_because_case_already_existed() {
        initialize("sjp.events.case-creation-failed-because-case-already-existed");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.urn", is(inputJsonPath.getString("urn")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_reopened_in_libra() {
        initialize("sjp.events.case-reopened-in-libra");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.libraCaseNumber", is(inputJsonPath.getString("libraCaseNumber"))),
                        withJsonPath("$.reason", is(inputJsonPath.getString("reason")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_reopened_in_libra_updated() {
        initialize("sjp.events.case-reopened-in-libra-updated");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.libraCaseNumber", is(inputJsonPath.getString("libraCaseNumber"))),
                        withJsonPath("$.reason", is(inputJsonPath.getString("reason")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_update_rejected() {
        initialize("sjp.events.case-update-rejected");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.reason", is(inputJsonPath.getString("reason")))
                        )
                ));
    }

    @Test
    public void sjp_events_plea_update_denied() {
        initialize("sjp.events.plea-update-denied");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.denialReason", is(inputJsonPath.getString("denialReason")))
                        )
                ));
    }

    @Test
    public void sjp_events_decision_rejected() {
        initialize("sjp.events.decision-rejected");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.decision.offenceDecisions[*].type", is(inputJsonPath.getList("decision.offenceDecisions.type"))),
                        withJsonPath("$.decision.offenceDecisions[*].offenceDecisionInformation.verdict", is(inputJsonPath.getList("decision.offenceDecisions.offenceDecisionInformation.verdict")))
                        )
                ));
    }

    @Test
    public void sjp_events_case_document_added() {
        initialize("sjp.events.case-document-added");

        assertThat(anonymisedJsonObject.toString(),
                isJson(allOf(
                        withJsonPath("$.caseDocument.documentType", is(inputJsonPath.getString("caseDocument.documentType")))
                        )
                ));
    }

    private static class AnonymiseUtil {
        private JsonPath inputJsonPath;
        private JsonObject inputJsonObject;
        private JsonObject anonymisedJsonObject;

        public JsonPath getInputJsonPath() {
            return inputJsonPath;
        }

        public JsonObject getAnonymisedJsonObject() {
            return anonymisedJsonObject;
        }

        public AnonymiseUtil apply(String eventName) {
            String fileContentsAsString = FileUtil.getFileContentsAsString(eventName + "/input.json");
            inputJsonPath = JsonPath.from(fileContentsAsString);

            inputJsonObject = jsonFromString(fileContentsAsString);
            final JsonEnvelope jsonEnvelope = EnvelopeFactory.createEnvelope(eventName, inputJsonObject);
            SjpEventTransformation st = new SjpEventTransformation();
            Stream<JsonEnvelope> apply = st.apply(jsonEnvelope);
            JsonEnvelope envelope = apply.findFirst().get();
            anonymisedJsonObject = envelope.payloadAsJsonObject();
            return this;
        }
    }
}
