package uk.gov.moj.cpp.sjp.persistence.entity.view;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;

public class UpdatedDefendantDetailsTest {

    @Test
    public void shouldMostRecentUpdateDateForSingleUpdate() {
        ZonedDateTime addressUpdatedAt = ZonedDateTime.now(UTC);
        UpdatedDefendantDetails updatedDefendantDetails = new UpdatedDefendantDetails(
                "firstName",
                "lastName",
                LocalDate.now(),
                UUID.randomUUID(),
                addressUpdatedAt,
                null,
                null,
                "caseUrn",
                UUID.randomUUID());

        final Optional<ZonedDateTime> mostRecentUpdateDataOptional = updatedDefendantDetails.getMostRecentUpdateDate();

        assertThat(mostRecentUpdateDataOptional.isPresent(), is(true));
        assertThat(mostRecentUpdateDataOptional.get(), is(addressUpdatedAt));
    }

    @Test
    public void shouldCorrectlyCompareUpdateDates() {
        ZonedDateTime addressUpdatedAt = ZonedDateTime.now(UTC).minusDays(2);
        ZonedDateTime dobUpdatedAt = ZonedDateTime.now(UTC).minusDays(1);
        ZonedDateTime nameUpdatedAt = ZonedDateTime.now(UTC);

        UpdatedDefendantDetails updatedDefendantDetails = new UpdatedDefendantDetails(
                "firstName",
                "lastName",
                LocalDate.now(),
                UUID.randomUUID(),
                addressUpdatedAt,
                dobUpdatedAt,
                nameUpdatedAt,
                "caseUrn",
                UUID.randomUUID());

        final Optional<ZonedDateTime> mostRecentUpdateDataOptional = updatedDefendantDetails.getMostRecentUpdateDate();

        assertThat(mostRecentUpdateDataOptional.isPresent(), is(true));
        assertThat(mostRecentUpdateDataOptional.get(), is(nameUpdatedAt));
    }

}
