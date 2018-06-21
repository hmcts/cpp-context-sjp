package uk.gov.moj.cpp.sjp.domain.testutils;

import static java.util.Collections.singletonList;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StoreOnlinePleaBuilder {

    private static final String MITIGATION = "It was an accident";
    private static final String NOT_GUILTY_BECAUSE = "I wasn't there";
    private static final String WITNESS_DISPUTE = "They were not there";
    private static final String WITNESS_DETAILS = "Job Bloggs";
    private static final String UNAVAILABILITY = "Not available from 12th Jan to 12th Feb";

    private static final BigDecimal INCOME_AMOUNT = BigDecimal.valueOf(100.50);
    private static final Boolean BENEFITS_CLAIMED = true;
    private static final String BENEFITS_TYPE = "Jobseekers allowance";
    private static final Boolean BENEFITS_DEDUCT = true;
    private static final String EMPLOYMENT_STATUS = "EMPLOYED";
    private static final String EMPLOYER_NAME = "Rightmove";
    private static final String EMPLOYER_REFERENCE = "12345";
    private static final String EMPLOYER_PHONE = "07471390873";
    private static final String EMPLOYER_ADDRESS_1 = "1 test lane";
    private static final String EMPLOYER_ADDRESS_2 = "Abbeymead";
    private static final String EMPLOYER_ADDRESS_3 = "Cheltenham";
    private static final String EMPLOYER_ADDRESS_4 = "Gloucestershire";
    private static final String EMPLOYER_POSTCODE = "GL538FB";

    public static final String PERSON_FIRST_NAME = "Derek";
    public static final String PERSON_LAST_NAME = "Smith";
    public static final String PERSON_ADDRESS_1 = "1 test road";
    public static final String PERSON_ADDRESS_2 = "Tulse Hill";
    public static final String PERSON_ADDRESS_3 = "Brixton";
    public static final String PERSON_ADDRESS_4 = "London";
    public static final String PERSON_POSTCODE = "SE249HG";
    public static final String PERSON_HOME_PHONE = "020734887";
    public static final String PERSON_MOBILE = "020734887";
    public static final String PERSON_EMAIL = "020734887";
    public static final LocalDate PERSON_DOB = LocalDate.of(1981, 10, 1);
    public static final String PERSON_NI_NUMBER = "020734887";

    private static final String OUTGOING_DESCRIPTION = "Accommodation";
    private static final BigDecimal OUTGOING_AMOUNT = BigDecimal.valueOf(400.50);

    public static PleadOnline defaultStoreOnlinePleaWithGuiltyPlea(final UUID offenceId, final UUID defendantId) {
        final List<Offence> offences = singletonList(
                new Offence(offenceId, GUILTY, false, MITIGATION, null)
        );
        return generatePleadOnline(false, defendantId, offences, null);
    }

    public static PleadOnline defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(final UUID offenceId, final UUID defendantId, final String interpreterLanguage) {
        final List<Offence> offences = singletonList(
                new Offence(offenceId, GUILTY, true, MITIGATION, null)
        );
        return generatePleadOnline(false, defendantId, offences, interpreterLanguage);
    }

    public static PleadOnline defaultStoreOnlinePleaWithNotGuiltyPlea(UUID offenceId, UUID defendantId, String interpreterLanguage, boolean includeTrialRequestedFields) {
        final List<Offence> offences = singletonList(
                new Offence(offenceId, NOT_GUILTY, true, null, NOT_GUILTY_BECAUSE)
        );
        return generatePleadOnline(includeTrialRequestedFields, defendantId, offences, interpreterLanguage);
    }

    public static PleadOnline defaultStoreOnlinePleaWithGuiltyPlea(final UUID offenceId, final UUID defendantId, final boolean newName, final boolean newAddress, final boolean newDob) {
        final List<Offence> offences = singletonList(
                new Offence(offenceId, GUILTY, false, MITIGATION, null)
        );
        return generatePleadOnline(false, defendantId, offences, null, newName, newAddress, newDob);
    }

    private static PleadOnline generatePleadOnline(final boolean includeTrialRequestedFields, final UUID defendantId, final List<Offence> offences, final String interpreterLanguage) {
        return generatePleadOnline(includeTrialRequestedFields, defendantId, offences, interpreterLanguage, false, false, false);
    }

    private static PleadOnline generatePleadOnline(final boolean includeTrialRequestedFields, final UUID defendantId, final List<Offence> offences, final String interpreterLanguage,
                                                   final boolean newName, final boolean newAddress, final boolean newDob) {

        final String firstName = newName ? "Norman" : PERSON_FIRST_NAME;
        final String address1 = newAddress ? "1 New Amsterdam Rd" : PERSON_ADDRESS_1;
        final LocalDate dob = newDob ? LocalDate.now().minusYears(18) : PERSON_DOB;

        final PersonalDetails person = new PersonalDetails(firstName, PERSON_LAST_NAME,
                new Address(address1, PERSON_ADDRESS_2, PERSON_ADDRESS_3, PERSON_ADDRESS_4, PERSON_POSTCODE),
                new ContactDetails(PERSON_EMAIL, PERSON_HOME_PHONE, PERSON_MOBILE), dob, PERSON_NI_NUMBER);


        final FinancialMeans financialMeans = new FinancialMeans(null, new Income(IncomeFrequency.MONTHLY, INCOME_AMOUNT),
                new Benefits(BENEFITS_CLAIMED, BENEFITS_TYPE, BENEFITS_DEDUCT), EMPLOYMENT_STATUS);
        final Employer employer = new Employer(null, EMPLOYER_NAME, EMPLOYER_REFERENCE, EMPLOYER_PHONE,
                new Address(EMPLOYER_ADDRESS_1, EMPLOYER_ADDRESS_2, EMPLOYER_ADDRESS_3, EMPLOYER_ADDRESS_4, EMPLOYER_POSTCODE));

        final List<Outgoing> outgoings = singletonList(
                new Outgoing(OUTGOING_DESCRIPTION, OUTGOING_AMOUNT)
        );
        if (includeTrialRequestedFields) {
            return new PleadOnline(defendantId, offences, UNAVAILABILITY, interpreterLanguage,
                    WITNESS_DETAILS, WITNESS_DISPUTE, person, financialMeans, employer, outgoings);
        }
        else {
            return new PleadOnline(defendantId, offences, null, interpreterLanguage,
                    null, null, person, financialMeans, employer, outgoings);
        }
    }

    public static PleadOnline defaultStoreOnlinePleaForMultipleOffences(Object[][] pleaInformationArray, UUID defendantId, String interpreterLanguage) {
        List<Offence> offences = Arrays.stream(pleaInformationArray)
                .map(pleaInformation -> {
                    boolean comeToCourt = (Boolean) pleaInformation[2];
                    if (pleaInformation[1].equals(GUILTY) && !comeToCourt) {
                        return new Offence(UUID.fromString(pleaInformation[0].toString()), GUILTY, false, MITIGATION, null);
                    }
                    else if (pleaInformation[1].equals(GUILTY) && comeToCourt) {
                        return new Offence(UUID.fromString(pleaInformation[0].toString()), GUILTY, true, MITIGATION, null);
                    }
                    else if (pleaInformation[1].equals(NOT_GUILTY) ) {
                        return new Offence(UUID.fromString(pleaInformation[0].toString()), NOT_GUILTY, true, null, NOT_GUILTY_BECAUSE);
                    }
                    return null;
                })
                .collect(Collectors.toList());

        final PersonalDetails person = new PersonalDetails(PERSON_FIRST_NAME, PERSON_LAST_NAME,
                new Address(PERSON_ADDRESS_1, PERSON_ADDRESS_2, PERSON_ADDRESS_3, PERSON_ADDRESS_4, PERSON_POSTCODE),
                new ContactDetails(PERSON_EMAIL, PERSON_HOME_PHONE, PERSON_MOBILE), PERSON_DOB, PERSON_NI_NUMBER);
        final FinancialMeans financialMeans = new FinancialMeans(null, new Income(IncomeFrequency.MONTHLY, INCOME_AMOUNT),
                new Benefits(BENEFITS_CLAIMED, BENEFITS_TYPE, BENEFITS_DEDUCT), EMPLOYMENT_STATUS);
        final Employer employer = new Employer(null, EMPLOYER_NAME, EMPLOYER_REFERENCE, EMPLOYER_PHONE,
                new Address(EMPLOYER_ADDRESS_1, EMPLOYER_ADDRESS_2, EMPLOYER_ADDRESS_3, EMPLOYER_ADDRESS_4, EMPLOYER_POSTCODE));

        final List<Outgoing> outgoings = singletonList(
                new Outgoing(OUTGOING_DESCRIPTION, OUTGOING_AMOUNT)
        );
        return new PleadOnline(defendantId, offences, UNAVAILABILITY, interpreterLanguage,
                WITNESS_DETAILS, WITNESS_DISPUTE, person, financialMeans, employer, outgoings);
    }
}
