package uk.gov.moj.cpp.sjp.persistence.repository;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;

import javax.inject.Inject;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

@RunWith(CdiTestRunner.class)
public class DefendantRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Test
    public void shouldFindDefendantWithDoBUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(now(UTC));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", null,null,null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails, contains(defendant));
    }

    @Test
    public void shouldFindDefendantWithDoBUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(now(UTC));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", now().minusDays(2),null,null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails, contains(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithDoBUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(now(UTC).minusDays(2));

        createCaseDetail(personalDetails, "TVL", now(UTC),null,null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithAddressUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", null,now(UTC),null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWhenUpdateHappenedMoreThan10DaysAgo() {
        final PersonalDetails personalDetails = new PersonalDetails();

        createCaseDetail(personalDetails, "TVL", null, now(UTC).minusDays(15), null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithAddressUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", now(UTC).minusDays(2), now(UTC), null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithAddressUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();

        createCaseDetail(personalDetails, "TVL", now(UTC), now(UTC).minusDays(2), null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithNameUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", null, null, now(UTC));

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldFindDefendantWithNameUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL", now(UTC).minusDays(2), null, now(UTC));

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithNameUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();

        createCaseDetail(personalDetails, "TVL", now(UTC), null, now(UTC).minusDays(2));

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldIgnoreDefendantWithNoDetailChanges() {
        createCaseDetail(new PersonalDetails(), "TVL", null, null, null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldIgnoreDefendantForOtherAuthorityGroup() {
        createCaseDetail(null, "TVL", null, null, null);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TFL", now().minusDays(10), now(), Collections.emptyList());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldfindCaseIdByDefendantId() {
        final UpdatedDefendantDetails caseDetail = createCaseDetail(new PersonalDetails(), "TVL", null, null, null);

        final UUID actualCaseId = defendantRepository.findCaseIdByDefendantId(caseDetail.getDefendantId());

        assertThat(actualCaseId, equalTo(caseDetail.getCaseId()));
    }


    @Test
    public void shouldFindDefendantsByReadyCases() {
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
                null);
    }

}
