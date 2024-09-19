package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * DB integration tests for {@link OffenceRepository} class
 */
@RunWith(CdiTestRunner.class)
public class OffenceRepositoryTest extends BaseTransactionalJunit4Test {

    private static final int NUM_PREVIOUS_CONVICTIONS = 1;

    @Inject
    private OffenceRepository offenceRepository;

    @Inject
    private CaseRepository caseRepository;

    @Test
    public void shouldFindOffencesByIds() {
        final UUID offenceId1 = UUID.randomUUID();
        final UUID offenceId2 = UUID.randomUUID();
        final UUID offenceId3 = UUID.randomUUID();
        final CaseDetail caseDetail = getCaseWithDefendantOffences(asList(offenceId1, offenceId2, offenceId3));
        caseRepository.save(caseDetail);

        final List<OffenceDetail> actual = offenceRepository.findByIds(asList(offenceId1));

        assertThat(actual, hasSize(1));
        assertThat(actual, contains(allOf(hasProperty("id", equalTo(offenceId1)))));
    }

    private CaseDetail getCaseWithDefendantOffences(final List<UUID> offenceIds) {
        final CaseDetail caseDetail = new CaseDetail(UUID.randomUUID());
        caseDetail.setDefendant(new DefendantDetail(
                UUID.randomUUID(),
                new PersonalDetails(),
                offenceIds.stream().map(this::createOffenceDetails).collect(toList()),
                NUM_PREVIOUS_CONVICTIONS,
                new LegalEntityDetails(),
                new Address(),
                new ContactDetails()
        ));

        return caseDetail;
    }

    private OffenceDetail createOffenceDetails(final UUID uuid) {
        return new OffenceDetail.OffenceDetailBuilder()
                .setId(uuid)
                .setChargeDate(LocalDate.now())
                .setSequenceNumber(123)
                .setCode(StringUtils.EMPTY)
                .setPlea(null)
                .setPleaMethod(null)
                .setSequenceNumber(1)
                .setStartDate(LocalDate.now())
                .setWording("")
                .build();
    }

}
