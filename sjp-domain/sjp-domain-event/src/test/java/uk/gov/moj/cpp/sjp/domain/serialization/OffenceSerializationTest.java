package uk.gov.moj.cpp.sjp.domain.serialization;

import static org.hamcrest.CoreMatchers.equalTo;

import uk.gov.moj.cpp.sjp.domain.Offence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;

public class OffenceSerializationTest extends AbstractSerializationTest<Offence> {

    private static final Offence FULL_OFFENCE = new Offence(
            UUID.fromString("2159f6e6-aa4e-49e4-b983-6a3673de67f1"), 123,
            "libra_offence_code", LocalDate.of(2010, 1, 1),
            456,
            null,
            LocalDate.of(2009, 2, 2),
            "offence_wording", "prosecution_facts",
            "witness_statement", BigDecimal.TEN,
            "prosecution_charge_wording_welsh", 789,
            LocalDate.of(2010, 3, 3),
            LocalDate.of(2011, 4, 4));


    private static final Offence FULL_OFFENCE_WITH_LEGACY_OFFENCE_DATE = new Offence(
            UUID.fromString("7884634a-8b25-4650-be3b-7ca39330935f"), 123,
            "libra_offence_code", LocalDate.of(2010, 1, 1),
            456,
            LocalDate.of(2009, 2, 2),
            null,
            "offence_wording", "prosecution_facts",
            "witness_statement", BigDecimal.TEN,
            "prosecution_charge_wording_welsh",
            789,
            LocalDate.of(2010, 3, 3),
            LocalDate.of(2011, 4, 4));

    private static final String EXPECTED_FULL_OFFENCE_SERIALIZATION = "{\"id\":\"2159f6e6-aa4e-49e4-b983-6a3673de67f1\",\"offenceSequenceNo\":123,\"libraOffenceCode\":\"libra_offence_code\",\"chargeDate\":\"2010-01-01\",\"libraOffenceDateCode\":456,\"offenceCommittedDate\":\"2009-02-02\",\"offenceWording\":\"offence_wording\",\"prosecutionFacts\":\"prosecution_facts\",\"witnessStatement\":\"witness_statement\",\"compensation\":10,\"prosecutionChargeWordingWelsh\":\"prosecution_charge_wording_welsh\",\"backDuty\":789,\"backDutyDateFrom\":\"2010-03-03\",\"backDutyDateTo\":\"2011-04-04\"}";
    private static final String EXPECTED_FULL_OFFENCE_WITH_LEGACY_OFFENCE_DATE_SERIALIZATION = "{\"id\":\"7884634a-8b25-4650-be3b-7ca39330935f\",\"offenceSequenceNo\":123,\"libraOffenceCode\":\"libra_offence_code\",\"chargeDate\":\"2010-01-01\",\"libraOffenceDateCode\":456,\"offenceCommittedDate\":\"2009-02-02\",\"offenceWording\":\"offence_wording\",\"prosecutionFacts\":\"prosecution_facts\",\"witnessStatement\":\"witness_statement\",\"compensation\":10,\"prosecutionChargeWordingWelsh\":\"prosecution_charge_wording_welsh\",\"backDuty\":789,\"backDutyDateFrom\":\"2010-03-03\",\"backDutyDateTo\":\"2011-04-04\"}";

    private static final Offence OFFENCE_WITHOUT_OPTIONAL_FIELDS = new Offence(
            UUID.fromString("2159f6e6-aa4e-49e4-b983-6a3673de67f1"), 123, "libra_offence_code", LocalDate.of(2010, 1, 1), 456,
            LocalDate.of(2009, 2, 2), "offence_wording", "prosecution_facts",
            "witness_statement", BigDecimal.TEN);

    private static final String EXPECTED_OFFENCE_WITHOUT_OPTIONAL_FIELDS_SERIALIZATION = "{\"id\":\"2159f6e6-aa4e-49e4-b983-6a3673de67f1\",\"offenceSequenceNo\":123,\"libraOffenceCode\":\"libra_offence_code\",\"chargeDate\":\"2010-01-01\",\"libraOffenceDateCode\":456,\"offenceCommittedDate\":\"2009-02-02\",\"offenceWording\":\"offence_wording\",\"prosecutionFacts\":\"prosecution_facts\",\"witnessStatement\":\"witness_statement\",\"compensation\":10}";

    @Override
    Map<Offence, Matcher<String>> getParams() {
        return ImmutableMap.of(
                FULL_OFFENCE, equalTo(EXPECTED_FULL_OFFENCE_SERIALIZATION),
                FULL_OFFENCE_WITH_LEGACY_OFFENCE_DATE, equalTo(EXPECTED_FULL_OFFENCE_WITH_LEGACY_OFFENCE_DATE_SERIALIZATION),
                OFFENCE_WITHOUT_OPTIONAL_FIELDS, equalTo(EXPECTED_OFFENCE_WITHOUT_OPTIONAL_FIELDS_SERIALIZATION)
        );
    }

}