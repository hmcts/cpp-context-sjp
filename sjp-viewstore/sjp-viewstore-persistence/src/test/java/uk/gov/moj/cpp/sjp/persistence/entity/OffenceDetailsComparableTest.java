package uk.gov.moj.cpp.sjp.persistence.entity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.greaterThan;
import static uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail.builder;

import org.junit.Test;

public class OffenceDetailsComparableTest {

    @Test
    public void shouldCompareOffenceDetailsByOrderIndex() {
        final OffenceDetail firstOffenceDetail = builder().withOrderIndex(1).build();
        final OffenceDetail secondOffenceDetail = builder().withOrderIndex(2).build();

        assertThat(secondOffenceDetail, greaterThan(firstOffenceDetail));
        assertThat(secondOffenceDetail, greaterThan(null));
        assertThat(firstOffenceDetail, comparesEqualTo(firstOffenceDetail));
    }
}
