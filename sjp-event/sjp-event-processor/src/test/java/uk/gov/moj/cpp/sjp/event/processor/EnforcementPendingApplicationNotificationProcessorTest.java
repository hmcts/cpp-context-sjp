package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.CaseApplication;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerated;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementAreaEmailHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementEmailAttachmentService;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementNotificationService;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDetailsDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.EmailNotification;
import uk.gov.moj.cpp.sjp.event.processor.utils.JsonHelper;
import uk.gov.moj.cpp.sjp.event.processor.utils.fake.FakeNotificationNotify;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonValue;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnforcementPendingApplicationNotificationProcessorTest {

    private static final String LJA_CODE = "LJA_CODE";
    private static final LocalDate DEFENDANT_DATE_OF_BIRTH = LocalDate.of(1980, 11, 5);

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private SjpService sjpService;
    @Mock
    private Sender sender;
    @Spy
    private FakeNotificationNotify notificationNotify;
    @Mock
    private EnforcementNotificationService endorsementRemovalNotificationService;
    @InjectMocks
    private EnforcementPendingApplicationNotificationProcessor processor;
    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;
    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private EnforcementAreaEmailHelper enforcementAreaEmailHelper;
    @Mock
    private EnforcementEmailAttachmentService enforcementEmailAttachmentService;

    private UUID templateId = randomUUID();
    private String replyToAddress = "noreply@cjscp.org.uk";
    private CaseDetails caseDetails;

    @Before
    public void setUp() throws IllegalAccessException {

        FieldUtils.writeField(processor, "templateId", templateId.toString(), true);
        FieldUtils.writeField(processor, "replyToAddress", replyToAddress, true);
    }

    @Test
    public void shouldSendEmailToNotificationNotify() {
        final UUID applicationId = randomUUID();
        final UUID fileId = randomUUID();

        final EnforcementPendingApplicationNotificationGenerated event = new EnforcementPendingApplicationNotificationGenerated(applicationId, fileId, now());
        final JsonEnvelope envelope = envelope(event);
        givenSjpCaseWithApplicationDecision(applicationId, DEFENDANT_DATE_OF_BIRTH, envelope);
        givenAnEmailSubject(ApplicationType.STAT_DEC);

        when(enforcementAreaEmailHelper.enforcementEmail(envelope, caseDetails, caseDetails.getDefendant().getPersonalDetails().getAddress().getPostcode())).thenReturn("notification@dvla.gov.uk");
        when(sjpService.getCaseDetailsByApplicationId(applicationId, envelope)).thenReturn(caseDetails);

        processor.sendEmailToNotificationNotify(envelope);

        final List<EmailNotification> requests = notificationNotify.getSendEmailRequests();
        assertThat(requests, hasSize(1));
        final EmailNotification emailNotification = requests.get(0);
        assertThat(emailNotification.getNotificationId(), equalTo(event.getApplicationId()));
        assertThat(emailNotification.getTemplateId(), equalTo(templateId));
        assertThat(emailNotification.getSendToAddress(), equalTo("notification@dvla.gov.uk"));
        assertThat(emailNotification.getReplyToAddress(), equalTo(replyToAddress));
        assertThat(emailNotification.getFileId(), equalTo(event.getFileId()));
        assertThat(emailNotification.getSubject().isPresent(), is(true));
        assertThat(emailNotification.getSubject().get(), is("Email Subject"));
    }

    @Test
    public void shouldSendNotificationQueuedCommand() {
        final UUID applicationId = randomUUID();
        final UUID fileId = randomUUID();
        final EnforcementPendingApplicationNotificationGenerated event =
                new EnforcementPendingApplicationNotificationGenerated(applicationId, fileId, ZonedDateTime.now());
        final JsonEnvelope envelope = envelope(event);
        givenSjpCaseWithApplicationDecision(applicationId, envelope);
        givenAnEmailSubject(ApplicationType.STAT_DEC);
        when(sjpService.getCaseDetailsByApplicationId(applicationId, envelope)).thenReturn(caseDetails);

        processor.sendEmailToNotificationNotify(envelope);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), metadata().withName("sjp.command.enforcement-pending-application-queue-notification"));
        assertThat(sentEnvelope.payload(), payloadIsJson(withJsonPath("$.applicationId", equalTo(event.getApplicationId().toString()))));
    }

    @Test
    public void shouldThrowErrorIfCaseNotPresentInViewstore() {
        final UUID applicationId = randomUUID();
        final UUID fileId = randomUUID();

        final EnforcementPendingApplicationNotificationGenerated event =
                new EnforcementPendingApplicationNotificationGenerated(applicationId, fileId, now());
        final JsonEnvelope envelope = envelope(event);
        givenAnEmailSubject(ApplicationType.STAT_DEC);
        when(sjpService.getCaseDetailsByApplicationId(applicationId, envelope)).thenReturn(null);

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Could not find case for application id: " + applicationId);

        processor.sendEmailToNotificationNotify(envelope);
    }

    private JsonEnvelope envelope(final EnforcementPendingApplicationNotificationGenerated event) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withName(EnforcementPendingApplicationNotificationGenerated.EVENT_NAME).withId(randomUUID()),
                JsonHelper.toJsonObject(event)
        );
    }

    private void givenSjpCaseWithApplicationDecision(final UUID applicationDecisionId, final JsonEnvelope envelope) {
        givenSjpCaseWithApplicationDecision(applicationDecisionId, null, envelope);
    }

    private void givenSjpCaseWithApplicationDecision(final UUID applicationId, final LocalDate defendantDateOfBirth, final JsonEnvelope envelope) {
        final UUID caseId = randomUUID();
        caseDetails = new CaseDetailsDecorator(CaseDetails
                .caseDetails()
                .withId(caseId)
                .withUrn("CASE_URN_001")
                .withDefendant(Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                        .withFirstName("Firstname")
                        .withLastName("Lastname")
                        .withDateOfBirth(defendantDateOfBirth)
                        .withAddress(Address.address().withPostcode("CR0 1AB").build())
                        .build()).build())
                .withCaseApplication(CaseApplication.caseApplication().withApplicationType(ApplicationType.STAT_DEC).build())
                .withCaseDecisions(singletonList(CaseDecision.caseDecision()
                        .withId(applicationId)
                        .withApplicationDecision(QueryApplicationDecision.queryApplicationDecision()
                                .withApplicationType(ApplicationType.STAT_DEC)
                                .withGranted(true)
                                .build())
                        .withSession(Session.session().withLocalJusticeAreaNationalCourtCode(LJA_CODE).build())
                        .build()))
                .build());

        when(sjpService.getCaseDetails(caseId, envelope)).thenReturn(caseDetails);
    }

    private void givenAnEmailSubject(ApplicationType applicationType) {
        when(enforcementEmailAttachmentService.getEmailSubject(applicationType)).thenReturn("Email Subject");
    }
}