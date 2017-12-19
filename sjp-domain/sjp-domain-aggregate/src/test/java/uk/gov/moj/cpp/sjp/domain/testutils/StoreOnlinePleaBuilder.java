package uk.gov.moj.cpp.sjp.domain.testutils;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class StoreOnlinePleaBuilder {

    private static final String MITIGATION = "It was an accident";
    private static final String NOT_GUILTY_BECAUSE = "I wasn't there";
    private static final String WITNESS_DISPUTE = "They were not there";
    private static final String WITNESS_DETAILS = "Job Bloggs";
    private static final String UNAVAILABILITY = "Not avaailble from 12th Jan to 12th Feb";

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
    private static final String OUTGOING_DESCRIPTION = "Accomodation";
    private static final BigDecimal OUTGOING_AMOUNT = BigDecimal.valueOf(400.50);

    public static PleadOnline defaultStoreOnlinePleaWithGuiltyPlea(UUID offenceId, String defendantId) {
        List<Offence> offences = Arrays.asList(
                new Offence(offenceId.toString(), PleaType.GUILTY, false, MITIGATION, null)
        );
        final FinancialMeans financialMeans = new FinancialMeans(null, new Income(IncomeFrequency.MONTHLY, INCOME_AMOUNT),
                new Benefits(BENEFITS_CLAIMED, BENEFITS_TYPE, BENEFITS_DEDUCT), EMPLOYMENT_STATUS);
        final Employer employer = new Employer(null, EMPLOYER_NAME, EMPLOYER_REFERENCE, EMPLOYER_PHONE,
                new Address(EMPLOYER_ADDRESS_1, EMPLOYER_ADDRESS_2, EMPLOYER_ADDRESS_3, EMPLOYER_ADDRESS_4, EMPLOYER_POSTCODE));

        final List<Outgoing> outgoings = Arrays.asList(
                new Outgoing(OUTGOING_DESCRIPTION, OUTGOING_AMOUNT)
        );
        return new PleadOnline(defendantId, offences, null, null,
                null, null, financialMeans, employer, outgoings);
    }

    public static PleadOnline defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(UUID offenceId, String defendantId, String interpreterLanguage) {
        List<Offence> offences = Arrays.asList(
                new Offence(offenceId.toString(), PleaType.GUILTY, true, MITIGATION, null)
        );
        final FinancialMeans financialMeans = new FinancialMeans(null, new Income(IncomeFrequency.MONTHLY, INCOME_AMOUNT),
                new Benefits(BENEFITS_CLAIMED, BENEFITS_TYPE, BENEFITS_DEDUCT), EMPLOYMENT_STATUS);
        final Employer employer = new Employer(null, EMPLOYER_NAME, EMPLOYER_REFERENCE, EMPLOYER_PHONE,
                new Address(EMPLOYER_ADDRESS_1, EMPLOYER_ADDRESS_2, EMPLOYER_ADDRESS_3, EMPLOYER_ADDRESS_4, EMPLOYER_POSTCODE));

        final List<Outgoing> outgoings = Arrays.asList(
                new Outgoing(OUTGOING_DESCRIPTION, OUTGOING_AMOUNT)
        );
        return new PleadOnline(defendantId, offences, null, interpreterLanguage,
                null, null, financialMeans, employer, outgoings);
    }

    public static PleadOnline defaultStoreOnlinePleaWithNotGuiltyPlea(UUID offenceId, String defendantId, String interpreterLanguage, boolean includeTrialRequestedFields) {
        final List<Offence> offences = Arrays.asList(
                new Offence(offenceId.toString(), PleaType.NOT_GUILTY, true, null, NOT_GUILTY_BECAUSE)
        );
        final FinancialMeans financialMeans = new FinancialMeans(null, new Income(IncomeFrequency.MONTHLY, INCOME_AMOUNT),
                new Benefits(BENEFITS_CLAIMED, BENEFITS_TYPE, BENEFITS_DEDUCT), EMPLOYMENT_STATUS);
        final Employer employer = new Employer(null, EMPLOYER_NAME, EMPLOYER_REFERENCE, EMPLOYER_PHONE,
                new Address(EMPLOYER_ADDRESS_1, EMPLOYER_ADDRESS_2, EMPLOYER_ADDRESS_3, EMPLOYER_ADDRESS_4, EMPLOYER_POSTCODE));

        final List<Outgoing> outgoings = Arrays.asList(
                new Outgoing(OUTGOING_DESCRIPTION, OUTGOING_AMOUNT)
        );
        if (includeTrialRequestedFields) {
            return new PleadOnline(defendantId, offences, UNAVAILABILITY, interpreterLanguage,
                    WITNESS_DETAILS, WITNESS_DISPUTE, financialMeans, employer, outgoings);
        }
        else {
            return new PleadOnline(defendantId, offences, null, interpreterLanguage,
                    null, null, financialMeans, employer, outgoings);
        }
    }

    public static PleadOnline defaultStoreOnlinePleaForMultipleOffences(Object[][] pleaInformationArray, String defendantId, String interpreterLanguage) {
        List<Offence> offences = Arrays.stream(pleaInformationArray)
                .map(pleaInformation -> {
                    boolean comeToCourt = (Boolean) pleaInformation[2];
                    if (pleaInformation[1].equals(PleaType.GUILTY) && !comeToCourt) {
                        return new Offence(pleaInformation[0].toString(), PleaType.GUILTY, false, MITIGATION, null);
                    }
                    else if (pleaInformation[1].equals(PleaType.GUILTY) && comeToCourt) {
                        return new Offence(pleaInformation[0].toString(), PleaType.GUILTY, true, MITIGATION, null);
                    }
                    else if (pleaInformation[1].equals(PleaType.NOT_GUILTY) ) {
                        return new Offence(pleaInformation[0].toString(), PleaType.NOT_GUILTY, true, null, NOT_GUILTY_BECAUSE);
                    }
                    return null;
                })
                .collect(Collectors.toList());

        final FinancialMeans financialMeans = new FinancialMeans(null, new Income(IncomeFrequency.MONTHLY, INCOME_AMOUNT),
                new Benefits(BENEFITS_CLAIMED, BENEFITS_TYPE, BENEFITS_DEDUCT), EMPLOYMENT_STATUS);
        final Employer employer = new Employer(null, EMPLOYER_NAME, EMPLOYER_REFERENCE, EMPLOYER_PHONE,
                new Address(EMPLOYER_ADDRESS_1, EMPLOYER_ADDRESS_2, EMPLOYER_ADDRESS_3, EMPLOYER_ADDRESS_4, EMPLOYER_POSTCODE));

        final List<Outgoing> outgoings = Arrays.asList(
                new Outgoing(OUTGOING_DESCRIPTION, OUTGOING_AMOUNT)
        );
        return new PleadOnline(defendantId, offences, UNAVAILABILITY, interpreterLanguage,
                WITNESS_DETAILS, WITNESS_DISPUTE, financialMeans, employer, outgoings);
    }
}
