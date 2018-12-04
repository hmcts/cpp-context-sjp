package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.Interpreter;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.AddressView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ContactView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.DefendantView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.OffenceView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDetailsView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;

public class ProsecutionCasesViewHelperTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID CASE_URN = randomUUID();

    private static final String OFFENCE_DEFINITION_ID = randomUUID().toString();
    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final String PROSECUTOR_ID = randomUUID().toString();
    private static final String PROSECUTOR_CODE = "TFL";
    private static final String OFFENCE_CJS_CODE = "offence CJS code";
    private static final String INTERPRETER_LANGUAGE = "ENGLISH";
    private static final String OFFENCE_MITIGATION = "I was not there";

    private static final LocalDate DECISION_DATE = now();
    private static final int DEFENDANT_NUM_PREVIOUS_CONVICTIONS = 1;

    private ProsecutionCasesViewHelper prosecutionCasesViewHelper = new ProsecutionCasesViewHelper();

    @Test
    public void shouldCreateProsecutionCaseViewsNoVerdict() {
        createProsecutionCaseViewsAndVerifyResultCorrect(null, null);
    }

    @Test
    public void shouldCreateProsecutionCaseViewsWithVerdict() {
        createProsecutionCaseViewsAndVerifyResultCorrect(
                createObjectBuilder()
                        .add("verdict", "foo")
                        .build(),
                DECISION_DATE);
    }

    private void createProsecutionCaseViewsAndVerifyResultCorrect(JsonObject decision, LocalDate expectedConvictionDate) {
        final Offence offence = createOffence();
        final NotifiedPleaView notifiedPleaView = new NotifiedPleaView(
                fromString(offence.getId()),
                now(),
                "NOTIFIED_GUILTY");
        final PersonalDetails defendantPersonalDetails = createDefendantPersonalDetails();
        CaseDetails caseDetails = createCaseDetails(defendantPersonalDetails, offence);

        final JsonObject referenceDataOffences = createReferenceDataOffences();
        final JsonObject prosecutor = createProsecutor();

        final List<ProsecutionCaseView> prosecutionCaseViews = prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                referenceDataOffences,
                prosecutor,
                decision,
                DECISION_DATE,
                notifiedPleaView,
                OFFENCE_MITIGATION);

        assertThat(prosecutionCaseViews.size(), is(1));

        final ProsecutionCaseView prosecutionCaseView = prosecutionCaseViews.get(0);
        assertThat(prosecutionCaseView.getId(), is(CASE_ID));
        assertThat(prosecutionCaseView.getInitiationCode(), is("J"));
        assertThat(prosecutionCaseView.getStatementOfFacts(), is(offence.getProsecutionFacts()));
        assertThat(prosecutionCaseView.getProsecutionCaseIdentifier(), is(new ProsecutionCaseIdentifierView(
                fromString(PROSECUTOR_ID),
                PROSECUTOR_CODE,
                CASE_URN.toString())));
        assertThat(prosecutionCaseView.getDefendants(), iterableWithSize(1));

        final DefendantView defendantView = prosecutionCaseView.getDefendants().get(0);

        assertDefendantDetailsMatch(defendantPersonalDetails, defendantView);

        final OffenceView offenceView = defendantView.getOffences().get(0);
        assertThat(offenceView.getId(), is(fromString(offence.getId())));
        assertThat(offenceView.getChargeDate(), is(LocalDate.parse(offence.getChargeDate())));
        assertThat(offenceView.getOffenceDefinitionId(), is(fromString(OFFENCE_DEFINITION_ID)));
        assertThat(offenceView.getStartDate(), is(LocalDate.parse(offence.getStartDate())));
        assertThat(offenceView.getWording(), is(offence.getWording()));
        assertThat(offenceView.getWordingWelsh(), is(offence.getWordingWelsh()));
        assertThat(offenceView.getNotifiedPlea(), is(notifiedPleaView));
        assertThat(offenceView.getConvictionDate(), is(expectedConvictionDate));
        assertThat(offenceView.getCount(), is(1));
    }

    private void assertDefendantDetailsMatch(final PersonalDetails defendantPersonalDetails, final DefendantView defendantView) {
        assertThat(defendantView.getId(), is(fromString(DEFENDANT_ID)));
        assertThat(defendantView.getProsecutionCaseId(), is(CASE_ID));
        assertThat(defendantView.getNumberOfPreviousConvictionsCited(), is(DEFENDANT_NUM_PREVIOUS_CONVICTIONS));
        assertThat(defendantView.getMitigation(), is(OFFENCE_MITIGATION));
        assertThat(defendantView.getOffences(), iterableWithSize(1));

        final PersonDetailsView personalDetailsView = defendantView.getPersonDefendant().getPersonDetails();
        assertThat(personalDetailsView.getFirstName(), is(defendantPersonalDetails.getFirstName()));
        assertThat(personalDetailsView.getLastName(), is(defendantPersonalDetails.getLastName()));
        assertThat(personalDetailsView.getTitle(), is(defendantPersonalDetails.getTitle().toUpperCase()));
        assertThat(personalDetailsView.getDateOfBirth(), is(LocalDate.parse(defendantPersonalDetails.getDateOfBirth())));
        assertThat(personalDetailsView.getGender(), is(defendantPersonalDetails.getGender().name()));


        final AddressView addressView = personalDetailsView.getAddress();
        assertThat(addressView.getAddress1(), is(defendantPersonalDetails.getAddress().getAddress1()));
        assertThat(addressView.getAddress2(), is(defendantPersonalDetails.getAddress().getAddress2()));
        assertThat(addressView.getAddress3(), is(defendantPersonalDetails.getAddress().getAddress3()));
        assertThat(addressView.getAddress4(), is(defendantPersonalDetails.getAddress().getAddress4()));
        assertThat(addressView.getAddress5(), is(defendantPersonalDetails.getAddress().getAddress5()));
        assertThat(addressView.getPostcode(), is(defendantPersonalDetails.getAddress().getPostcode()));

        final ContactView contactView = personalDetailsView.getContact();
        assertThat(contactView.getHome(), is(defendantPersonalDetails.getContactDetails().getHome()));
        assertThat(contactView.getWork(), is(defendantPersonalDetails.getContactDetails().getBusiness()));
        assertThat(contactView.getPrimaryEmail(), is(defendantPersonalDetails.getContactDetails().getEmail()));
        assertThat(contactView.getSecondaryEmail(), is(defendantPersonalDetails.getContactDetails().getEmail2()));
    }

    private JsonObject createProsecutor() {
        return createObjectBuilder()
                .add("prosecutors", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", PROSECUTOR_ID)
                                .add("shortName", PROSECUTOR_CODE)))
                .build();
    }

    private JsonObject createReferenceDataOffences() {
        return createObjectBuilder()
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("cjsOffenceCode", OFFENCE_CJS_CODE)
                                .add("offenceId", OFFENCE_DEFINITION_ID)))
                .build();
    }

    private CaseDetails createCaseDetails(final PersonalDetails defendantPersonalDetails, final Offence offence) {
        return CaseDetails.caseDetails()
                .withId(CASE_ID.toString())
                .withUrn(CASE_URN.toString())
                .withDefendant(Defendant.defendant()
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
                .withDateOfBirth("1989-11-12")
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
                .build();
    }

    private Offence createOffence() {
        return Offence.offence()
                .withCjsCode(OFFENCE_CJS_CODE)
                .withId(randomUUID().toString())
                .withOffenceCode("offence code")
                .withWording("wording")
                .withWordingWelsh("welsh wording")
                .withStartDate(now().format(DateTimeFormatter.ISO_DATE))
                .withChargeDate(now().minusDays(5).format(DateTimeFormatter.ISO_DATE))
                .withOffenceSequenceNumber(11)
                .withProsecutionFacts("Prosecution facts")
                .build();
    }
}
