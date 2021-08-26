
package uk.gov.moj.cpp.sjp.persistence.repository;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType.STAT_DEC;

@Ignore
@RunWith(CdiTestRunner.class)
public class CaseApplicationRepositoryTest extends BaseTransactionalTest {

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID APPLICATION_TYPE_ID = UUID.randomUUID();
    private static final String APPLICATION_REFERENCE = "ref";
    private static final String APPLICATION_REASON = "reason";
    private static final LocalDate DATE_RECEIVED = LocalDate.now();
    private static final String PROSECUTING_AUTHORITY = "TFL";
    private static final int NUM_PREVIOUS_CONVICTIONS = 3;
    private static final BigDecimal COSTS = BigDecimal.valueOf(10.33);
    private static final UUID VALID_MATERIAL_ID_1 = randomUUID();
    private static final UUID VALID_MATERIAL_ID_2 = randomUUID();
    private static final String POSTCODE = "CR0 1AB";
    private static final String OFFENCE_CODE = "PS0001";
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static LocalDate postingDate = LocalDate.of(2015, 12, 31);
    private static final String URN = "TFL12345678A";
    private static  CaseDetail caseDetail;

    @Inject
    private Clock clock;

    @Inject
    private CaseApplicationRepository caseApplicationRepository;

    @Inject
    private CaseRepository caseRepository;

    @Test
    public void testFindByApplicationId() {
        caseDetail = createCase(CASE_ID, URN, DEFENDANT_ID, POSTCODE, VALID_MATERIAL_ID_1, VALID_MATERIAL_ID_2);
        caseRepository.save(caseDetail);
        caseApplicationRepository.save(getCaseApplication());
        caseApplicationRepository.findBy(APPLICATION_ID);
        final CaseApplication actual = caseApplicationRepository.findBy(APPLICATION_ID);
        Assert.assertEquals(APPLICATION_ID, actual.getApplicationId());
        Assert.assertEquals(APPLICATION_TYPE_ID, actual.getTypeId());
        Assert.assertEquals(APPLICATION_REFERENCE, actual.getApplicationReference());
        Assert.assertEquals(APPLICATION_REASON, actual.getOutOfTimeReason());
        Assert.assertEquals(STAT_DEC, actual.getApplicationType());
    }

    private CaseApplication getCaseApplication() {
        final CaseApplication caseApplication = new CaseApplication();
        caseApplication.setApplicationId(APPLICATION_ID);
        caseApplication.setCaseDetail(createCase(CASE_ID, URN, DEFENDANT_ID, POSTCODE, VALID_MATERIAL_ID_1, VALID_MATERIAL_ID_2));
        caseApplication.setTypeId(APPLICATION_TYPE_ID);
        caseApplication.setDateReceived(DATE_RECEIVED);
        caseApplication.setTypeCode(APPLICATION_TYPE_ID.toString());
        caseApplication.setApplicationReference(APPLICATION_REFERENCE);
        caseApplication.setApplicationType(STAT_DEC);
        caseApplication.setOutOfTime(true);
        caseApplication.setOutOfTimeReason(APPLICATION_REASON);
        return caseApplication;
    }

    private CaseDetail createCase(final UUID caseId, final String urn, final UUID defendantId, final String postcode, final UUID... materialIds) {

        postingDate = postingDate.minusDays(1);

        final CaseDetailBuilder caseDetailBuilder = CaseDetailBuilder.aCase()
                .withCaseId(caseId)
                .withUrn(urn)
                .withProsecutingAuthority(PROSECUTING_AUTHORITY)
                .withCosts(COSTS)
                .withPostingDate(postingDate)
                .withDefendantDetail(DefendantDetailBuilder.aDefendantDetail()
                        .withId(defendantId)
                        .withPostcode(postcode)
                        .withLastName(RandomGenerator.string(10).next())
                        .withOffenceCode(OFFENCE_CODE)
                        .withNumberOfPreviousConvictions(NUM_PREVIOUS_CONVICTIONS)
                        .build())
                .withCreatedOn(clock.now());

        for (final UUID materialId : materialIds) {
            caseDetailBuilder.addCaseDocument(getCaseDocument(caseId, materialId));
        }

        return caseDetailBuilder.build();
    }

    private CaseDocument getCaseDocument(final UUID caseId, final UUID materialId) {
        return new CaseDocument(randomUUID(), materialId, "SJPN", clock.now(), caseId, 1);
    }
}
