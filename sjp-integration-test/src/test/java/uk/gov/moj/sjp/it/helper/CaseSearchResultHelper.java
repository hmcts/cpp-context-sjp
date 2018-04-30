package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startMagistrateSession;
import static uk.gov.moj.sjp.it.stub.ReferenceDataStub.stubCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.test.BaseIntegrationTest.USER_ID;
import static uk.gov.moj.sjp.it.util.DefaultRequests.searchCases;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CaseSearchResultHelper {

    public static final String CASE_SEARCH_RESULTS_MEDIA_TYPE = "application/vnd.sjp.query.case-search-results+json";
    private static final String LONDON_LJA_NATIONAL_COURT_CODE = "2572";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

    private final UUID caseId;
    private final String urn;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final UUID searchUserId;

    public CaseSearchResultHelper(final UUID searchUserId) {
        this(null, null, null, null, searchUserId);
    }

    public CaseSearchResultHelper(final UUID caseId, final String urn, final String defendantLastName, final LocalDate dateOfBirth) {
        this(caseId, urn, defendantLastName, dateOfBirth, USER_ID);
    }

    private CaseSearchResultHelper(final UUID caseId, final String urn, final String defendantLastName, final LocalDate dateOfBirth, final UUID searchUserId) {
        this.caseId = caseId;
        this.urn = urn;
        this.lastName = defendantLastName;
        this.dateOfBirth = dateOfBirth;
        this.searchUserId = searchUserId;
    }

    public void verifyPersonFound(final String urn, final String lastName) {
        poll(searchCases(lastName, searchUserId))
                .until(status().is(OK), payload().isJson(
                        withJsonPath("$.results[?(@.urn=='" + urn + "')]", hasSize(1))
                ));
    }

    public void verifyPersonNotFound(final String urn, final String lastName) {
        poll(searchCases(lastName, searchUserId))
                .timeout(5, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(
                        withJsonPath("$.results[?(@.urn=='" + urn + "')]", hasSize(0))
                ));
    }

    public void verifyPleaReceivedDate() {
        poll(searchCases(urn, searchUserId))
                .until(status().is(OK), payload().isJson(
                        withJsonPath("$.results[0].pleaDate", notNullValue())
                ));
    }

    public void verifyNoPleaReceivedDate() {
        poll(searchCases(urn, searchUserId))
                .until(status().is(OK), payload().isJson(
                        withoutJsonPath("$.results[0].pleaDate")
                ));
    }

    public void verifyWithdrawalRequestedDate() {
        poll(searchCases(urn, searchUserId))
                .until(status().is(OK), payload().isJson(
                        withJsonPath("$.results[0].withdrawalRequestedDate", notNullValue())
                ));
    }

    public void verifyNoWithdrawalRequestedDate() {
        poll(searchCases(urn, searchUserId))
                .until(status().is(OK), payload().isJson(
                        withoutJsonPath("$.results[0].withdrawalRequestedDate")
                ));
    }

    public void verifyAssignment(final boolean assigned) {
        poll(searchCases(urn, searchUserId))
                .until(status().is(OK), payload().isJson(allOf(
                        withJsonPath("$.results[0].urn", is(urn)),
                        withJsonPath("$.results[0].assigned", is(assigned)))));
    }

    public void verifyUrnFound(final String urn) {
        poll(searchCases(urn, searchUserId))
                .until(status().is(OK), payload().isJson(withJsonPath("$.results", hasSize(1))));
    }

    public void verifyUrnNotFound(final String urn) {
        poll(searchCases(urn, searchUserId))
                .until(status().is(OK), payload().isJson(withJsonPath("$.results", hasSize(0))));
    }

    public void verifyPersonInfoByUrn() {
        verifyPersonInfo(urn, lastName, dateOfBirth);
    }

    public void verifyPersonInfoByLastNameAndDateOfBirth(String lastName, LocalDate dateOfBirth) {
        verifyPersonInfo(lastName, lastName, dateOfBirth);
    }

    public void startSessionAndAssignCase() {
        stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, LONDON_LJA_NATIONAL_COURT_CODE);

        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        startMagistrateSession(sessionId, userId, LONDON_COURT_HOUSE_OU_CODE, "Alan Smith");
        requestCaseAssignment(sessionId, userId);
    }

    public void completeCase() {
        new CompleteCaseProducer(caseId).completeCase();
    }

    private void verifyPersonInfo(final String query, final String lastName, final LocalDate dateOfBirth) {
        poll(searchCases(query, searchUserId))
                .until(status().is(OK), payload().isJson(allOf(
                        withJsonPath("$.results[*]", hasItem(isJson(
                                allOf(
                                        withJsonPath("urn", equalTo(urn)),
                                        withJsonPath("defendant.lastName", equalTo(lastName)),
                                        withJsonPath("defendant.dateOfBirth", equalTo(LocalDates.to(dateOfBirth)))
                                )))))));
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
}
