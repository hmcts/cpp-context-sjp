package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.REOPENING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.justice.services.test.utils.core.converter.JsonObjectToObjectConverterFactory.createJsonObjectToObjectConverter;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION;

import org.hamcrest.CoreMatchers;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.CaseApplication;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationRequired;
import uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.ConversionFormat;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.DocumentGenerationRequest;
import uk.gov.moj.cpp.sjp.event.processor.utils.fake.FakeFileStorer;
import uk.gov.moj.cpp.sjp.event.processor.utils.fake.FakeSystemDocGenerator;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("squid:S2187")
public class EnforcementEmailAttachmentServiceTest {
    private static final String EVENT_NAME = "sjp.events.enforcement-pending-application-notification-initiated";
    private static final String STAT_DECS_EMAIL_SUBJECT = "Subject: APPLICATION FOR A STATUTORY DECLARATION RECEIVED (COMMISSIONER OF OATHS)";
    private static final String REOPENING_EMAIL_SUBJECT = "Subject: APPLICATION TO REOPEN RECEIVED";
    private static final String INVALID_EMAIL_SUBJECT = "Subject: INVALID EMAIL SENT";

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private FakeFileStorer fileStorer;

    @Spy
    private JsonObjectToObjectConverter converter = createJsonObjectToObjectConverter();

    @Spy
    private FakeSystemDocGenerator systemDocGenerator;

    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private EnforcementEmailAttachmentService service;

    @Mock
    private SjpService sjpService;


    private EnforcementPendingApplicationNotificationRequired initiatedEvent;
    final UUID CASE_ID = UUID.randomUUID();
    final UUID APP_ID = UUID.randomUUID();
    final ZonedDateTime INITIATED_TIME = ZonedDateTime.now();
    final String GOB_ACCOUNT_NUMBER = "1234567";
    final String DEF_NAME = "FIRSTNAME LASTNAME";
    final String URN = "TFL123456";
    final int DIV_CODE = 99;
    final LocalDate LISTED_DATE = LocalDate.now().minusDays(10);
    final String SEND_TO_ADDRESS = "enforcement@email.com";
    final String DEFENDANT_DATE_OF_BIRTH = "26-01-1967";
    final String DEFENDANT_ADDRESS = "Flat 2 9 Russell St Reading RG1 9CD";
    final String DEFENDANT_EMAIL = "test@hotmail.com";
    final String DEFENDANT_CONTACT_NUM = "02033827384";
    final String ORIGINAL_DATE_OF_SENTENCE = "27-08-2023";

    @Spy
    private Enveloper envelopers = createEnveloper();
    final String subJect = "APPLICATION FOR A STATUTORY DECLARATION RECEIVED (COMMISSIONER OF OATHS)";;

    @BeforeEach
    public void setUp() {
        initiatedEvent
                = new EnforcementPendingApplicationNotificationRequired
                (CASE_ID, APP_ID, INITIATED_TIME, GOB_ACCOUNT_NUMBER, DEF_NAME, URN, DIV_CODE, LISTED_DATE, DEFENDANT_ADDRESS,
                        DEFENDANT_DATE_OF_BIRTH, DEFENDANT_EMAIL, ORIGINAL_DATE_OF_SENTENCE, DEFENDANT_CONTACT_NUM);
    }

    @Test
    public void shouldStoreMetadataInFileServer() throws FileServiceException {

        final JsonObject jsonObject = mock(JsonObject.class);
        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope(EVENT_NAME, jsonObject);
        CaseDetails caseDetails = mock(CaseDetails.class);
        CaseApplication caseApplication = mock(CaseApplication.class);
        when(caseDetails.getCaseApplication()).thenReturn(caseApplication);
        when(caseApplication.getApplicationType()).thenReturn(STAT_DEC);
        when(sjpService.getCaseDetailsByApplicationId(initiatedEvent.getApplicationId(), privateEvent)).thenReturn(caseDetails);
        
        service.generateNotification(initiatedEvent, privateEvent);

        assertThat(fileStorer.getAll(), hasSize(1));
        final JsonObject metadata = fileStorer.getAll().get(0).getKey();
        assertThat(metadata.size(), is(3));
        assertThat(metadata.getString("fileName"), equalTo(fileName(APP_ID)));
        assertThat(metadata.getString("conversionFormat"), equalTo("pdf"));
        assertThat(metadata.getString("templateName"), equalTo(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION.getValue()));
    }

