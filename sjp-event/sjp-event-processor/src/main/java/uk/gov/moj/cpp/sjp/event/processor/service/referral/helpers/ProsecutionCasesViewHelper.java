package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static com.google.common.collect.Iterables.getFirst;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
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
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDefendantView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.PersonDetailsView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseIdentifierView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class ProsecutionCasesViewHelper {

    private static final String NO_VERDICT = "NO_VERDICT";

    public List<ProsecutionCaseView> createProsecutionCaseViews(
            final CaseDetails caseDetails,
            final JsonObject referenceDataOffences,
            final JsonObject prosecutors,
            final JsonObject caseDecision,
            final LocalDate referredAt,
            final NotifiedPleaView notifiedPleaView,
            final String pleaMitigation) {

        final Offence offenceDetails = ofNullable(getFirst(caseDetails.getDefendant().getOffences(), null))
                .orElseThrow(() -> new IllegalStateException(String.format("Offence not found for case %s", caseDetails.getId())));

        final String verdict = caseDecision.getString("verdict", NO_VERDICT);
        final LocalDate convictionDate = NO_VERDICT.equals(verdict) ? null : referredAt;

        final DefendantView defendantView = createDefendantView(
                caseDetails,
                referenceDataOffences,
                offenceDetails,
                convictionDate,
                notifiedPleaView,
                pleaMitigation);

        final JsonObject prosecutor = prosecutors.getJsonArray("prosecutors").getJsonObject(0);
        final ProsecutionCaseIdentifierView prosecutionCaseIdentifier = new ProsecutionCaseIdentifierView(
                UUID.fromString(prosecutor.getString("id")),
                prosecutor.getString("shortName"),
                caseDetails.getUrn());

        final ProsecutionCaseView prosecutionCaseView = new ProsecutionCaseView(
                caseDetails.getId(),
                "J",
                offenceDetails.getProsecutionFacts(),
                prosecutionCaseIdentifier,
                singletonList(defendantView));

        return singletonList(prosecutionCaseView);
    }

    private DefendantView createDefendantView(
            final CaseDetails caseDetails,
            final JsonObject referenceDataOffences,
            final Offence offenceDetails,
            final LocalDate convictionDate,
            final NotifiedPleaView notifiedPleaView,
            final String pleaMitigation) {

        final Defendant defendantDetails = caseDetails.getDefendant();
        final PersonDefendantView personDefendantView = createPersonDefendantView(defendantDetails);

        final OffenceView offence = createOffenceView(
                referenceDataOffences,
                convictionDate,
                offenceDetails,
                notifiedPleaView);

        return new DefendantView(
                defendantDetails.getId(),
                caseDetails.getId(),
                defendantDetails.getNumPreviousConvictions(),
                pleaMitigation,
                singletonList(offence),
                personDefendantView);
    }

    private static PersonDefendantView createPersonDefendantView(final Defendant defendant) {
        final PersonalDetails defendantPersonalDetails = defendant.getPersonalDetails();
        final ContactView contact = createContactView(defendantPersonalDetails);
        final AddressView address = createAddressView(defendantPersonalDetails);

        return new PersonDefendantView(new PersonDetailsView(
                defendantPersonalDetails.getTitle().toUpperCase(),
                defendantPersonalDetails.getFirstName(),
                defendantPersonalDetails.getLastName(),
                defendantPersonalDetails.getDateOfBirth(),
                defendantPersonalDetails.getGender().name(),
                ofNullable(defendant.getInterpreter())
                        .map(Interpreter::getLanguage)
                        .orElse(null),
                address,
                contact));
    }

    private static OffenceView createOffenceView(
            final JsonObject referenceDataOffences,
            final LocalDate convictionDate,
            final Offence offenceDetails,
            final NotifiedPleaView notifiedPleaView) {

        final Optional<String> offenceDefinitionIdOptional = referenceDataOffences.getJsonArray("offences").getValuesAs(JsonObject.class).stream()
                .filter(referenceDataOffence -> referenceDataOffence.getString("cjsOffenceCode").equals(offenceDetails.getCjsCode()))
                .map(referenceDataOffence -> referenceDataOffence.getString("offenceId"))
                .findFirst();

        return new OffenceView(
                offenceDetails.getId(),
                offenceDefinitionIdOptional.map(UUID::fromString).orElse(null),
                offenceDetails.getWording(),
                offenceDetails.getWordingWelsh(),
                LocalDate.parse(offenceDetails.getStartDate()),
                LocalDate.parse(offenceDetails.getChargeDate()),
                convictionDate,
                offenceDetails.getOffenceSequenceNumber(),
                notifiedPleaView);
    }

    private static AddressView createAddressView(final PersonalDetails defendantPersonalDetails) {
        final Address defendantAddress = defendantPersonalDetails.getAddress();

        return new AddressView(
                defendantAddress.getAddress1(),
                defendantAddress.getAddress2(),
                defendantAddress.getAddress3(),
                defendantAddress.getAddress4(),
                defendantAddress.getAddress5(),
                defendantAddress.getPostcode());
    }

    private static ContactView createContactView(final PersonalDetails defendantPersonalDetails) {
        final ContactDetails defendantContactDetails = defendantPersonalDetails.getContactDetails();

        return new ContactView(
                defendantPersonalDetails.getContactDetails().getHome(),
                defendantContactDetails.getBusiness(),
                defendantContactDetails.getMobile(),
                defendantContactDetails.getEmail(),
                defendantContactDetails.getEmail2());
    }

}
