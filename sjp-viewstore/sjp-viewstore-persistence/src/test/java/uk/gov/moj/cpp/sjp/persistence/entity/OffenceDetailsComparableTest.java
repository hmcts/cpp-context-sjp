package uk.gov.moj.cpp.sjp.persistence.entity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail.builder;

import org.junit.jupiter.api.Test;

public class OffenceDetailsComparableTest {

    @Test
    public void shouldCompareOffenceDetailsBySequenceNumber() {
        final OffenceDetail firstOffenceDetail = builder().setSequenceNumber(1).build();
        final OffenceDetail secondOffenceDetail = builder().setSequenceNumber(2).build();

        assertThat(secondOffenceDetail, greaterThan(firstOffenceDetail));
        assertThat(secondOffenceDetail, greaterThan(null));
        assertThat(firstOffenceDetail, comparesEqualTo(firstOffenceDetail));
    }
}
