package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
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
public class OffenceRepositoryTest extends BaseTransactionalTest {

    private static final UUID VALID_OFFENCE_DETAIL_ID = UUID.randomUUID();

    @Inject
    private OffenceRepository offenceRepository;

    @Inject
    private CaseRepository caseRepository;

    @Test
    public void shouldFindOptionalBy() {
        //given
        caseRepository.save(getCaseWithDefendantOffences());

        OffenceDetail actual = offenceRepository.findBy(VALID_OFFENCE_DETAIL_ID);
        assertNotNull("Should not be null", actual);
        assertEquals(VALID_OFFENCE_DETAIL_ID, actual.getId());
    }

    private static CaseDetail getCaseWithDefendantOffences() {
        final CaseDetail caseDetail = new CaseDetail(UUID.randomUUID());
        caseDetail.setDefendant(new DefendantDetail(
                UUID.randomUUID(),
                new PersonalDetails(),
                singletonList(getOffenceDetail(VALID_OFFENCE_DETAIL_ID)),
                1
        ));

        return caseDetail;
    }

    private static OffenceDetail getOffenceDetail(final UUID uuid) {
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
