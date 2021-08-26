package uk.gov.moj.cpp.sjp.persistence.repository;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
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
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

@RunWith(CdiTestRunner.class)
public class DefendantRepositoryTest extends BaseTransactionalTest {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Test
    public void shouldFindDefendantWithDoBUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(ZonedDateTime.now(UTC));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails, contains(defendant));
    }

    @Test
    public void shouldFindDefendantWithDoBUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(ZonedDateTime.now(UTC));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now().minusDays(2));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL",  ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails, contains(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithDoBUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(ZonedDateTime.now(UTC).minusDays(2));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC));

        createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL",  ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithAddressUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(ZonedDateTime.now(UTC));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL",  ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWhenUpdateHappenedMoreThan10DaysAgo() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(ZonedDateTime.now(UTC).minusDays(15));

        createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithAddressUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(ZonedDateTime.now(UTC));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC).minusDays(2));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithAddressUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(ZonedDateTime.now(UTC).minusDays(2));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC));

        createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithNameUpdatedAndUpdatesNotAcknowledgedYet() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markNameUpdated(ZonedDateTime.now(UTC));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldFindDefendantWithNameUpdatedAndUpdatesAcknowledgedBefore() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markNameUpdated(ZonedDateTime.now(UTC));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC).minusDays(2));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithNameUpdateWhenAcknowledged() {
        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markNameUpdated(ZonedDateTime.now(UTC).minusDays(2));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC));

        createCaseDetail(personalDetails, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldIgnoreDefendantWithNoDetailChanges() {
        createCaseDetail(new PersonalDetails(), "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TVL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldIgnoreDefendantForOtherAuthorityGroup() {
        createCaseDetail(null, "TVL");

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority("TFL", ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldfindCaseIdByDefendantId() {
        final UpdatedDefendantDetails caseDetail = createCaseDetail(new PersonalDetails(), "TVL");

        final UUID actualCaseId = defendantRepository.findCaseIdByDefendantId(caseDetail.getDefendantId());

        assertThat(actualCaseId, equalTo(caseDetail.getCaseId()));
    }


    @Test
    public void shouldFindDefendantsByReadyCases() {
        final UpdatedDefendantDetails caseDetail = createCaseDetail(new PersonalDetails(), "TVL");

        final List<DefendantDetail> byReadyCases = defendantRepository.findByReadyCases();

        assertThat(byReadyCases, iterableWithSize(1));
        assertThat(byReadyCases.get(0).getId(), is(caseDetail.getDefendantId()));
        assertThat(byReadyCases.get(0).getCaseDetail().getId(), is(caseDetail.getCaseId()));
        assertThat(byReadyCases.get(0).getPersonalDetails().getFirstName(), is(caseDetail.getFirstName()));
    }

    private UpdatedDefendantDetails createCaseDetail(
            final PersonalDetails personalDetails,
            final String prosecutingAuthority) {

        final DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withId(UUID.randomUUID())
                .withPersonalDetails(personalDetails)
                .build();

        final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withCaseId(UUID.randomUUID())
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantDetail(defendantDetail)
                .build();

        final DefendantDetail defendant = caseRepository.save(caseDetail).getDefendant();

        final ReadyCase readyCase = new ReadyCase(caseDetail.getId(), CaseReadinessReason.DEFAULT_STATUS, UUID.randomUUID(), SessionType.MAGISTRATE, 3,"TFL", LocalDate.now());

        readyCaseRepository.save(readyCase);

        return new UpdatedDefendantDetails(
                defendant.getPersonalDetails().getFirstName(),
                defendant.getPersonalDetails().getLastName(),
                defendant.getPersonalDetails().getDateOfBirth(),
                defendant.getId(),
                defendant.getPersonalDetails().getAddressUpdatedAt(),
                defendant.getPersonalDetails().getDateOfBirthUpdatedAt(),
                defendant.getPersonalDetails().getNameUpdatedAt(),
                defendant.getCaseDetail().getUrn(),
                defendant.getCaseDetail().getId(),
                defendant.getPersonalDetails().getRegion());
    }

}
