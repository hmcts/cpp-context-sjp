package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.lang.String.valueOf;
import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails.caseDetails;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.Interpreter;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.AddressView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ContactView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.EmployerOrganisationView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.OffenceView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDetailsView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;

public class ProsecutionCasesViewHelperTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID CASE_URN = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID PROSECUTOR_ID = randomUUID();
    private static final UUID OFFENCE_DEFINITION_ID = randomUUID();
    private static final String PROSECUTOR_CODE = "TFL";
    private static final String OFFENCE_CJS_CODE = "offence CJS code";
    private static final String INTERPRETER_LANGUAGE = "ENGLISH";
    private static final String OFFENCE_MITIGATION = "I was not there";

    private static final LocalDate DECISION_DATE = now();
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

    private ProsecutionCasesViewHelper prosecutionCasesViewHelper = new ProsecutionCasesViewHelper();

    @Test
    public void shouldCreateProsecutionCaseViewsWithoutConvictionDateWhenVerdictNotPresent() {
        final JsonObject decision = createObjectBuilder().build();
        createProsecutionCaseViewsAndVerifyResultCorrect(decision, null, createCaseFileDefendantDetails(), createEmployer());
    }

    @Test
    public void shouldCreateProsecutionCaseViewsWithoutConvictionDateWhenNoVerdict() {
        final JsonObject decision = createObjectBuilder()
                .add("verdict", "NO_VERDICT")
                .build();
        createProsecutionCaseViewsAndVerifyResultCorrect(decision, null, createCaseFileDefendantDetails(), createEmployer());
    }

    @Test
    public void shouldCreateProsecutionCaseViewsWithConvictionDateWhenVerdictPresent() {
        final JsonObject decision = createObjectBuilder()
                .add("verdict", "Proved SJP")
                .build();
        createProsecutionCaseViewsAndVerifyResultCorrect(decision, DECISION_DATE, createCaseFileDefendantDetails(), createEmployer());
    }

    @Test
    public void shouldDefaultToEnglishHearingLanguageWhenCaseFileDetailsNull() {
        final JsonObject decision = createObjectBuilder()
                .add("verdict", "Proved SJP")
                .build();
        createProsecutionCaseViewsAndVerifyResultCorrect(decision, DECISION_DATE, null, createEmployer());
    }

    @Test
    public void shouldNotIncludeEmployerDetailsIfEmployerNameNotPresent() {
        final JsonObject decision = createObjectBuilder()
                .add("verdict", "Proved SJP")
                .build();

        createProsecutionCaseViewsAndVerifyResultCorrect(decision, DECISION_DATE, null, EmployerDetails.employerDetails().build());
    }

    private void createProsecutionCaseViewsAndVerifyResultCorrect(final JsonObject decision, final LocalDate expectedConvictionDate, final JsonObject caseFileDefendantDetails, final EmployerDetails employer) {
        final Offence offence = createOffence();
        final NotifiedPleaView notifiedPleaView = new NotifiedPleaView(
                offence.getId(),
                now(),
                "NOTIFIED_GUILTY");
        final PersonalDetails defendantPersonalDetails = createDefendantPersonalDetails();
        final CaseDetails caseDetails = createCaseDetails(defendantPersonalDetails, offence);

        final JsonObject referenceDataOffences = createReferenceDataOffences();
        final JsonObject prosecutor = createProsecutor();

        final List<ProsecutionCaseView> prosecutionCaseViews = prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                referenceDataOffences,
                prosecutor,
                decision,
                caseFileDefendantDetails,
                employer,
                nonNull(caseFileDefendantDetails) ? DEFENDANT_NATIONALITY_ID : null,
                DEFENDANT_ETHNICITY_ID,
                DECISION_DATE,
                notifiedPleaView,
                OFFENCE_MITIGATION);

        assertThat(prosecutionCaseViews.size(), is(1));

        final ProsecutionCaseView prosecutionCaseView = prosecutionCaseViews.get(0);
        assertThat(prosecutionCaseView.getId(), is(CASE_ID));
        assertThat(prosecutionCaseView.getInitiationCode(), is("J"));
        assertThat(prosecutionCaseView.getStatementOfFacts(), is(offence.getProsecutionFacts()));
        assertThat(prosecutionCaseView.getProsecutionCaseIdentifier(), is(new ProsecutionCaseIdentifierView(
                PROSECUTOR_ID,
                PROSECUTOR_CODE,
                CASE_URN.toString())));
        assertThat(prosecutionCaseView.getStatementOfFactsWelsh(), is(nonNull(caseFileDefendantDetails) ? STATEMENT_OF_FACTS_WELSH : null));
        assertThat(prosecutionCaseView.getDefendants(), iterableWithSize(1));

        final DefendantView defendantView = prosecutionCaseView.getDefendants().get(0);

        assertDefendantDetailsMatch(defendantPersonalDetails, defendantView, employer, caseFileDefendantDetails, employer);

        final OffenceView offenceView = defendantView.getOffences().get(0);
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
    }

    private void assertDefendantDetailsMatch(final PersonalDetails defendantPersonalDetails,
                                             final DefendantView defendantView,
                                             final EmployerDetails employer,
                                             final JsonObject caseFileDefendantDetails,
                                             final EmployerDetails employerDetails) {

        assertThat(defendantView.getId(), is(DEFENDANT_ID));
        assertThat(defendantView.getProsecutionCaseId(), is(CASE_ID));
        assertThat(defendantView.getNumberOfPreviousConvictionsCited(), is(DEFENDANT_NUM_PREVIOUS_CONVICTIONS));
        assertThat(defendantView.getMitigation(), is(OFFENCE_MITIGATION));
        assertThat(defendantView.getOffences(), iterableWithSize(1));

        final PersonDetailsView personalDetailsView = defendantView.getPersonDefendant().getPersonDetails();
        assertThat(personalDetailsView.getFirstName(), is(defendantPersonalDetails.getFirstName()));
        assertThat(personalDetailsView.getLastName(), is(defendantPersonalDetails.getLastName()));
        assertThat(personalDetailsView.getTitle(), is(defendantPersonalDetails.getTitle().toUpperCase()));
        assertThat(personalDetailsView.getDateOfBirth(), is(defendantPersonalDetails.getDateOfBirth()));
        assertThat(personalDetailsView.getGender(), is(defendantPersonalDetails.getGender().name()));

        assertThat(personalDetailsView.getDocumentationLanguageNeeds(), is(nonNull(caseFileDefendantDetails) ? "WELSH" : "ENGLISH"));
        assertThat(personalDetailsView.getNationalityId(), is(nonNull(caseFileDefendantDetails) ? DEFENDANT_NATIONALITY_ID : null));
        assertThat(personalDetailsView.getNationalInsuranceNumber(), is(NATIONAL_INSURANCE_NUMBER));
        assertThat(personalDetailsView.getEthnicityId(), is(DEFENDANT_ETHNICITY_ID));
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

        final ContactView contactView = personalDetailsView.getContact();
        assertThat(contactView.getHome(), is(defendantPersonalDetails.getContactDetails().getHome()));
        assertThat(contactView.getWork(), is(nonNull(caseFileDefendantDetails) ? DEFENDANT_WORK_PHONE : null));
        assertThat(contactView.getPrimaryEmail(), is(defendantPersonalDetails.getContactDetails().getEmail()));
        assertThat(contactView.getSecondaryEmail(), is(nonNull(caseFileDefendantDetails) ? DEFENDANT_SECONDARY_EMAIL : null));

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

    private JsonObject createProsecutor() {
        return createObjectBuilder()
                .add("prosecutors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", PROSECUTOR_ID.toString())
                                .add("shortName", PROSECUTOR_CODE)))
                .build();
    }

    private JsonObject createReferenceDataOffences() {
        return createObjectBuilder()
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("cjsOffenceCode", OFFENCE_CJS_CODE)
                                .add("offenceId", OFFENCE_DEFINITION_ID.toString())))
                .build();
    }

    private CaseDetails createCaseDetails(final PersonalDetails defendantPersonalDetails, final Offence offence) {
        return caseDetails()
                .withId(CASE_ID)
                .withUrn(CASE_URN.toString())
                .withDefendant(defendant()
                        .withId(DEFENDANT_ID)
                        .withInterpreter(Interpreter.interpreter()
                                .withLanguage(INTERPRETER_LANGUAGE)
                                .build())
                        .withNumPreviousConvictions(DEFENDANT_NUM_PREVIOUS_CONVICTIONS)
                        .withPersonalDetails(defendantPersonalDetails)
                        .withOffences(singletonList(offence))
                        .build())
                .build();
    }

    private PersonalDetails createDefendantPersonalDetails() {
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
                .withNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER)
                .build();
    }

    private Offence createOffence() {
        return Offence.offence()
                .withCjsCode(OFFENCE_CJS_CODE)
                .withId(randomUUID())
                .withOffenceCode("offence code")
                .withWording("wording")
                .withWordingWelsh("welsh wording")
                .withStartDate(now().toString())
                .withChargeDate(now().minusDays(5).toString())
                .withOffenceSequenceNumber(11)
                .withProsecutionFacts("Prosecution facts")
                .build();
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
                                .add("statementOfFactsWelsh", STATEMENT_OF_FACTS_WELSH)
                                .add("offenceCommittedEndDate", OFFENCE_END_DATE.toString())))
                .build();
    }

}
