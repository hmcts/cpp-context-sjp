package uk.gov.moj.cpp.sjp.event.processor.service.notification;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.REOPENING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.justice.json.schemas.domains.sjp.queries.DisqualificationType.POINTS;
import static uk.gov.justice.services.test.utils.core.converter.JsonObjectToObjectConverterFactory.createJsonObjectToObjectConverter;
import static uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.TemplateIdentifier.NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.DisqualificationType;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryOffenceDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.Session;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.core.sjp.decision.DecisionType;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.processor.helper.JsonObjectConversionHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.LocalJusticeArea;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDetailsDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.ConversionFormat;
import uk.gov.moj.cpp.sjp.event.processor.service.systemdocgenerator.DocumentGenerationRequest;
import uk.gov.moj.cpp.sjp.event.processor.utils.builders.ApplicationDecisionSetAsideEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.utils.fake.FakeFileStorer;
import uk.gov.moj.cpp.sjp.event.processor.utils.fake.FakeSystemDocGenerator;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EndorsementRemovalNotificationServiceTest {

    private static final String OFFENCE_CODE = "OFFENCE_CODE";
    private static final LocalDate OFFENCE_START_DATE = LocalDate.of(2020, 1, 1);
    private static final String OFFENCE_TITLE = "Offence title";
    private static final String LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE = "2577";
    private static final ZonedDateTime ORIGINAL_CONVICTION_DATE = ZonedDateTime.of(2020, 1, 2, 0, 0, 0, 0, ZoneId.systemDefault());
    private static final ZonedDateTime DECISION_SAVED_AT = now();
    private static final int POINTS_DISQUALIFICATION = 0;
    private static final int DISCRETIONARY_DISQUALIFICATION = 1;
    private static final LocalJusticeArea LOCAL_JUSTICE_AREA = new LocalJusticeArea("LJA Code", "LJA Name");
    public static final String CASE_URN = "TVLXYZ01";

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
    private EndorsementRemovalNotificationService service;

    private ApplicationDecisionSetAside applicationDecisionSetAside;
    private UUID caseId;
    private UUID sessionId;
    private UUID applicationId;
    private UUID applicationDecisionId;
    private Session session;
    private JsonEnvelope envelope;
    private ZonedDateTime applicationSavedAt;
    private CaseDetailsDecorator caseDetails;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID();
        sessionId = randomUUID();
        applicationId = randomUUID();
        applicationSavedAt = ZonedDateTime.of(LocalDate.of(2020, 11, 20), LocalTime.now(), ZoneId.systemDefault());
        applicationDecisionId = randomUUID();
        applicationDecisionSetAside = new ApplicationDecisionSetAside(applicationId, caseId, CASE_URN);
        envelope = ApplicationDecisionSetAsideEnvelope.of(applicationDecisionSetAside);
    }

    @Test
    public void shouldStoreMetadataInFileServer() throws FileServiceException {
        givenCaseWithEndorsementsToBeRemoved();
        givenEnforcementAreaPresentInReferenceData();
        givenOffenceMetadataIsPresentInReferenceData();

        service.generateNotification(caseDetails, envelope);

        assertThat(fileStorer.getAll(), hasSize(1));
        final JsonObject metadata = fileStorer.getAll().get(0).getKey();
        assertThat(metadata.size(), is(3));
        assertThat(metadata.getString("fileName"), equalTo(format("notification-to-dvla-to-remove-endorsement-%s.pdf", applicationDecisionId)));
        assertThat(metadata.getString("conversionFormat"), equalTo("pdf"));
        assertThat(metadata.getString("templateName"), equalTo(NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT.getValue()));
    }

    @Test
    public void shouldStoreTemplateDataForNoticeGenerationFileServer() throws FileServiceException {
        givenCaseWithEndorsementsToBeRemoved();
        givenEnforcementAreaPresentInReferenceData();
        givenOffenceMetadataIsPresentInReferenceData();

        service.generateNotification(caseDetails, envelope);

        final EndorsementRemovalNotificationTemplateData templateData = getTemplateData(fileStorer.getAll().get(0));
        assertThat(templateData, notNullValue());
        assertThat(templateData.getDateOfOrder(), equalTo("20 November 2020"));
        assertThat(templateData.getLjaCode(), equalTo(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE));
        assertThat(templateData.getLjaName(), equalTo("LJA Name"));
        assertThat(templateData.getCaseUrn(), equalTo("Case URN"));
        assertThat(templateData.getDefendantName(), equalTo("Robert Robertson"));
        assertThat(templateData.getDefendantAddress(), equalTo("line1, line2, POSTCODE"));
        assertThat(templateData.getDefendantDateOfBirth(), equalTo("21 May 1980"));
        assertThat(templateData.getDefendantGender(), equalTo("Male"));
        assertThat(templateData.getDefendantDriverNumber(), equalTo("Driver Number"));
        assertThat(templateData.getReasonForIssue(), equalTo("Statutory declaration accepted"));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved(), hasSize(1));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved().get(0).getOffenceTitle(), equalTo(OFFENCE_TITLE));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved().get(0).getOriginalCourtCode(), equalTo(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved().get(0).getOriginalConvictionDate(), equalTo(ORIGINAL_CONVICTION_DATE.toLocalDate().toString()));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved().get(0).getDvlaEndorsementCode(), equalTo("Dvla code"));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved().get(0).getOriginalOffenceDate(), equalTo(OFFENCE_START_DATE.toString()));
    }

    @Test
    public void shouldStoreTemplateDataForNoticeGenerationFileServerWithUnknownDefendantDateOfBirth() throws FileServiceException {
        final Defendant.Builder defendant = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withFirstName("Robert")
                .withLastName("Robertson")
                .withDateOfBirth(null)
                .build());
        givenCaseWithEndorsementsToBeRemoved(defendant);
        givenEnforcementAreaPresentInReferenceData();
        givenOffenceMetadataIsPresentInReferenceData();

        service.generateNotification(caseDetails, envelope);

        final EndorsementRemovalNotificationTemplateData templateData = getTemplateData(fileStorer.getAll().get(0));
        assertThat(templateData.getDefendantDateOfBirth(), equalTo("unknown"));
    }

    @Test
    public void shouldGetConvictionDateFromOffenceDecisionSavedAtIfConvictionDateIsNotPresentInTheOffence() throws FileServiceException {
        givenCaseWithEndorsementsToBeRemovedAndNoPreviousConvictionDate();
        givenEnforcementAreaPresentInReferenceData();
        givenOffenceMetadataIsPresentInReferenceData();

        service.generateNotification(caseDetails, envelope);

        final EndorsementRemovalNotificationTemplateData templateData = getTemplateData(fileStorer.getAll().get(0));
        assertThat(templateData, not(nullValue()));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved(), hasSize(1));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved().get(0).getOriginalConvictionDate(), equalTo(DECISION_SAVED_AT.toLocalDate().toString()));
    }

    @Test
    public void shouldNotSendOffencesWithNoPenaltyPointsApplied() throws FileServiceException {
        givenCaseWithLicenceEndorsements();
        givenEnforcementAreaPresentInReferenceData();
        givenOffenceMetadataIsPresentInReferenceData();

        service.generateNotification(caseDetails, envelope);

        final EndorsementRemovalNotificationTemplateData templateData = getTemplateData(fileStorer.getAll().get(0));
        assertThat(templateData, not(nullValue()));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved(), hasSize(3));
    }


    @Test
    public void shouldSendDvlaCodeForEachOffencesWithDisqualificationBasedOnDisqualificationType() throws FileServiceException {
        givenCaseWithDisqualifications();
        givenEnforcementAreaPresentInReferenceData();
        givenOffenceMetadataIsPresentInReferenceData();

        service.generateNotification(caseDetails, envelope);

        final EndorsementRemovalNotificationTemplateData templateData = getTemplateData(fileStorer.getAll().get(0));
        assertThat(templateData, not(nullValue()));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved(), hasSize(2));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved().get(POINTS_DISQUALIFICATION).getDvlaEndorsementCode(), is("TT99"));
        assertThat(templateData.getDrivingEndorsementsToBeRemoved().get(DISCRETIONARY_DISQUALIFICATION).getDvlaEndorsementCode(), is("Dvla code"));
    }

    @Test
    public void shouldHandleMultipleApplicationDecisionsSendingEndorsementsForTheLatestOnly() throws FileServiceException {
        givenCaseWithEndorsementsInMultipleApplications();
        givenEnforcementAreaPresentInReferenceData();
        givenOffenceMetadataIsPresentInReferenceData();

        service.generateNotification(caseDetails, envelope);

        final EndorsementRemovalNotificationTemplateData templateData = getTemplateData(fileStorer.getAll().get(0));
        assertThat(templateData, not(nullValue()));
        assertThat(templateData.getReasonForIssue(), is("Case re-opened under section 142"));
    }

    @Test
    public void shouldRequestPdfGenerationOnSystemDocGenerator() throws FileServiceException {
        givenCaseWithEndorsementsToBeRemoved();
        givenEnforcementAreaPresentInReferenceData();
        givenOffenceMetadataIsPresentInReferenceData();

        service.generateNotification(caseDetails, envelope);

        final DocumentGenerationRequest request = systemDocGenerator.getDocumentGenerationRequest(envelope);
        assertThat(request.getOriginatingSource(), equalTo("sjp"));
        assertThat(request.getTemplateIdentifier(), equalTo(NOTIFICATION_TO_DVLA_TO_REMOVE_ENDORSEMENT));
        assertThat(request.getConversionFormat(), equalTo(ConversionFormat.PDF));
        assertThat(request.getSourceCorrelationId(), equalTo(applicationDecisionId.toString()));
    }

    @Test
    public void shouldBuildSubjectWithAllArguments() {
        final Defendant.Builder defendant = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withFirstName("Robert")
                .withLastName("Robertson")
                .withDateOfBirth(LocalDate.of(1980, 5, 21))
                .build());
        givenCaseWithEndorsementsToBeRemoved(defendant);
        givenEnforcementAreaPresentInReferenceData();

        final String subject = service.buildEmailSubject(caseDetails.getCurrentApplicationDecision().get(), envelope);

        assertThat(subject, equalTo("Notification to DVLA to Remove Endorsement: LJA Name; Robert Robertson; 21 May 1980; Case URN:"));
    }

    @Test
    public void shouldBuildSubjectWithUnknownDefendantDateOfBirth() {
        final Defendant.Builder defendant = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withFirstName("Robert")
                .withLastName("Robertson")
                .withDateOfBirth(null)
                .build());
        givenCaseWithEndorsementsToBeRemoved(defendant);
        givenEnforcementAreaPresentInReferenceData();

        final String subject = service.buildEmailSubject(caseDetails.getCurrentApplicationDecision().get(), envelope);

        assertThat(subject, equalTo("Notification to DVLA to Remove Endorsement: LJA Name; Robert Robertson; unknown; Case URN:"));
    }

    private Offence.Builder createOffence() {
        return Offence.offence()
                .withId(randomUUID())
                .withOffenceCode(OFFENCE_CODE)
                .withStartDate(OFFENCE_START_DATE.toString());
    }

    private CaseDecision statDecGrantedDecision(final ZonedDateTime previousFinalDecision) {
        return CaseDecision.caseDecision()
                .withId(applicationDecisionId)
                .withSavedAt(applicationSavedAt)
                .withApplicationDecision(applicationDecision(previousFinalDecision).build())
                .withSession(session)
                .build();
    }

    private CaseDecision.Builder caseDecision() {
        return CaseDecision.caseDecision()
                .withId(randomUUID())
                .withSavedAt(now())
                .withSession(session);
    }

    private CaseDecision.Builder financialPenaltyDecision(final UUID offenceId) {
        return CaseDecision.caseDecision()
                .withId(randomUUID())
                .withSavedAt(now())
                .withOffenceDecisions(singletonList(
                        financialPenaltyWithEndorsements(offenceId, ORIGINAL_CONVICTION_DATE.toLocalDate())))
                .withSession(session);
    }

    private QueryOffenceDecision financialPenaltyWithEndorsements(final UUID offenceId, final LocalDate originalConvictionDate) {
        return QueryOffenceDecision.queryOffenceDecision()
                .withOffenceId(offenceId)
                .withDecisionType(DecisionType.FINANCIAL_PENALTY)
                .withLicenceEndorsement(true)
                .withPenaltyPointsImposed(10)
                .withConvictionDate(originalConvictionDate)
                .build();
    }

    private void startSession() {
        session = Session.session()
                .withSessionId(sessionId)
                .withCourtHouseCode("Court House Code")
                .withCourtHouseName("Court House Name")
                .withLocalJusticeAreaNationalCourtCode(LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE)
                .build();
    }

    private QueryApplicationDecision.Builder applicationDecision(final ZonedDateTime previousFinalDecisionSavedAt) {
        return QueryApplicationDecision.queryApplicationDecision()
                .withApplicationType(ApplicationType.STAT_DEC)
                .withOutOfTime(false)
                .withGranted(true)
                .withPreviousFinalDecision(previousFinalDecisionSavedAt);
    }

    private QueryOffenceDecision.Builder financialPenalty(final UUID offenceId) {
        return QueryOffenceDecision.queryOffenceDecision()
                .withOffenceId(offenceId)
                .withDecisionType(DecisionType.FINANCIAL_PENALTY)
                .withConvictionDate(LocalDate.now());
    }

    private CaseDetails.Builder createCase(final Defendant.Builder defendantBuilder) {
        return createCase(defendantBuilder.build().getOffences()).withDefendant(defendantBuilder.build());
    }

    private CaseDetails.Builder createCase(final List<Offence> offences) {
        final CaseDetails.Builder builder = CaseDetails.caseDetails()
                .withId(caseId)
                .withUrn("Case URN")
                .withDefendant(Defendant.defendant()
                        .withPersonalDetails(PersonalDetails.personalDetails()
                                .withFirstName("Robert")
                                .withLastName("Robertson")
                                .withDateOfBirth(LocalDate.of(1980, 5, 21))
                                .withGender(Gender.MALE)
                                .withDriverNumber("Driver Number")
                                .withAddress(Address.address()
                                        .withAddress1("line1")
                                        .withAddress2("line2")
                                        .withPostcode("POSTCODE")
                                        .build())
                                .build())
                        .withOffences(offences)
                        .build());

        final CaseDecision caseDecision1 = financialPenaltyDecision(offences.get(0).getId()).build();
        final CaseDecision caseDecision2 = statDecGrantedDecision(caseDecision1.getSavedAt());
        builder.withCaseDecisions(asList(caseDecision1, caseDecision2));

        return builder;
    }

    private void givenEnforcementAreaPresentInReferenceData() {
        when(referenceDataService.getLocalJusticeAreaByCode(envelope, LOCAL_JUSTICE_AREA_NATIONAL_COURT_CODE))
                .thenReturn(LOCAL_JUSTICE_AREA);
    }

    private void givenOffenceMetadataIsPresentInReferenceData() {
        final JsonObject response = createObjectBuilder()
                .add("title", OFFENCE_TITLE)
                .add("dvlaCode", "Dvla code")
                .build();
        when(referenceDataOffencesService.getOffenceReferenceData(envelope, OFFENCE_CODE, OFFENCE_START_DATE.toString()))
                .thenReturn(Optional.of(response));
    }

    private void givenCaseWithEndorsementsToBeRemoved() {
        startSession();

        final Offence offence = createOffence().build();
        caseDetails = new CaseDetailsDecorator(createCase(singletonList(offence)).build());
    }

    private void givenCaseWithEndorsementsToBeRemoved(final Defendant.Builder defendant) {
        startSession();

        final Offence offence = createOffence().build();
        defendant.withOffences(singletonList(offence));
        caseDetails = new CaseDetailsDecorator(createCase(defendant).build());
    }

    private void givenCaseWithLicenceEndorsements() {
        startSession();

        final Offence offence1 = createOffence().build();
        final Offence offence2 = createOffence().build();
        final Offence offence3 = createOffence().build();

        final CaseDetails.Builder caseBuilder = createCase(asList(offence1, offence2, offence3));

        final CaseDecision caseDecision1 = caseDecision()
                .withOffenceDecisions(asList(
                        financialPenalty(offence1.getId()).withLicenceEndorsement(true).withPenaltyPointsImposed(10).build(),
                        financialPenalty(offence2.getId()).withLicenceEndorsement(true).withPenaltyPointsImposed(0).build(),
                        financialPenalty(offence3.getId()).withLicenceEndorsement(true).withPenaltyPointsImposed(null).build()
                )).build();

        final CaseDecision caseDecision2 = caseDecision()
                .withApplicationDecision(applicationDecision(caseDecision1.getSavedAt()).build())
                .build();

        caseBuilder.withCaseDecisions(asList(caseDecision1, caseDecision2));

        caseDetails = new CaseDetailsDecorator(caseBuilder.build());
    }

    private void givenCaseWithDisqualifications() {
        startSession();

        final Offence offence1 = createOffence().build();
        final Offence offence2 = createOffence().build();
        final Offence offence3 = createOffence().build();

        final CaseDetails.Builder caseBuilder = createCase(asList(offence1, offence2, offence3));

        final CaseDecision caseDecision1 = caseDecision()
                .withOffenceDecisions(asList(
                        financialPenalty(offence1.getId()).withDisqualification(true).withDisqualificationType(POINTS).build(),
                        financialPenalty(offence2.getId()).withDisqualification(true).withDisqualificationType(DisqualificationType.DISCRETIONARY).build(),
                        financialPenalty(offence3.getId()).withDisqualification(false).withDisqualificationType(null).build()
                )).build();

        final CaseDecision caseDecision2 = caseDecision()
                .withApplicationDecision(applicationDecision(caseDecision1.getSavedAt()).build())
                .build();

        caseBuilder.withCaseDecisions(asList(caseDecision1, caseDecision2));

        caseDetails = new CaseDetailsDecorator(caseBuilder.build());
    }

    private void givenCaseWithEndorsementsInMultipleApplications() {
        startSession();

        final Offence offence1 = createOffence().build();
        final Offence offence2 = createOffence().build();
        final Offence offence3 = createOffence().build();

        final CaseDetails.Builder caseBuilder = createCase(asList(offence1, offence2, offence3));

        final CaseDecision caseDecision1 = caseDecision()
                .withOffenceDecisions(asList(
                        financialPenalty(offence1.getId()).withLicenceEndorsement(true).withPenaltyPointsImposed(10).build(),
                        financialPenalty(offence2.getId()).withLicenceEndorsement(true).withPenaltyPointsImposed(0).build(),
                        financialPenalty(offence3.getId()).withLicenceEndorsement(true).withPenaltyPointsImposed(null).build()
                )).build();

        final CaseDecision caseDecision2 = caseDecision()
                .withApplicationDecision(
                        applicationDecision(caseDecision1.getSavedAt())
                                .withGranted(false)
                                .build()
                )
                .withSavedAt(caseDecision1.getSavedAt().plusHours(1))
                .build();

        final CaseDecision caseDecision3 = caseDecision()
                .withApplicationDecision(
                        applicationDecision(caseDecision1.getSavedAt())
                                .withGranted(true)
                                .withApplicationType(REOPENING)
                                .build()
                )
                .withSavedAt(caseDecision1.getSavedAt().plusHours(2))
                .build();

        caseBuilder.withCaseDecisions(asList(caseDecision1, caseDecision2, caseDecision3));

        caseDetails = new CaseDetailsDecorator(caseBuilder.build());
    }

    private void givenCaseWithEndorsementsToBeRemovedAndNoPreviousConvictionDate() {
        startSession();

        final Offence offence = createOffence().build();

        final CaseDecision caseDecision1 = financialPenaltyDecision(offence.getId())
                .withOffenceDecisions(singletonList(financialPenaltyWithEndorsements(offence.getId(), null)))
                .withSavedAt(DECISION_SAVED_AT)
                .build();

        final CaseDecision caseDecision2 = caseDecision()
                .withApplicationDecision(
                        applicationDecision(caseDecision1.getSavedAt())
                                .withGranted(true)
                                .withApplicationType(STAT_DEC)
                                .build()
                )
                .withSavedAt(caseDecision1.getSavedAt().plusHours(2))
                .build();

        this.caseDetails = new CaseDetailsDecorator(createCase(singletonList(offence))
                .withCaseDecisions(asList(caseDecision1, caseDecision2))
                .build());

    }

    private EndorsementRemovalNotificationTemplateData getTemplateData(final Pair<JsonObject, InputStream> fileStoreEntry) {
        final JsonObject fileContent = JsonObjectConversionHelper.streamToJsonObject(fileStoreEntry.getValue());
        return jsonObjectToObjectConverter.convert(fileContent, EndorsementRemovalNotificationTemplateData.class);
    }
}
