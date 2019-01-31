package uk.gov.moj.cpp.sjp.domain.testutils;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.RandomStringUtils;

public class CaseBuilder {

    private static final String URN = "urnValue";
    private static final String ENTERPRISE_ID = "enterpriseIdValue";
    private static BigDecimal COMPENSATION = BigDecimal.valueOf(11.11);
    private static final List<Offence> OFFENCES = Collections.singletonList(
            new Offence(UUID.randomUUID(), 0, null, null,
                    0, null, null, null, null, COMPENSATION)
    );
    private static final int NUM_PREVIOUS_CONVICTIONS = 3;
    private static final Address ADDRESS = new Address("street", "suburb", "town", "county", "address5", "AA1 2BB");

    private static BigDecimal COSTS = BigDecimal.valueOf(33.33);
    private static LocalDate POSTING_DATE = LocalDate.of(2015, 12, 3);

    private static final String TITLE = "Mr";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1980, 12, 3);
    private static final Gender GENDER = Gender.MALE;
    private static final String NATIONAL_INSURANCE_NUMBER = RandomStringUtils.random(10);
    private static final String DRIVER_NUMBER = RandomStringUtils.random(10);
    private static final ContactDetails CONTACT_DETAILS = new ContactDetails("020734777", "020734888", "020734999", "email1@bbb.ccc", "email2@bbb.ccc");

    private static final String LANGUAGE_NEEDS = RandomStringUtils.random(10);

    private UUID id;
    private String urn;
    private String enterpriseId;
    private ProsecutingAuthority prosecutingAuthority;
    private Defendant defendant;

    private CaseBuilder() {
        id = DefaultTestData.CASE_ID;
        urn = URN;
        enterpriseId = ENTERPRISE_ID;
        prosecutingAuthority = ProsecutingAuthority.TFL;
        defendant = new Defendant(DefaultTestData.DEFENDANT_ID, TITLE, FIRST_NAME, LAST_NAME,
                DATE_OF_BIRTH, GENDER, NATIONAL_INSURANCE_NUMBER, DRIVER_NUMBER, ADDRESS,
                CONTACT_DETAILS, NUM_PREVIOUS_CONVICTIONS, OFFENCES, LANGUAGE_NEEDS);
    }

    public static CaseBuilder aDefaultSjpCase() {
        return new CaseBuilder();
    }

    public CaseBuilder withDefendant(Defendant defendant) {
        this.defendant = defendant;
        return this;
    }

    public CaseBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public Case build() {
        return new Case(id,
                urn,
                enterpriseId,
                prosecutingAuthority,
                COSTS,
                POSTING_DATE,
                defendant);
    }
}
