package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CaseAggregate#updateDefendantNationalInsuranceNumber}
 */
public class UpdateDefendantNationalInsuranceNumberTest extends CaseAggregateBaseTest {

    private static final String NATIONAL_INSURANCE_NUMBER = randomAlphanumeric(10);

    private UUID userId;

    @BeforeEach
    public void init() {
        userId = randomUUID();
    }

    @Test
    public void shouldUpdateDefendantNationalInsuranceNumber() {
        when(caseAggregate.updateDefendantNationalInsuranceNumber(userId, defendantId, NATIONAL_INSURANCE_NUMBER))
                .thenExpect(new DefendantsNationalInsuranceNumberUpdated(caseId, defendantId, NATIONAL_INSURANCE_NUMBER));
    }

    @Test
    public void shouldUpdateDefendantNationalInsuranceNumberWhenNoUserId() {
        when(caseAggregate.updateDefendantNationalInsuranceNumber(null, defendantId, NATIONAL_INSURANCE_NUMBER))
                .thenExpect(new DefendantsNationalInsuranceNumberUpdated(caseId, defendantId, NATIONAL_INSURANCE_NUMBER));
    }

    @Test
    public void shouldUpdateDefendantNationalInsuranceNumberWhenNoDefendantId() {
        final UUID nullDefendantId = null;
        when(caseAggregate.updateDefendantNationalInsuranceNumber(userId, nullDefendantId, NATIONAL_INSURANCE_NUMBER))
                .thenExpect(new DefendantsNationalInsuranceNumberUpdated(caseId, nullDefendantId, NATIONAL_INSURANCE_NUMBER));
    }

    @Test
    public void shouldUpdateDefendantNationalInsuranceNumberWhenEmptyNIN() {
        final String nullNationalInsuranceNumber = null;
        when(caseAggregate.updateDefendantNationalInsuranceNumber(userId, defendantId, nullNationalInsuranceNumber))
                .thenExpect(new DefendantsNationalInsuranceNumberUpdated(caseId, defendantId, nullNationalInsuranceNumber));
    }

}
