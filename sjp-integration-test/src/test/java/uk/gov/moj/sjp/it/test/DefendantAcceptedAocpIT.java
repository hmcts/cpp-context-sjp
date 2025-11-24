package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.parse;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.json.schemas.domains.sjp.User.user;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.AOCP_PENDING;
import static uk.gov.moj.cpp.sjp.event.processor.service.NotificationNotifyDocumentType.PARTIAL_AOCP_CRITERIA_NOTIFICATION;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.AOCP_ACCEPTED_EMAIL_NOTIFICATION;
import static uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus.Status.FAILED;
import static uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus.Status.QUEUED;
import static uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus.Status.SENT;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.PleadOnlineHelper.getOnlinePleaAocpAccepted;
import static uk.gov.moj.sjp.it.helper.SessionHelper.resetAndStartAocpSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubAddMapping;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubGetFromIdMapper;
import static uk.gov.moj.sjp.it.stub.NotificationNotifyStub.verifyNotification;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.executeTimerJobs;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.pollUntilAocpProcessExists;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationFailed;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationSent;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.NotificationNotifyStub;
import uk.gov.moj.sjp.it.util.SjpViewstore;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.collect.Sets;
import io.restassured.path.json.JsonPath;
import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefendantAcceptedAocpIT extends BaseIntegrationTest {
    private static final String TEMPLATE_PLEA_AOCP_ONLINE_PAYLOAD = "raml/json/sjp.command-plead-aocp-online.json";
    private static final String AOCP_ENGLISH_TEMPLATE_ID = "2300fded-e52f-4564-a92a-a6412b1c0f09";
    private static final String DEFENDANT_REGION = "croydon";
    private static final String NATIONAL_COURT_CODE = "1080";
    private final EventListener eventListener = new EventListener();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private UUID offenceId;
    private final SjpViewstore sjpViewstore = new SjpViewstore();
    final User legalAdviser = user()
            .withUserId(UUID.fromString("7242d476-9ca3-454a-93ee-78bf148602bf"))
            .withFirstName("John")
            .withLastName("Smith")
            .build();

    @BeforeEach
    @SuppressWarnings("squid:S2925")
    public void setUp() throws Exception {
        cleanViewStore();
        offenceId = randomUUID();
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        this.createCasePayloadBuilder =
                withDefaults()
                        .withOffenceId(offenceId)
                        .withOffenceCode("PS00002")
                        .withPostingDate(LocalDate.now());
        stubGetFromIdMapper(PARTIAL_AOCP_CRITERIA_NOTIFICATION.name(), this.createCasePayloadBuilder.getId().toString(),
                "CASE_ID", this.createCasePayloadBuilder.getId().toString());
        stubAddMapping();

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubQueryOffencesByCode("PS00002");
        createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        stubForUserDetails(legalAdviser, "ALL");
        pollUntilCaseByIdIsOk(createCasePayloadBuilder.getId());
        stubCountryByPostcodeQuery("W1T 1JY", "England");
        stubDefaultCourtByCourtHouseOUCodeQuery();
    }


    @Test
    public void verifyDefendantAcceptedAocp() throws InterruptedException {

        stubGetFromIdMapper(AOCP_ACCEPTED_EMAIL_NOTIFICATION.name(), createCasePayloadBuilder.getId().toString(),
                "CASE_ID", "7242d476-9ca3-454a-93ee-78bf148602bf");
        stubAddMapping();

        resetAndStartAocpSession();

        final String stringPayload = getPayload(TEMPLATE_PLEA_AOCP_ONLINE_PAYLOAD);

        final CreateCase.OffenceBuilder offenceBuilder = createCasePayloadBuilder.getOffenceBuilder();
        String offenceId = offenceBuilder.getId().toString();

        final JSONObject payload = new JSONObject(stringPayload.replace("OFFENCE_ID", offenceId));

        final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(createCasePayloadBuilder.getId());
        pleadOnlineHelper.pleadOnlineAocp(payload.toString(), Response.Status.ACCEPTED);

        final UUID caseId = createCasePayloadBuilder.getId();
        final CreateCase.DefendantBuilder defendantBuilder = createCasePayloadBuilder.getDefendantBuilder();
        final UUID defendantId = defendantBuilder.getId();

        TimeUnit.SECONDS.sleep(2);

        final JsonPath response = JsonPath.from(
                getOnlinePleaAocpAccepted(caseId.toString(), defendantId.toString(), isJson(allOf(
                        withJsonPath("$.defendantId"),
                        withJsonPath("$.onlinePleaDetails[0].plea", equalTo(AOCP_PENDING.name())))
                ), USER_ID));

        assertThat(response.getString("caseId"), is(caseId.toString()));
        assertThat(response.getString("defendantId"), is(defendantId.toString()));
        assertThat(response.getString("onlinePleaDetails[0].offenceId"), is(offenceId));
        assertThat(parse(response.getString("submittedOn")).toLocalDate(), is(ZonedDateTime.now().toLocalDate()));

        verifyNotification("criminal@gmail.com", createCasePayloadBuilder.getUrn(), AOCP_ENGLISH_TEMPLATE_ID);

        assertStatusUpdatedInViewstore(caseId, QUEUED);
        JsonObject actual = sendNotificationNotifyNotificationFailedPublicEvent(caseId);
        assertThat(actual.getString("caseId"), Matchers.equalTo(caseId.toString()));
        assertThat(actual.getString("failedTime"), not(nullValue()));
        assertStatusUpdatedInViewstore(caseId, FAILED);

        actual = sendNotificationNotifyNotificationSentPublicEvent(caseId);
        assertThat(actual.getString("caseId"), Matchers.equalTo(caseId.toString()));
        assertThat(actual.getString("sentTime"), not(nullValue()));
        assertStatusUpdatedInViewstore(caseId, SENT);

        final String pendingProcess = pollUntilAocpProcessExists("timerTimeout", caseId.toString());
        executeTimerJobs(pendingProcess);

        pollForCase(caseId, new Matcher[]{
                withJsonPath("$.status", is(CaseStatus.COMPLETED.name())),
                withJsonPath("$.id", is(caseId.toString())),
                withJsonPath("$.resultedThroughAocp", is(true)),
                withJsonPath("$.defendantAcceptedAocp", is(true)),
                withJsonPath("$.defendant.offences[0].plea", is("GUILTY"))
        });

    }

    private JsonObject sendNotificationNotifyNotificationFailedPublicEvent(final UUID notificationId) {
        eventListener.subscribe(AocpAcceptedEmailNotificationFailed.EVENT_NAME)
                .run(() -> NotificationNotifyStub.publishNotificationFailedPublicEvent(notificationId));

        final Optional<JsonEnvelope> event = eventListener.popEvent(AocpAcceptedEmailNotificationFailed.EVENT_NAME);

        assertThat(event.isPresent(), is(true));
        return event.get().payloadAsJsonObject();
    }

    private JsonObject sendNotificationNotifyNotificationSentPublicEvent(final UUID notificationId) {
        eventListener.subscribe(AocpAcceptedEmailNotificationSent.EVENT_NAME)
                .run(() -> NotificationNotifyStub.publishNotificationSentPublicEvent(notificationId));

        final Optional<JsonEnvelope> event = eventListener.popEvent(AocpAcceptedEmailNotificationSent.EVENT_NAME);

        assertThat(event.isPresent(), is(true));
        return event.get().payloadAsJsonObject();
    }

    private void assertStatusUpdatedInViewstore(final UUID caseId,
                                                final AocpAcceptedEmailStatus.Status status) {
        pollUntil(() -> sjpViewstore.countNotificationOfAocpAcceptedEmails(caseId, status), greaterThan(0));
    }

    private void pollUntil(final Callable<Integer> function, final Matcher<Integer> matcher) {
        Awaitility.await()
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .timeout(3, TimeUnit.SECONDS)
                .until(function, matcher);
    }
}