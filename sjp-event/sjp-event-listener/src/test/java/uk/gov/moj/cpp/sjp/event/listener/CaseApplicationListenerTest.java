package uk.gov.moj.cpp.sjp.event.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseApplicationForReopeningRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.CaseStatDecRecorded;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseApplicationService;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.time.LocalDate;
import java.util.UUID;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.persistence.entity.ApplicationStatus.*;
import static uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType.REOPENING;
import static uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType.STAT_DEC;

import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class CaseApplicationListenerTest {

    @InjectMocks
    private CaseApplicationListener listener;

    @Mock
    private CaseService caseService;

    @Mock
    private CaseApplicationService caseApplicationService;

    @Captor
    private ArgumentCaptor<CaseDetail> caseDetailArgumentCaptor;

    @Captor
    private ArgumentCaptor<CaseApplication> caseApplicationArgumentCaptor;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private final static UUID CASE_ID = randomUUID();

    private final static UUID APPLICATION_ID = randomUUID();
    private static final String STAT_DEC_CODE = "MC80528";
    private static final String REOPENING_CODE = "MC80524";
    private static final String APPLICATION_RECEIVED_DATE = LocalDate.now().minusDays(1).toString();
    private final static CourtApplication STAT_DEC_TYPE_APP = CourtApplication.courtApplication()
            .withId(APPLICATION_ID)
            .withApplicationStatus(ApplicationStatus.DRAFT)
            .withApplicationReceivedDate(APPLICATION_RECEIVED_DATE)
            .withType(CourtApplicationType.courtApplicationType()
                    .withId(UUID.randomUUID())
                    .withType("Appearance to make statutory declaration (SJP case)")
                    .withCode(STAT_DEC_CODE)
                    .withAppealFlag(false)
                    .build())
            .withApplicant(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .withSubject(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .build();

    private final static CourtApplication APPLICATION_FOR_REOPENING_TYPE_APP = CourtApplication.courtApplication()
            .withId(CASE_ID)
            .withApplicationStatus(ApplicationStatus.DRAFT)
            .withApplicationReceivedDate(APPLICATION_RECEIVED_DATE)
            .withType(CourtApplicationType.courtApplicationType()
                    .withId(APPLICATION_ID)
                    .withType("Application to reopen case")
                    .withCode(REOPENING_CODE)
                    .withAppealFlag(false)
                    .build())
            .withApplicant(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .withSubject(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .build();

    private CaseDetail caseDetail;

    private CaseApplication caseApplication;

    @BeforeEach
    public void setUp() {
        caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseApplication = new CaseApplication();
        caseApplication.setApplicationId(APPLICATION_ID);
        caseApplication.setCaseDetail(caseDetail);
        caseApplication.setDateReceived(LocalDate.now().minusDays(4));

    }

    @Test
    public void shouldHandleCaseApplicationEvents() {
        assertThat(CaseApplicationListener.class, isHandlerClass(EVENT_LISTENER)
                .with(method("handleCaseApplicationRecorded").thatHandles("sjp.events.case-application-recorded"))
                .with(method("handleStatDecRecorded").thatHandles("sjp.events.case-stat-dec-recorded"))
                .with(method("handleReopeningRecorded").thatHandles("sjp.events.case-application-for-reopening-recorded"))
        );
    }

    @Test
    public void shouldSaveApplicationDetails() {
        final JsonObject jsonObject  = mock(JsonObject.class);
        when(caseService.findById(CASE_ID)).thenReturn(caseDetail);
        when(objectToJsonObjectConverter.convert(STAT_DEC_TYPE_APP)).thenReturn(jsonObject);
        final CaseApplicationRecorded caseApplicationRecorded = new CaseApplicationRecorded.Builder()
                .withCourtApplication(STAT_DEC_TYPE_APP)
                .withCaseId(CASE_ID)
                .build();

        final Envelope<CaseApplicationRecorded> applicationRecordedEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-application-recorded"),
                caseApplicationRecorded);

        listener.handleCaseApplicationRecorded(applicationRecordedEnvelope);
        verify(caseService).saveCaseDetail(caseDetailArgumentCaptor.capture());

        final CaseDetail caseDetail = caseDetailArgumentCaptor.getValue();
        assertThat(caseDetail.getCurrentApplication(), notNullValue());
        final CaseApplication caseApplication = caseDetail.getCurrentApplication();
        assertThat(caseApplication.getApplicationId(), equalTo(APPLICATION_ID));
        assertThat(caseApplication.getCaseDetail(), equalTo(caseDetail));
        assertThat(caseApplication.getTypeId(), equalTo(STAT_DEC_TYPE_APP.getType().getId()));
        assertThat(caseApplication.getTypeCode(), equalTo(STAT_DEC_TYPE_APP.getType().getCode()));
        assertThat(caseApplication.getApplicationStatus().name(), equalTo(STAT_DEC_TYPE_APP.getApplicationStatus().name()));
        assertThat(caseApplication.isOutOfTime(), is(false));
        assertThat(caseApplication.getOutOfTimeReason(), equalTo(caseApplicationRecorded.getCourtApplication().getOutOfTimeReasons()));
        assertThat(caseApplication.getDateReceived(), equalTo(LocalDate.parse(caseApplicationRecorded.getCourtApplication().getApplicationReceivedDate())));
        assertThat(caseApplication.getInitiatedApplication(), equalTo(jsonObject));
    }

    @Test
    public void shouldRecordStatDecDetails() {
        when(caseApplicationService.findById(APPLICATION_ID)).thenReturn(caseApplication);

        final CaseStatDecRecorded caseStatDecRecorded = new CaseStatDecRecorded.Builder()
                .withApplicationId(APPLICATION_ID)
                .withApplicant(STAT_DEC_TYPE_APP.getApplicant())
                .build();

        final Envelope<CaseStatDecRecorded> statDecRecordedEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-stat-dec-recorded"),
                caseStatDecRecorded);

        listener.handleStatDecRecorded(statDecRecordedEnvelope);

        verify(caseApplicationService).saveCaseApplication(caseApplicationArgumentCaptor.capture());
        final CaseApplication caseApplication = caseApplicationArgumentCaptor.getValue();

        assertThat(caseApplication.getApplicationType(), equalTo(STAT_DEC));

    }

    @Test
    public void shouldRecordReopeningDetails() {
        when(caseApplicationService.findById(APPLICATION_ID)).thenReturn(caseApplication);

        final CaseApplicationForReopeningRecorded reopeningRecorded = new CaseApplicationForReopeningRecorded.Builder()
                .withApplicationId(APPLICATION_ID)
                .withApplicant(APPLICATION_FOR_REOPENING_TYPE_APP.getApplicant())
                .build();

        final Envelope<CaseApplicationForReopeningRecorded> reopeningEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.case-application-for-reopening-recorded"),
                reopeningRecorded);

        listener.handleReopeningRecorded(reopeningEnvelope);

        verify(caseApplicationService).saveCaseApplication(caseApplicationArgumentCaptor.capture());
        final CaseApplication caseApplication = caseApplicationArgumentCaptor.getValue();

        assertThat(caseApplication.getApplicationType(), equalTo(REOPENING));

    }

    @Test
    public void shouldUpdateApplicationStatus() {
        when(caseApplicationService.findById(APPLICATION_ID)).thenReturn(caseApplication);

        final ApplicationStatusChanged statusChanged =
                new ApplicationStatusChanged(APPLICATION_ID,
                        uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_PENDING);

        final Envelope<ApplicationStatusChanged> statusChangedEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.application-status-changed"),
                statusChanged
        );

        listener.handleApplicationStatusChanged(statusChangedEnvelope);
        verify(caseApplicationService).saveCaseApplication(caseApplicationArgumentCaptor.capture());
        final CaseApplication caseApplication = caseApplicationArgumentCaptor.getValue();

        assertThat(caseApplication.getApplicationStatus(),
                equalTo(REOPENING_PENDING));
    }

    @Test
    public void shouldSetCaseAsideOnApplicationSetAside() {
        caseDetail.setCompleted(true);
        caseDetail.setSetAside(false);

        when(caseService.findById(CASE_ID)).thenReturn(caseDetail);

        final ApplicationDecisionSetAside applicationSetAside = new ApplicationDecisionSetAside(APPLICATION_ID, CASE_ID, caseDetail.getUrn());
        final Envelope<ApplicationDecisionSetAside> envelope = envelopeFrom(
                metadataWithRandomUUID(ApplicationDecisionSetAside.EVENT_NAME),
                applicationSetAside
        );

        listener.handleApplicationDecisionSetAside(envelope);

        verify(caseService).saveCaseDetail(caseDetail);
        assertThat(caseDetail.getSetAside(), is(true));
        assertThat(caseDetail.isCompleted(), is(false));
    }

}
