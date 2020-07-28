package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.lang.String.valueOf;
import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails.caseDetails;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;
import static uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing.caseReferredForCourtHearing;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.Interpreter;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.PleaType;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.AddressView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ContactView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.EmployerOrganisationView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.OffenceFactsView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.OffenceView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDetailsView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCasesViewHelperTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID CASE_URN = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID PROSECUTOR_ID = randomUUID();
    private static final UUID OFFENCE_DEFINITION_ID = randomUUID();
    private static final UUID OFFENCE_ID1 = randomUUID();
    private static final UUID OFFENCE_ID2 = randomUUID();
    private static final ZonedDateTime DECISION_DATE = ZonedDateTime.now();
    private static final String PROSECUTOR_CODE = "TFL";
    private static final String OFFENCE_CJS_CODE1 = "offence CJS code";
    private static final String OFFENCE_CJS_CODE2 = "offence CJS code";
    private static final String LANGUAGE_E = "ENGLISH";
    private static final String LANGUAGE_W = "WELSH";
    private static final String LANGUAGE_X = "Japanese";
    private static final String OFFENCE_MITIGATION = "I was not there";

    private static final int DEFENDANT_NUM_PREVIOUS_CONVICTIONS = 1;

    private static final String DEFENDANT_NATIONALITY_CODE = "defendantNationality";
    private static final String DEFENDANT_NATIONALITY_ID = randomUUID().toString();
    private static final String DEFENDANT_ETHNICITY = "defendantEthnicity";
    private static final String DEFENDANT_ETHNICITY_ID = randomUUID().toString();
    private static final String DEFENDANT_OCCUPATION = "defendantOccupation";
    private static final int DEFENDANT_OCCUPATION_CODE = 1;
    private static final String NATIONAL_INSURANCE_NUMBER = "NINO";
    private static final String DEFENDANT_SPECIFIC_REQUIREMENTS = "defendantSpecificRequirements";
    private static final String EMPLOYER_NAME = "Employer Name";
    private static final String STATEMENT_OF_FACTS_WELSH = "welsh facts";
    private static final String DEFENDANT_WORK_PHONE = "defendant work phone";
    private static final String DEFENDANT_SECONDARY_EMAIL = "defendant secondary email";
    private static final LocalDate OFFENCE_END_DATE = LocalDate.now().plusDays(20);
    private static final ZonedDateTime PLEA_DATE = ZonedDateTime.now();
    private static final String PROSECUTION_CASE_REFERENCE = "2OknbZ5Xl4";
    private static final VerdictType NULL_VERDICT = null;
    private static final String DISABILITY_NEEDS = "Hearing aid";

    private ProsecutionCasesViewHelper prosecutionCasesViewHelper = new ProsecutionCasesViewHelper();

    @Test
    public void shouldCreateProsecutionCaseViewsWithoutConvictionDateWhenVerdictNotPresent() {
        createProsecutionCaseViewsAndVerifyResultCorrect(null, createCaseFileDefendantDetails(), createEmployer(), VerdictType.NO_VERDICT);
    }

    @Test
    public void shouldCreateProsecutionCaseViewsWithoutConvictionDateWhenNoVerdict() {
        createProsecutionCaseViewsAndVerifyResultCorrect(null, createCaseFileDefendantDetails(), createEmployer(), VerdictType.NO_VERDICT);
    }

    @Test
    public void shouldCreateProsecutionCaseViewsWithoutConvictionDateWhenVerdictIsNull() {
        createProsecutionCaseViewsAndVerifyResultCorrect(null, createCaseFileDefendantDetails(), createEmployer(), NULL_VERDICT);
    }

    @Test
    public void shouldCreateProsecutionCaseViewsWithConvictionDate() {
        createProsecutionCaseViewsAndVerifyResultCorrect(DECISION_DATE.toLocalDate(), createCaseFileDefendantDetails(), createEmployer(), VerdictType.PROVED_SJP);
    }

    @Test
    public void shouldDefaultToEnglishHearingLanguageWhenCaseFileDetailsNull() {
        createProsecutionCaseViewsAndVerifyResultCorrect(DECISION_DATE.toLocalDate(), null, createEmployer(), VerdictType.PROVED_SJP);
    }

    @Test
    public void shouldNotIncludeEmployerDetailsIfEmployerNameNotPresent() {
        createProsecutionCaseViewsAndVerifyResultCorrect(DECISION_DATE.toLocalDate(), null, EmployerDetails.employerDetails().build(), VerdictType.PROVED_SJP);
    }

    @Test
    public void shouldReplaceEmailAndPhoneNumberWithNullsWhenBlank() {
        List<Offence> offences = createOffences(OFFENCE_ID1, OFFENCE_ID2);

        createProsecutionCaseViewsAndVerifyResultCorrect(DECISION_DATE.toLocalDate(), null, createEmployer(), createDefendantPersonalDetails()
                        .withContactDetails(ContactDetails.contactDetails()
                                .withBusiness(null)
                                .withHome("")
                                .withEmail("")
                                .withEmail2(null)
                                .withMobile("")
                                .build()
                        )
                , VerdictType.FOUND_NOT_GUILTY, offences);
    }

    @Test
    public void shouldCreateProsecutionViewWhenDefendantTitleIsNull() {
        List<Offence> offences = createOffences(OFFENCE_ID1, OFFENCE_ID2);

        createProsecutionCaseViewsAndVerifyResultCorrect(DECISION_DATE.toLocalDate(), null, createEmployer(), createDefendantPersonalDetails().withTitle(null)
                        .withContactDetails(ContactDetails.contactDetails()
                                .withBusiness(null)
                                .withHome("")
                                .withEmail("")
                                .withEmail2(null)
                                .withMobile("")
                                .build()
                        )
                , VerdictType.FOUND_NOT_GUILTY, offences);
    }

    @Test
    public void shouldCreateProsecutionViewWhenDefendantTitleIsNotStandard() {
        List<Offence> offences = createOffences(OFFENCE_ID1, OFFENCE_ID2);

        createProsecutionCaseViewsAndVerifyResultCorrect(DECISION_DATE.toLocalDate(), null, createEmployer(), createDefendantPersonalDetails().withTitle("Doctor")
                        .withContactDetails(ContactDetails.contactDetails()
                                .withBusiness(null)
                                .withHome("")
                                .withEmail("")
                                .withEmail2(null)
                                .withMobile("")
                                .build()
                        )
                , VerdictType.FOUND_NOT_GUILTY, offences);
    }

    @Test
    public void shouldApplyInterpreterSpecifiedByCourtReferrer() {

        final List<Offence> offences = new ArrayList<>();
        offences.add(createOffence(OFFENCE_CJS_CODE1, OFFENCE_ID1, null, 1));

        final PersonalDetails defendantPersonalDetails = createDefendantPersonalDetails().build();
        final CaseDetails caseDetails = createCaseDetails(defendantPersonalDetails, offences);

        final JsonObject prosecutor = createProsecutor();
        final EmployerDetails employer = createEmployer();
        final JsonObject caseFileDefendantDetails = createCaseFileDefendantDetails();

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withReferredAt(DECISION_DATE)
                .withReferredOffences(getReferredOffencesWithVerdict(VerdictType.PROVED_SJP))
                .withDefendantCourtOptions(new DefendantCourtOptions(new DefendantCourtInterpreter(LANGUAGE_X, true),null, disabilityNeedsOf(DISABILITY_NEEDS)))
                .build();

        final List<ProsecutionCaseView> prosecutionCaseViews = prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                prosecutor,
                null,
                caseFileDefendantDetails,
                employer,
                DEFENDANT_NATIONALITY_ID,
                DEFENDANT_ETHNICITY_ID,
                caseReferredForCourtHearing,
                OFFENCE_MITIGATION,
                mockCJSOffenceCodeToOffenceDefinitionId(),
                singletonList(offences.get(0)));

        assertThat(prosecutionCaseViews.size(), is(1));

        final ProsecutionCaseView prosecutionCaseView = prosecutionCaseViews.get(0);

        final DefendantView defendantView = prosecutionCaseView.getDefendants().get(0);

        assertThat(defendantView.getPersonDefendant().getPersonDetails().getDocumentationLanguageNeeds(), is(LANGUAGE_W));
        assertThat(defendantView.getPersonDefendant().getPersonDetails().getInterpreterLanguageNeeds(), is(LANGUAGE_X));
        assertThat(defendantView.getPersonDefendant().getPersonDetails().getDisabilityStatus(), is(DISABILITY_NEEDS));
    }

    @Test
    public void shouldCreateMultipleOffenceViewsAndGetProsecutionFactsIfAvailableOnAnyOffence() {

        final List<Offence> offences = new ArrayList<>();

        offences.add(createOffence(OFFENCE_CJS_CODE1, OFFENCE_ID1, null, 1));
        offences.add(createOffence(OFFENCE_CJS_CODE2, OFFENCE_ID1, null, 2));

        Offence nonReferredOffence = createOffence(OFFENCE_CJS_CODE1, OFFENCE_ID2, "Prosecution facts", 3);
        offences.add(nonReferredOffence);

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withReferredAt(DECISION_DATE)
                .withReferredOffences(getReferredOffencesWithVerdict(VerdictType.PROVED_SJP))
                .build();

        final NotifiedPleaView notifiedPleaView = new NotifiedPleaView(
                offences.get(0).getId(),
                now(),
                "NOTIFIED_GUILTY");

        final PersonalDetails defendantPersonalDetails = createDefendantPersonalDetails().build();
        final CaseDetails caseDetails = createCaseDetails(defendantPersonalDetails, offences);

        final JsonObject prosecutor = createProsecutor();
        final EmployerDetails employer = createEmployer();
        final JsonObject prosecutionCaseFile = createCaseFileDetails();
        final JsonObject caseFileDefendantDetails = createCaseFileDefendantDetails();

        final List<ProsecutionCaseView> prosecutionCaseViews = prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                prosecutor,
                prosecutionCaseFile,
                caseFileDefendantDetails,
                employer,
                nonNull(caseFileDefendantDetails) ? DEFENDANT_NATIONALITY_ID : null,
                DEFENDANT_ETHNICITY_ID,
                caseReferredForCourtHearing,
                OFFENCE_MITIGATION,
                mockCJSOffenceCodeToOffenceDefinitionId(),
                Arrays.asList(offences.get(0), offences.get(1)));

        assertThat(prosecutionCaseViews.size(), is(1));

        final ProsecutionCaseView prosecutionCaseView = prosecutionCaseViews.get(0);
        assertProsecutionView(prosecutionCaseView, nonReferredOffence, prosecutionCaseFile, caseFileDefendantDetails);

        final DefendantView defendantView = prosecutionCaseView.getDefendants().get(0);

        assertDefendantDetailsMatch(defendantPersonalDetails, defendantView, employer, caseFileDefendantDetails, employer, 2);

        final OffenceView offenceView1 = defendantView.getOffences().get(0);
        assetOffenceView(offenceView1, offences.get(0), caseFileDefendantDetails, notifiedPleaView, DECISION_DATE.toLocalDate());

        final OffenceView offenceView2 = defendantView.getOffences().get(1);
        assetOffenceView(offenceView2, offences.get(1), caseFileDefendantDetails, notifiedPleaView, DECISION_DATE.toLocalDate());
    }

    @Test
    public void shouldCreateMultipleOffenceViewsAndGetOffenceFactsWithVehicleMakeAndRegistration() {

        final List<Offence> offences = new ArrayList<>();

        final Offence referredOffence1 = Offence.offence()
                .withCjsCode(OFFENCE_CJS_CODE1)
                .withId(OFFENCE_ID1)
                .withOffenceCode("offence code")
                .withPlea(PleaType.GUILTY_REQUEST_HEARING)
                .withPleaDate(PLEA_DATE)
                .withWording("wording")
                .withWordingWelsh("welsh wording")
                .withStartDate(now().toString())
                .withChargeDate(now().minusDays(5).toString())
                .withOffenceSequenceNumber(1)
                .withVehicleMake("TESTMAKE")
                .withVehicleRegistrationMark("TES61 TTT")
                .build();
        offences.add(referredOffence1);

        final Offence nonReferredOffence = Offence.offence()
                .withCjsCode(OFFENCE_CJS_CODE1)
                .withId(OFFENCE_ID2)
                .withOffenceCode("offence code")
                .withPlea(PleaType.GUILTY_REQUEST_HEARING)
                .withPleaDate(PLEA_DATE)
                .withWording("wording")
                .withWordingWelsh("welsh wording")
                .withStartDate(now().toString())
                .withChargeDate(now().minusDays(5).toString())
                .withProsecutionFacts("Prosecution facts")
                .withOffenceSequenceNumber(3)
                .build();
        offences.add(nonReferredOffence);

        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withReferredAt(DECISION_DATE)
                .withReferredOffences(getReferredOffencesWithVerdict(VerdictType.PROVED_SJP))
                .build();

        final NotifiedPleaView notifiedPleaView = new NotifiedPleaView(
                offences.get(0).getId(),
                now(),
                "NOTIFIED_GUILTY");

        final PersonalDetails defendantPersonalDetails = createDefendantPersonalDetails().build();
        final CaseDetails caseDetails = createCaseDetails(defendantPersonalDetails, offences);

        final JsonObject prosecutor = createProsecutor();
        final EmployerDetails employer = createEmployer();
        final JsonObject prosecutionCaseFile = createCaseFileDetails();
        final JsonObject caseFileDefendantDetails = createCaseFileDefendantDetails();

        final List<ProsecutionCaseView> prosecutionCaseViews = prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                prosecutor,
                prosecutionCaseFile,
                caseFileDefendantDetails,
                employer,
                nonNull(caseFileDefendantDetails) ? DEFENDANT_NATIONALITY_ID : null,
                DEFENDANT_ETHNICITY_ID,
                caseReferredForCourtHearing,
                OFFENCE_MITIGATION,
                mockCJSOffenceCodeToOffenceDefinitionId(),
                Arrays.asList(offences.get(0), offences.get(1)));

        assertThat(prosecutionCaseViews.size(), is(1));

        final ProsecutionCaseView prosecutionCaseView = prosecutionCaseViews.get(0);
        assertProsecutionView(prosecutionCaseView, nonReferredOffence, prosecutionCaseFile, caseFileDefendantDetails);

        final DefendantView defendantView = prosecutionCaseView.getDefendants().get(0);

        assertDefendantDetailsMatch(defendantPersonalDetails, defendantView, employer, caseFileDefendantDetails, employer, 2);

        final OffenceView offenceView1 = defendantView.getOffences().get(0);
        assetOffenceView(offenceView1, offences.get(0), caseFileDefendantDetails, notifiedPleaView, DECISION_DATE.toLocalDate());

    }

    private void createProsecutionCaseViewsAndVerifyResultCorrect(final LocalDate expectedConvictionDate,
                                                                  final JsonObject caseFileDefendantDetails,
                                                                  final EmployerDetails employer,
                                                                  final VerdictType verdictType) {
        List<Offence> offences = createOffences(OFFENCE_ID1, OFFENCE_ID2);
        createProsecutionCaseViewsAndVerifyResultCorrect(expectedConvictionDate, caseFileDefendantDetails, employer, createDefendantPersonalDetails(), verdictType, offences);
    }

    private void createProsecutionCaseViewsAndVerifyResultCorrect(final LocalDate expectedConvictionDate,
                                                                  final JsonObject caseFileDefendantDetails,
                                                                  final EmployerDetails employer,
                                                                  final PersonalDetails.Builder personalDetailsBuilder,
                                                                  final VerdictType verdictType,
                                                                  List<Offence> offences) {
        final CaseReferredForCourtHearing caseReferredForCourtHearing = caseReferredForCourtHearing()
                .withReferredAt(DECISION_DATE)
                .withReferredOffences(getReferredOffencesWithVerdict(verdictType))
                .build();
        final Offence offence = offences.get(0);
        final NotifiedPleaView notifiedPleaView = new NotifiedPleaView(
                offence.getId(),
                now(),
                "NOTIFIED_GUILTY");
        final PersonalDetails defendantPersonalDetails = personalDetailsBuilder.build();
        final CaseDetails caseDetails = createCaseDetails(defendantPersonalDetails, offences);

        final JsonObject prosecutor = createProsecutor();

        final List<ProsecutionCaseView> prosecutionCaseViews = prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                prosecutor,
                null,
                caseFileDefendantDetails,
                employer,
                nonNull(caseFileDefendantDetails) ? DEFENDANT_NATIONALITY_ID : null,
                DEFENDANT_ETHNICITY_ID,
                caseReferredForCourtHearing,
                OFFENCE_MITIGATION,
                mockCJSOffenceCodeToOffenceDefinitionId(),
                singletonList(offence));

        assertThat(prosecutionCaseViews.size(), is(1));

        final ProsecutionCaseView prosecutionCaseView = prosecutionCaseViews.get(0);
        assertProsecutionView(prosecutionCaseView, offence, null, caseFileDefendantDetails);

        final DefendantView defendantView = prosecutionCaseView.getDefendants().get(0);

        assertDefendantDetailsMatch(defendantPersonalDetails, defendantView, employer, caseFileDefendantDetails, employer, 1);

        final OffenceView offenceView = defendantView.getOffences().get(0);
        assetOffenceView(offenceView, offence, caseFileDefendantDetails, notifiedPleaView, expectedConvictionDate);
    }

    private void assetOffenceView(final OffenceView offenceView, final Offence offence, final JsonObject caseFileDefendantDetails, final NotifiedPleaView notifiedPleaView, final LocalDate expectedConvictionDate) {
        assertThat(offenceView.getId(), is(offence.getId()));
        assertThat(offenceView.getChargeDate(), is(LocalDate.parse(offence.getChargeDate())));
        assertThat(offenceView.getOffenceDefinitionId(), is(OFFENCE_DEFINITION_ID));
        assertThat(offenceView.getStartDate(), is(LocalDate.parse(offence.getStartDate())));
        assertThat(offenceView.getEndDate(), is(nonNull(caseFileDefendantDetails) ? OFFENCE_END_DATE : null));
        assertThat(offenceView.getWording(), is(offence.getWording()));
        assertThat(offenceView.getWordingWelsh(), is(offence.getWordingWelsh()));
        assertThat(offenceView.getNotifiedPlea(), is(notifiedPleaView));
        assertThat(offenceView.getConvictionDate(), is(expectedConvictionDate));
        assertThat(offenceView.getCount(), is(1));
        assertThat(ofNullable(offenceView.getOffenceFacts()).map(OffenceFactsView::getVehicleMake).orElse(null), is(offence.getVehicleMake()));
        assertThat(ofNullable(offenceView.getOffenceFacts()).map(OffenceFactsView::getVehicleRegistration).orElse(null), is(offence.getVehicleRegistrationMark()));
    }

    private void assertDefendantDetailsMatch(final PersonalDetails defendantPersonalDetails,
                                             final DefendantView defendantView,
                                             final EmployerDetails employer,
                                             final JsonObject caseFileDefendantDetails,
                                             final EmployerDetails employerDetails,
                                             final int offenceViews) {

        assertThat(defendantView.getId(), is(DEFENDANT_ID));
        assertThat(defendantView.getProsecutionCaseId(), is(CASE_ID));
        assertThat(defendantView.getNumberOfPreviousConvictionsCited(), is(DEFENDANT_NUM_PREVIOUS_CONVICTIONS));
        assertThat(defendantView.getMitigation(), is(OFFENCE_MITIGATION));
        assertThat(defendantView.getOffences(), iterableWithSize(offenceViews));

        assertThat(defendantView.getPersonDefendant().getSelfDefinedEthnicityId(), is(DEFENDANT_ETHNICITY_ID));

        final PersonDetailsView personalDetailsView = defendantView.getPersonDefendant().getPersonDetails();
        assertThat(personalDetailsView.getFirstName(), is(defendantPersonalDetails.getFirstName()));
        assertThat(personalDetailsView.getLastName(), is(defendantPersonalDetails.getLastName()));
        assertThat(personalDetailsView.getTitle(), is(nonNull(defendantPersonalDetails.getTitle()) ? defendantPersonalDetails.getTitle().toUpperCase() : null));
        assertThat(personalDetailsView.getDateOfBirth(), is(defendantPersonalDetails.getDateOfBirth()));
        assertThat(personalDetailsView.getGender(), is(defendantPersonalDetails.getGender().name()));

        assertThat(personalDetailsView.getDocumentationLanguageNeeds(), is(nonNull(caseFileDefendantDetails) ? "WELSH" : "ENGLISH"));
        assertThat(personalDetailsView.getNationalityId(), is(nonNull(caseFileDefendantDetails) ? DEFENDANT_NATIONALITY_ID : null));
        assertThat(personalDetailsView.getNationalInsuranceNumber(), is(NATIONAL_INSURANCE_NUMBER));
        assertThat(personalDetailsView.getOccupation(), is(nonNull(caseFileDefendantDetails) ? DEFENDANT_OCCUPATION : null));
        assertThat(personalDetailsView.getOccupationCode(), is(nonNull(caseFileDefendantDetails) ? valueOf(DEFENDANT_OCCUPATION_CODE) : null));
        assertThat(personalDetailsView.getSpecificRequirements(), is(nonNull(caseFileDefendantDetails) ? DEFENDANT_SPECIFIC_REQUIREMENTS : null));

        final AddressView addressView = personalDetailsView.getAddress();
        assertThat(addressView.getAddress1(), is(defendantPersonalDetails.getAddress().getAddress1()));
        assertThat(addressView.getAddress2(), is(defendantPersonalDetails.getAddress().getAddress2()));
        assertThat(addressView.getAddress3(), is(defendantPersonalDetails.getAddress().getAddress3()));
        assertThat(addressView.getAddress4(), is(defendantPersonalDetails.getAddress().getAddress4()));
        assertThat(addressView.getAddress5(), is(defendantPersonalDetails.getAddress().getAddress5()));
        assertThat(addressView.getPostcode(), is(defendantPersonalDetails.getAddress().getPostcode()));

        assertContactDetails(defendantPersonalDetails, caseFileDefendantDetails, personalDetailsView);

        final EmployerOrganisationView employerOrganisationDetails = defendantView.getPersonDefendant().getEmployerOrganisation();
        if (nonNull(employerDetails.getName())) {
            assertThat(employerOrganisationDetails.getAddress().getAddress1(), is(employer.getAddress().getAddress1()));
            assertThat(employerOrganisationDetails.getAddress().getAddress2(), is(employer.getAddress().getAddress2()));
            assertThat(employerOrganisationDetails.getAddress().getAddress3(), is(employer.getAddress().getAddress3()));
            assertThat(employerOrganisationDetails.getAddress().getAddress4(), is(employer.getAddress().getAddress4()));
            assertThat(employerOrganisationDetails.getAddress().getAddress5(), is(employer.getAddress().getAddress5()));
            assertThat(employerOrganisationDetails.getAddress().getPostcode(), is(employer.getAddress().getPostcode()));
            assertThat(employerOrganisationDetails.getContact().getWork(), is(employer.getPhone()));
            assertThat(employerOrganisationDetails.getName(), is(employer.getName()));
        } else {
            assertThat(employerOrganisationDetails, nullValue());
        }
    }

    private void assertContactDetails(final PersonalDetails defendantPersonalDetails,
                                      final JsonObject caseFileDefendantDetails,
                                      final PersonDetailsView personalDetailsView) {
        final ContactView contactView = personalDetailsView.getContact();
        assertThatNullIfBlankOrGivenValue(contactView.getHome(), defendantPersonalDetails.getContactDetails().getHome());
        assertThatNullIfBlankOrGivenValue(contactView.getHome(), defendantPersonalDetails.getContactDetails().getHome());
        assertThatNullIfBlankOrGivenValue(contactView.getWork(), nonNull(caseFileDefendantDetails) ? DEFENDANT_WORK_PHONE : null);
        assertThatNullIfBlankOrGivenValue(contactView.getMobile(), defendantPersonalDetails.getContactDetails().getMobile());
        assertThatNullIfBlankOrGivenValue(contactView.getPrimaryEmail(), defendantPersonalDetails.getContactDetails().getEmail());
        assertThatNullIfBlankOrGivenValue(contactView.getSecondaryEmail(), nonNull(caseFileDefendantDetails) ? DEFENDANT_SECONDARY_EMAIL : null);
    }

    private void assertThatNullIfBlankOrGivenValue(final String actual, final String expected) {
        assertThat(actual, is(StringUtils.isBlank(expected) ? null : expected));
    }

    private void assertProsecutionView(final ProsecutionCaseView prosecutionCaseView, Offence offence,
                                       final JsonObject prosecutionCaseFile, final JsonObject caseFileDefendantDetails) {
        assertThat(prosecutionCaseView.getId(), is(CASE_ID));
        assertThat(prosecutionCaseView.getInitiationCode(), is("J"));
        assertThat(prosecutionCaseView.getStatementOfFacts(), is(offence.getProsecutionFacts()));
        assertThat(prosecutionCaseView.getProsecutionCaseIdentifier(), is(new ProsecutionCaseIdentifierView(
                PROSECUTOR_ID,
                PROSECUTOR_CODE,
                CASE_URN.toString(),
                null)));
        assertThat(prosecutionCaseView.getStatementOfFactsWelsh(), is(nonNull(caseFileDefendantDetails) ? STATEMENT_OF_FACTS_WELSH : null));
        assertThat(prosecutionCaseView.getDefendants(), iterableWithSize(1));
    }

    private JsonObject createProsecutor() {
        return createObjectBuilder()
                .add("prosecutors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", PROSECUTOR_ID.toString())
                                .add("shortName", PROSECUTOR_CODE)))
                .build();
    }

    private CaseDetails createCaseDetails(final PersonalDetails defendantPersonalDetails, List<Offence> offences) {
        return caseDetails()
                .withId(CASE_ID)
                .withUrn(CASE_URN.toString())
                .withDefendant(defendant()
                        .withId(DEFENDANT_ID)
                        .withInterpreter(Interpreter.interpreter()
                                .withLanguage(LANGUAGE_E)
                                .build())
                        .withNumPreviousConvictions(DEFENDANT_NUM_PREVIOUS_CONVICTIONS)
                        .withPersonalDetails(defendantPersonalDetails)
                        .withOffences(offences)
                        .build())
                .build();
    }

    private PersonalDetails.Builder createDefendantPersonalDetails() {
        return PersonalDetails.personalDetails()
                .withFirstName("Boris")
                .withLastName("Becker")
                .withTitle("Mr")
                .withDateOfBirth(LocalDate.of(1989, 11, 12))
                .withGender(Gender.MALE)
                .withAddress(Address.address()
                        .withAddress1("Address 1")
                        .withAddress2("Address 2")
                        .withAddress3("Address 3")
                        .withAddress4("Address 4")
                        .withAddress5("Address 5")
                        .withPostcode("E11 12G")
                        .build())
                .withContactDetails(ContactDetails.contactDetails()
                        .withBusiness("+1234132514")
                        .withEmail("b.b@foo.bar")
                        .withEmail2("b.b@bar.foo")
                        .withHome("+5234132514")
                        .withMobile("+6214132514")
                        .build())
                .withTitle("Mr")
                .withNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER);
    }

    private EmployerDetails createEmployer() {
        return EmployerDetails.employerDetails()
                .withName(EMPLOYER_NAME)
                .withAddress(Address.address()
                        .withAddress1("Employer Address 1")
                        .withAddress2("Employer Address 2")
                        .withAddress3("Employer Address 3")
                        .withAddress4("Employer Address 4")
                        .withAddress5("Employer Address 5")
                        .withPostcode("Employer Postcode")
                        .build())
                .withPhone("Employer Phone")
                .build();
    }

    private JsonObject createCaseFileDefendantDetails() {
        return createObjectBuilder()
                .add(
                        "selfDefinedInformation",
                        createObjectBuilder()
                                .add("nationality", DEFENDANT_NATIONALITY_CODE)
                                .add("ethnicity", DEFENDANT_ETHNICITY))
                .add(
                        "personalInformation",
                        createObjectBuilder()
                                .add("occupation", DEFENDANT_OCCUPATION)
                                .add("occupationCode", DEFENDANT_OCCUPATION_CODE)
                                .add("work", DEFENDANT_WORK_PHONE)
                                .add("secondaryEmail", DEFENDANT_SECONDARY_EMAIL))
                .add("nationalInsuranceNumber", NATIONAL_INSURANCE_NUMBER)
                .add("documentationLanguage", "W")
                .add("specificRequirements", DEFENDANT_SPECIFIC_REQUIREMENTS)
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("offenceId", OFFENCE_ID1.toString())
                                .add("statementOfFactsWelsh", STATEMENT_OF_FACTS_WELSH)
                                .add("offenceCommittedEndDate", OFFENCE_END_DATE.toString())))
                .build();
    }

    private JsonObject createCaseFileDetails(){
        return createObjectBuilder()
                .add("caseId", "709f6cd8-b371-41bc-be6b-841d42b7fbfd")
                .add("prosecutorInformant", "Adam")
                .add("prosecutionCaseReference", PROSECUTION_CASE_REFERENCE)
                .add("prosecutionAuthority", "GAEAA01")
                .add("originatingOrganisation", "GAEAA01")
                .add("defendants", createArrayBuilder().add(createCaseFileDefendantDetails()))
                .build();
    }

    private static Offence createOffence(String code, UUID id, String facts, int sequenceNumber) {
        return Offence.offence()
                .withCjsCode(code)
                .withId(id)
                .withOffenceCode("offence code")
                .withPlea(PleaType.GUILTY_REQUEST_HEARING)
                .withPleaDate(PLEA_DATE)
                .withWording("wording")
                .withWordingWelsh("welsh wording")
                .withStartDate(now().toString())
                .withChargeDate(now().minusDays(5).toString())
                .withProsecutionFacts(facts)
                .withOffenceSequenceNumber(sequenceNumber)
                .build();
    }
    private static List<Offence> createOffences(final UUID... ids) {
        return Arrays.stream(ids)
                .map(id ->
                        createOffence(OFFENCE_CJS_CODE1, id, "Prosecution facts", 11))
                .collect(Collectors.toList());
    }

    private static Map<String, UUID> mockCJSOffenceCodeToOffenceDefinitionId() {
        return ImmutableMap.of(OFFENCE_CJS_CODE1, OFFENCE_DEFINITION_ID);
    }

    private static List<OffenceDecisionInformation> getReferredOffencesWithVerdict(VerdictType verdictType) {
        return singletonList(OffenceDecisionInformation.createOffenceDecisionInformation(OFFENCE_ID1, verdictType));
    }
}