    @Test
    public void shouldStoreTemplateDataForNoticeGenerationFileServer() throws FileServiceException {
        final JsonObject jsonObject = mock(JsonObject.class);
        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope(EVENT_NAME, jsonObject);
        CaseDetails caseDetails = mock(CaseDetails.class);
        CaseApplication caseApplication = mock(CaseApplication.class);
        when(caseDetails.getCaseApplication()).thenReturn(caseApplication);
        when(caseApplication.getApplicationType()).thenReturn(STAT_DEC);
        when(sjpService.getCaseDetailsByApplicationId(initiatedEvent.getApplicationId(), privateEvent)).thenReturn(caseDetails);
        service.generateNotification(initiatedEvent, privateEvent);

        final EnforcementPendingApplicationNotificationTemplateData templateData = getTemplateData(fileStorer.getAll().get(0));
        assertThat(templateData, notNullValue());
        assertThat(templateData.getDateApplicationIsListed(), equalTo(LISTED_DATE));
        assertThat(templateData.getCaseReference(), equalTo(URN));
        assertThat(templateData.getDivisionCode(), equalTo(DIV_CODE));
        assertThat(templateData.getDefendantName(), equalTo(DEF_NAME));
        assertThat(templateData.getGobAccountNumber(), equalTo(GOB_ACCOUNT_NUMBER));
        assertThat(templateData.getTitle(), equalTo(subJect));
    }

    @Test
    public void shouldRequestPdfGenerationOnSystemDocGenerator() throws FileServiceException {
        final JsonObject jsonObject = mock(JsonObject.class);
        final JsonEnvelope privateEvent = EnvelopeFactory.createEnvelope(EVENT_NAME, jsonObject);
        CaseDetails caseDetails = mock(CaseDetails.class);
        CaseApplication caseApplication = mock(CaseApplication.class);
        when(caseDetails.getCaseApplication()).thenReturn(caseApplication);
        when(caseApplication.getApplicationType()).thenReturn(STAT_DEC);
        when(sjpService.getCaseDetailsByApplicationId(initiatedEvent.getApplicationId(), privateEvent)).thenReturn(caseDetails);
        service.generateNotification(initiatedEvent, privateEvent);

        final DocumentGenerationRequest request = systemDocGenerator.getDocumentGenerationRequest(privateEvent);
        assertThat(request.getOriginatingSource(), equalTo("sjp"));
        assertThat(request.getTemplateIdentifier(), equalTo(ENFORCEMENT_PENDING_APPLICATION_NOTIFICATION));
        assertThat(request.getConversionFormat(), equalTo(ConversionFormat.PDF));
        assertThat(request.getSourceCorrelationId(), equalTo(APP_ID.toString()));
    }

    @Test
    public void shouldBuildEmailSubjectWithStatDecApplicationType() {
        final String subject = service.getEmailSubject(STAT_DEC);
        assertThat(subject, equalTo(STAT_DECS_EMAIL_SUBJECT));
    }

    @Test
    public void shouldBuildEmailSubjectWithReopeningApplicationType() {
        final String subject = service.getEmailSubject(REOPENING);
        assertThat(subject, equalTo(REOPENING_EMAIL_SUBJECT));
    }

    @Test
    public void shouldBuildEmailSubjectWithOtherApplicationType() {
        final String errorMessage = String.format("Invalid Application Type, unable to derive email subject for application type: %s", null);
        var e = assertThrows(IllegalStateException.class, () -> service.getEmailSubject(null));
        assertThat(e.getMessage(), CoreMatchers.is(errorMessage));
    }

    private EnforcementPendingApplicationNotificationTemplateData getTemplateData(final Pair<JsonObject, InputStream> fileStoreEntry) {
        final JsonObject fileContent = JsonObjectConversionHelper.streamToJsonObject(fileStoreEntry.getValue());
        return jsonObjectToObjectConverter.convert(fileContent, EnforcementPendingApplicationNotificationTemplateData.class);
    }

    private String fileName(final UUID applicationId) {
        return String.format("enforcement-pending-application-%s.pdf", applicationId);
    }
}