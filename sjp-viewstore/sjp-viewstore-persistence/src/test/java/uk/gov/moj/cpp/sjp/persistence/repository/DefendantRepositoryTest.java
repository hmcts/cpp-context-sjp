package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class DefendantRepositoryTest {

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private final UtcClock clock = new UtcClock();

    private CaseRepository caseRepository;

    private DefendantRepository defendantRepository;

    private ReadyCaseRepository readyCaseRepository;

    @BeforeEach
    void createRepositoriesWithInjectedEntityManager() {
        caseRepository = new CaseRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseRepository);

        defendantRepository = new DefendantRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(defendantRepository);

        readyCaseRepository = new ReadyCaseRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(readyCaseRepository);
    }

    @Test
    void shouldFindDefendantWithDoBUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(clock.now());

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", null,null,null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        final UpdatedDefendantDetails found = defendantDetails.get(0);
        assertThat(found.getDefendantId(), is(defendant.getDefendantId()));
        assertThat(found.getFirstName(), is(defendant.getFirstName()));
        assertThat(found.getLastName(), is(defendant.getLastName()));
        assertThat(found.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(found.getCaseId(), is(defendant.getCaseId()));
        assertThat(found.getCaseUrn(), is(defendant.getCaseUrn()));
        assertThat(found.getRegion(), is(defendant.getRegion()));
        assertThat(found.getLegalEntityName(), is(defendant.getLegalEntityName()));
        assertThat(found.getProsecutingAuthority(), is(defendant.getProsecutingAuthority()));
        assertThat(found.getAddressUpdatedAt(), is(defendant.getAddressUpdatedAt()));
        assertThat(found.getDateOfBirthUpdatedAt(), is(defendant.getDateOfBirthUpdatedAt()));
        assertThat(found.getNameUpdatedAt(), is(defendant.getNameUpdatedAt()));
    }

    @Test
    void shouldFindDefendantWithDoBUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(clock.now());

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", clock.now().minusDays(2),null,null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        final UpdatedDefendantDetails found = defendantDetails.get(0);
        assertThat(found.getDefendantId(), is(defendant.getDefendantId()));
        assertThat(found.getFirstName(), is(defendant.getFirstName()));
        assertThat(found.getLastName(), is(defendant.getLastName()));
        assertThat(found.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(found.getCaseId(), is(defendant.getCaseId()));
        assertThat(found.getCaseUrn(), is(defendant.getCaseUrn()));
        assertThat(found.getRegion(), is(defendant.getRegion()));
        assertThat(found.getLegalEntityName(), is(defendant.getLegalEntityName()));
        assertThat(found.getProsecutingAuthority(), is(defendant.getProsecutingAuthority()));
        assertThat(found.getAddressUpdatedAt(), is(defendant.getAddressUpdatedAt()));
        assertThat(found.getDateOfBirthUpdatedAt(), is(defendant.getDateOfBirthUpdatedAt()));
        assertThat(found.getNameUpdatedAt(), is(defendant.getNameUpdatedAt()));
    }

    @Test
    void shouldIgnoreDefendantWithDoBUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(clock.now().minusDays(2));

        createCaseDetail(personalDetails, "TVL", clock.now(),null,null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    void shouldFindDefendantWithAddressUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", null,clock.now(),null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        final UpdatedDefendantDetails found = defendantDetails.get(0);
        assertThat(found.getDefendantId(), is(defendant.getDefendantId()));
        assertThat(found.getFirstName(), is(defendant.getFirstName()));
        assertThat(found.getLastName(), is(defendant.getLastName()));
        assertThat(found.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(found.getCaseId(), is(defendant.getCaseId()));
        assertThat(found.getCaseUrn(), is(defendant.getCaseUrn()));
        assertThat(found.getRegion(), is(defendant.getRegion()));
        assertThat(found.getLegalEntityName(), is(defendant.getLegalEntityName()));
        assertThat(found.getProsecutingAuthority(), is(defendant.getProsecutingAuthority()));
        assertThat(found.getAddressUpdatedAt(), is(defendant.getAddressUpdatedAt()));
        assertThat(found.getDateOfBirthUpdatedAt(), is(defendant.getDateOfBirthUpdatedAt()));
        assertThat(found.getNameUpdatedAt(), is(defendant.getNameUpdatedAt()));
    }

    @Test
    void shouldIgnoreDefendantWhenUpdateHappenedMoreThan10DaysAgo() {
        final PersonalDetails personalDetails = new PersonalDetails();

        createCaseDetail(personalDetails, "TVL", null, clock.now().minusDays(15), null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    void shouldFindDefendantWithAddressUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", clock.now().minusDays(2), clock.now(), null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        final UpdatedDefendantDetails found = defendantDetails.get(0);
        assertThat(found.getDefendantId(), is(defendant.getDefendantId()));
        assertThat(found.getFirstName(), is(defendant.getFirstName()));
        assertThat(found.getLastName(), is(defendant.getLastName()));
        assertThat(found.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(found.getCaseId(), is(defendant.getCaseId()));
        assertThat(found.getCaseUrn(), is(defendant.getCaseUrn()));
        assertThat(found.getRegion(), is(defendant.getRegion()));
        assertThat(found.getLegalEntityName(), is(defendant.getLegalEntityName()));
        assertThat(found.getProsecutingAuthority(), is(defendant.getProsecutingAuthority()));
        assertThat(found.getAddressUpdatedAt(), is(defendant.getAddressUpdatedAt()));
        assertThat(found.getDateOfBirthUpdatedAt(), is(defendant.getDateOfBirthUpdatedAt()));
        assertThat(found.getNameUpdatedAt(), is(defendant.getNameUpdatedAt()));
    }

    @Test
    void shouldIgnoreDefendantWithAddressUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();

        createCaseDetail(personalDetails, "TVL", clock.now(), clock.now().minusDays(2), null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    void shouldFindDefendantWithNameUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", null, null, clock.now());

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        final UpdatedDefendantDetails found = defendantDetails.get(0);
        assertThat(found.getDefendantId(), is(defendant.getDefendantId()));
        assertThat(found.getFirstName(), is(defendant.getFirstName()));
        assertThat(found.getLastName(), is(defendant.getLastName()));
        assertThat(found.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(found.getCaseId(), is(defendant.getCaseId()));
        assertThat(found.getCaseUrn(), is(defendant.getCaseUrn()));
        assertThat(found.getRegion(), is(defendant.getRegion()));
        assertThat(found.getLegalEntityName(), is(defendant.getLegalEntityName()));
        assertThat(found.getProsecutingAuthority(), is(defendant.getProsecutingAuthority()));
        assertThat(found.getAddressUpdatedAt(), is(defendant.getAddressUpdatedAt()));
        assertThat(found.getDateOfBirthUpdatedAt(), is(defendant.getDateOfBirthUpdatedAt()));
        assertThat(found.getNameUpdatedAt(), is(defendant.getNameUpdatedAt()));
    }

    @Test
    void shouldFindDefendantWithNameUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", clock.now().minusDays(2), null, clock.now());

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        final UpdatedDefendantDetails found = defendantDetails.get(0);
        assertThat(found.getDefendantId(), is(defendant.getDefendantId()));
        assertThat(found.getFirstName(), is(defendant.getFirstName()));
        assertThat(found.getLastName(), is(defendant.getLastName()));
        assertThat(found.getDateOfBirth(), is(defendant.getDateOfBirth()));
        assertThat(found.getCaseId(), is(defendant.getCaseId()));
        assertThat(found.getCaseUrn(), is(defendant.getCaseUrn()));
        assertThat(found.getRegion(), is(defendant.getRegion()));
        assertThat(found.getLegalEntityName(), is(defendant.getLegalEntityName()));
        assertThat(found.getProsecutingAuthority(), is(defendant.getProsecutingAuthority()));
        assertThat(found.getAddressUpdatedAt(), is(defendant.getAddressUpdatedAt()));
        assertThat(found.getDateOfBirthUpdatedAt(), is(defendant.getDateOfBirthUpdatedAt()));
        assertThat(found.getNameUpdatedAt(), is(defendant.getNameUpdatedAt()));
    }

    @Test
    void shouldIgnoreDefendantWithNameUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();

        createCaseDetail(personalDetails, "TVL", clock.now(), null, clock.now().minusDays(2));

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    void shouldIgnoreDefendantWithNoDetailChanges() {
        createCaseDetail(new PersonalDetails(), "TVL", null, null, null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    void shouldIgnoreDefendantForOtherAuthorityGroup() {
        createCaseDetail(null, "TVL", null, null, null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TFL", clock.now().minusDays(10), clock.now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    void shouldfindCaseIdByDefendantId() {
        final UpdatedDefendantDetails caseDetail = createCaseDetail(new PersonalDetails(), "TVL", null, null, null);

        final UUID actualCaseId = defendantRepository.findCaseIdByDefendantId(caseDetail.getDefendantId());

        assertThat(actualCaseId, equalTo(caseDetail.getCaseId()));
    }


    @Test
    void shouldFindDefendantsByReadyCases() {
        final UpdatedDefendantDetails caseDetail = createCaseDetail(new PersonalDetails(), "TVL", null, null, null);

        final List<DefendantDetail> byReadyCases = defendantRepository.findByReadyCases();

        assertThat(byReadyCases, iterableWithSize(1));
        assertThat(byReadyCases.get(0).getId(), is(caseDetail.getDefendantId()));
        assertThat(byReadyCases.get(0).getCaseDetail().getId(), is(caseDetail.getCaseId()));
        assertThat(byReadyCases.get(0).getPersonalDetails().getFirstName(), is(caseDetail.getFirstName()));
    }

    private UpdatedDefendantDetails createCaseDetail(
            final PersonalDetails personalDetails,
            final String prosecutingAuthority,
            final ZonedDateTime updatesAcknowledgedAt,
            final ZonedDateTime addressUpdatedAt,
            final ZonedDateTime nameUpdatedAt) {

        final DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withId(UUID.randomUUID())
                .withPersonalDetails(personalDetails)
                .withUpdatesAcknowledgedAt(updatesAcknowledgedAt)
                .withAddressUpdatedAt(addressUpdatedAt)
                .withNameUpdatedAt(nameUpdatedAt)
                .build();

        final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withCaseId(UUID.randomUUID())
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantDetail(defendantDetail)
                .build();

        final DefendantDetail defendant = caseRepository.save(caseDetail).getDefendant();

        final ReadyCase readyCase = new ReadyCase(caseDetail.getId(), CaseReadinessReason.DEFAULT_STATUS, UUID.randomUUID(), SessionType.MAGISTRATE, 3, "TFL", LocalDate.now(), LocalDate.now());

        readyCaseRepository.save(readyCase);

        return new UpdatedDefendantDetails(
                defendant.getPersonalDetails().getFirstName(),
                defendant.getPersonalDetails().getLastName(),
                defendant.getPersonalDetails().getDateOfBirth(),
                defendant.getId(),
                defendant.getAddressUpdatedAt(),
                defendant.getPersonalDetails().getDateOfBirthUpdatedAt(),
                defendant.getNameUpdatedAt(),
                defendant.getCaseDetail().getUrn(),
                defendant.getCaseDetail().getId(),
                defendant.getRegion(),
                null,
                "TVL");
    }

}
