package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import org.hamcrest.CoreMatchers;
import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.LegalEntityDetails;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerated;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.SystemIdMapperService;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDetailsDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.notification.EndorsementRemovalNotificationService;
import uk.gov.moj.cpp.sjp.event.processor.service.notificationnotify.EmailNotification;
import uk.gov.moj.cpp.sjp.event.processor.utils.JsonHelper;
import uk.gov.moj.cpp.sjp.event.processor.utils.fake.FakeNotificationNotify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonValue;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EndorsementRemovalNotificationProcessorTest {

    private static final String LJA_CODE = "LJA_CODE";
    private static final String LJA_NAME = "LJA Name";
    private static final LocalDate DEFENDANT_DATE_OF_BIRTH = LocalDate.of(1980, 11, 5);

    @Mock
    private SjpService sjpService;
    @Mock
    private Sender sender;
    @Mock
    private ReferenceDataService referenceDataService;
    @Spy
    private FakeNotificationNotify notificationNotify;
    @Mock
    private EndorsementRemovalNotificationService endorsementRemovalNotificationService;
    @InjectMocks
    private EndorsementRemovalNotificationProcessor processor;
    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;
    @Mock
    private SystemIdMapperService systemIdMapperService;

    private UUID templateId = randomUUID();
    private String replyToAddress = "noreply@cjscp.org.uk";
    private CaseDetailsDecorator caseDetails;

    @BeforeEach
    public void setUp() throws IllegalAccessException {
        FieldUtils.writeField(processor, "templateId", templateId.toString(), true);
        FieldUtils.writeField(processor, "replyToAddress", replyToAddress, true);
    }

    @Test
    public void shouldSendEmailToNotificationNotify() {
        final UUID applicationDecisionId = randomUUID();
        final UUID fileId = randomUUID();
        final NotificationToRemoveEndorsementsGenerated event = new NotificationToRemoveEndorsementsGenerated(applicationDecisionId, fileId);
        final JsonEnvelope envelope = envelope(event);
        givenDvlaEmailAddressIsPresentInReferenceData(envelope);
        givenSjpCaseWithApplicationDecision(applicationDecisionId, DEFENDANT_DATE_OF_BIRTH, envelope);
        givenAnEmailSubject(envelope);

        processor.sendEmailToNotificationNotify(envelope);

        final List<EmailNotification> requests = notificationNotify.getSendEmailRequests();
        assertThat(requests, hasSize(1));
        final EmailNotification emailNotification = requests.get(0);
        assertThat(emailNotification.getNotificationId(), equalTo(event.getApplicationDecisionId()));
        assertThat(emailNotification.getTemplateId(), equalTo(templateId));
        assertThat(emailNotification.getSendToAddress(), equalTo("notification@dvla.gov.uk"));
        assertThat(emailNotification.getReplyToAddress(), equalTo(replyToAddress));
        assertThat(emailNotification.getFileId(), equalTo(event.getFileId()));
        assertThat(emailNotification.getSubject().isPresent(), is(true));
        assertThat(emailNotification.getSubject().get(), is("Email Subject"));
    }

    @Test
    public void shouldSendEmailToNotificationNotifyWhenDefendantIsCompany() {
        final UUID applicationDecisionId = randomUUID();
        final UUID fileId = randomUUID();
        final NotificationToRemoveEndorsementsGenerated event = new NotificationToRemoveEndorsementsGenerated(applicationDecisionId, fileId);
        final JsonEnvelope envelope = envelope(event);
        givenDvlaEmailAddressIsPresentInReferenceData(envelope);
        givenSjpCaseWithApplicationDecisionWhenDefendantIsCompany(applicationDecisionId, envelope);
        givenAnEmailSubject(envelope);

        processor.sendEmailToNotificationNotify(envelope);

        final List<EmailNotification> requests = notificationNotify.getSendEmailRequests();
        assertThat(requests, hasSize(1));
        final EmailNotification emailNotification = requests.get(0);
        assertThat(emailNotification.getNotificationId(), equalTo(event.getApplicationDecisionId()));
        assertThat(emailNotification.getTemplateId(), equalTo(templateId));
        assertThat(emailNotification.getSendToAddress(), equalTo("notification@dvla.gov.uk"));
        assertThat(emailNotification.getReplyToAddress(), equalTo(replyToAddress));
        assertThat(emailNotification.getFileId(), equalTo(event.getFileId()));
        assertThat(emailNotification.getSubject().isPresent(), is(true));
        assertThat(emailNotification.getSubject().get(), is("Email Subject"));
    }

    @Test
    public void shouldSendNotificationQueuedCommand() {
        final UUID applicationDecisionId = randomUUID();
        final UUID fileId = randomUUID();
        final NotificationToRemoveEndorsementsGenerated event =
                new NotificationToRemoveEndorsementsGenerated(applicationDecisionId, fileId);
        final JsonEnvelope envelope = envelope(event);
        givenDvlaEmailAddressIsPresentInReferenceData(envelope);
        givenSjpCaseWithApplicationDecision(applicationDecisionId, envelope);
        givenAnEmailSubject(envelope);

        processor.sendEmailToNotificationNotify(envelope);

        verify(sender,times(2)).send(envelopeCaptor.capture());
        final List<Envelope<JsonValue>> envelopesSent = envelopeCaptor.getAllValues();
        assertThat(envelopesSent.get(0).metadata(), metadata().withName("sjp.command.endorsement-removal-notification-queued"));
        assertThat(envelopesSent.get(0).payload(), payloadIsJson(withJsonPath("$.applicationDecisionId", equalTo(event.getApplicationDecisionId().toString()))));
        assertThat(envelopesSent.get(1).metadata(), metadata().withName("sjp.command.upload-case-document"));
    }

    @Test
    public void shouldThrowErrorIfEmailNotPresentInReferenceData() {
        final UUID applicationDecisionId = randomUUID();
        final UUID fileId = randomUUID();
        final NotificationToRemoveEndorsementsGenerated event =
                new NotificationToRemoveEndorsementsGenerated(applicationDecisionId, fileId);
        final JsonEnvelope envelope = envelope(event);
        when(referenceDataService.getDvlaPenaltyPointNotificationEmailAddress(envelope)).thenReturn(Optional.empty());

        var e = assertThrows(IllegalStateException.class, () -> processor.sendEmailToNotificationNotify(envelope));
        assertThat(e.getMessage(), CoreMatchers.is("Unable to find DVLA email address in reference data"));
    }

    @Test
    public void shouldThrowErrorIfCaseNotPresentInViewstore() {
        final UUID applicationDecisionId = randomUUID();
        final UUID fileId = randomUUID();
        final NotificationToRemoveEndorsementsGenerated event =
                new NotificationToRemoveEndorsementsGenerated(applicationDecisionId, fileId);
        final JsonEnvelope envelope = envelope(event);
        givenDvlaEmailAddressIsPresentInReferenceData(envelope);
        when(sjpService.getCaseDetailsByApplicationDecisionId(applicationDecisionId, envelope)).thenReturn(Optional.empty());

        var e = assertThrows(IllegalStateException.class, () -> processor.sendEmailToNotificationNotify(envelope));
        assertThat(e.getMessage(), CoreMatchers.is("Could not find case for application decision id: " + applicationDecisionId));
    }

    private JsonEnvelope envelope(final NotificationToRemoveEndorsementsGenerated event) {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withName(NotificationToRemoveEndorsementsGenerated.EVENT_NAME).withId(randomUUID()),
                JsonHelper.toJsonObject(event)
        );
    }

    private void givenSjpCaseWithApplicationDecision(final UUID applicationDecisionId, final JsonEnvelope envelope) {
        givenSjpCaseWithApplicationDecision(applicationDecisionId, null, envelope);
    }

    private void givenSjpCaseWithApplicationDecision(final UUID applicationDecisionId, final LocalDate defendantDateOfBirth, final JsonEnvelope envelope) {
        caseDetails = new CaseDetailsDecorator(CaseDetails
                .caseDetails()
                .withId(randomUUID())
                .withUrn("CASE_URN_001")
                .withDefendant(Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                        .withFirstName("Firstname")
                        .withLastName("Lastname")
                        .withDateOfBirth(defendantDateOfBirth)
                        .build()).build())
                .withCaseDecisions(singletonList(CaseDecision.caseDecision()
                        .withId(applicationDecisionId)
                        .withApplicationDecision(QueryApplicationDecision.queryApplicationDecision()
                                .withApplicationType(ApplicationType.STAT_DEC)
                                .withGranted(true)
                                .build())
                        .withSession(Session.session().withLocalJusticeAreaNationalCourtCode(LJA_CODE).build())
                        .build()))
                .build());

        when(sjpService.getCaseDetailsByApplicationDecisionId(applicationDecisionId, envelope)).thenReturn(Optional.of(caseDetails));
    }

    private void givenSjpCaseWithApplicationDecisionWhenDefendantIsCompany(final UUID applicationDecisionId, final JsonEnvelope envelope) {
        caseDetails = new CaseDetailsDecorator(CaseDetails
                .caseDetails()
                .withId(randomUUID())
                .withUrn("CASE_URN_001")
                .withDefendant(Defendant.defendant()
                        .withLegalEntityDetails(LegalEntityDetails.legalEntityDetails()
                                .withLegalEntityName("Samba LTD")
                                .withAddress(Address.address()
                                        .withPostcode("BH7 7EA")
                                        .build())
                                .build()).build())
                .withCaseDecisions(singletonList(CaseDecision.caseDecision()
                        .withId(applicationDecisionId)
                        .withApplicationDecision(QueryApplicationDecision.queryApplicationDecision()
                                .withApplicationType(ApplicationType.STAT_DEC)
                                .withGranted(true)
                                .build())
                        .withSession(Session.session().withLocalJusticeAreaNationalCourtCode(LJA_CODE).build())
                        .build()))
                .build());

        when(sjpService.getCaseDetailsByApplicationDecisionId(applicationDecisionId, envelope)).thenReturn(Optional.of(caseDetails));
    }

    private void givenDvlaEmailAddressIsPresentInReferenceData(final JsonEnvelope envelope) {
        when(referenceDataService.getDvlaPenaltyPointNotificationEmailAddress(envelope)).thenReturn(Optional.of("notification@dvla.gov.uk"));
    }

    private void givenAnEmailSubject(final JsonEnvelope envelope) {
        when(endorsementRemovalNotificationService.buildEmailSubject(caseDetails.getCurrentApplicationDecision().get(), envelope)).thenReturn("Email Subject");
    }
}