package uk.gov.moj.cpp.sjp.command.utils;

import static java.util.Collections.singletonList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.json.schemas.domains.sjp.Address.address;
import static uk.gov.justice.json.schemas.domains.sjp.command.Employer.employer;
import static uk.gov.justice.json.schemas.domains.sjp.command.PersonalDetails.personalDetails;
import static uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline.pleadOnline;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.command.Employer;
import uk.gov.justice.json.schemas.domains.sjp.command.FinancialMeans;
import uk.gov.justice.json.schemas.domains.sjp.command.Offence;
import uk.gov.justice.json.schemas.domains.sjp.command.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.command.Plea;
import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;

import java.util.UUID;

import javax.json.JsonObject;

public class CommonObjectBuilderUtil {

    public static JsonObject buildAddressWithPostcode(final String postcode) {
        return createObjectBuilder()
                .add("address1", "line1")
                .add("address2", "line2")
                .add("postcode", postcode)
                .build();
    }

    public static JsonObject buildDefendantWithAddress(final JsonObject address){
        return createObjectBuilder()
                .add("firstName", "David")
                .add("gender", "Male")
                .add("address", address)
                .build();
    }

    public static PleadOnline buildPleadOnline(final Plea plea, final UUID caseId, final FinancialMeans financialMeans,
                                               final PersonalDetails personalDetails, final Employer employer) {
        return pleadOnline()
                .withCaseId(caseId)
                .withOffences(singletonList(Offence.offence()
                        .withPlea(plea)
                        .build()))
                .withFinancialMeans(financialMeans)
                .withPersonalDetails(personalDetails)
                .withEmployer(employer)
                .build();
    }

    public static PleadOnline buildPleadOnline(final Plea plea, final UUID caseId, final FinancialMeans financialMeans) {
        return pleadOnline()
                .withCaseId(caseId)
                .withOffences(singletonList(Offence.offence()
                        .withPlea(plea)
                        .build()))
                .withFinancialMeans(financialMeans)
                .withPersonalDetails(buildPersonalDetailsWithAddress(buildAddressObjectWithPostcode("SE1 1PJ")))
                .build();
    }

    public static PersonalDetails buildPersonalDetailsWithAddress(final Address address) {
        return personalDetails()
                .withFirstName("John")
                .withLastName("Doe")
                .withDateOfBirth("01/01/1999")
                .withAddress(address)
                .build();
    }

    public static Employer buildEmployerWithAddress(final Address address) {
        return employer()
                .withEmployeeReference("12345")
                .withName("Pret a manger")
                .withPhone("020 7998 0007")
                .withAddress(address)
                .build();
    }

    public static Address buildAddressObjectWithPostcode(final String postcode) {
        return address()
                .withAddress1("14 Tottenham Court Road")
                .withAddress3("London")
                .withPostcode(postcode)
                .build();
    }
}
