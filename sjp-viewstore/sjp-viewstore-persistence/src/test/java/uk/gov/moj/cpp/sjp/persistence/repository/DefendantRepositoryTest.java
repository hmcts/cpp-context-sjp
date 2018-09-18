package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TVL;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DefendantRepositoryTest extends BaseTransactionalTest {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private DefendantRepository defendantRepository;

    @Test
    public void shouldFindDefendantWithDoBUpdatedAndUpdatesNotAcknowledgedYet() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(ZonedDateTime.now(UTC));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails, contains(defendant));
    }

    @Test
    public void shouldFindDefendantWithDoBUpdatedAndUpdatesAcknowledgedBefore() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(ZonedDateTime.now(UTC));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now().minusDays(2));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(),  ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails, contains(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithDoBUpdateWhenAcknowledged() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markDateOfBirthUpdated(ZonedDateTime.now(UTC).minusDays(2));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC));

        createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(),  ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithAddressUpdatedAndUpdatesNotAcknowledgedYet() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(ZonedDateTime.now(UTC));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(),  ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWhenUpdateHappenedMoreThan10DaysAgo() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(ZonedDateTime.now(UTC).minusDays(15));

        createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithAddressUpdatedAndUpdatesAcknowledgedBefore() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(ZonedDateTime.now(UTC));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC).minusDays(2));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithAddressUpdateWhenAcknowledged() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markAddressUpdated(ZonedDateTime.now(UTC).minusDays(2));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC));

        createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldFindDefendantWithNameUpdatedAndUpdatesNotAcknowledgedYet() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markNameUpdated(ZonedDateTime.now(UTC));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldFindDefendantWithNameUpdatedAndUpdatesAcknowledgedBefore() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markNameUpdated(ZonedDateTime.now(UTC));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC).minusDays(2));

        final UpdatedDefendantDetails defendant = createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(1));
        assertThat(defendantDetails.get(0), is(defendant));
    }

    @Test
    public void shouldIgnoreDefendantWithNameUpdateWhenAcknowledged() {
        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.markNameUpdated(ZonedDateTime.now(UTC).minusDays(2));
        personalDetails.acknowledgeUpdates(ZonedDateTime.now(UTC));

        createCaseDetail(personalDetails, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldIgnoreDefendantWithNoDetailChanges() {
        createCaseDetail(new PersonalDetails(), TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TVL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    @Test
    public void shouldIgnoreDefendantForOtherAuthorityGroup() {
        createCaseDetail(null, TVL);

        final List<UpdatedDefendantDetails> defendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(TFL.name(), ZonedDateTime.now().minusDays(10), ZonedDateTime.now());

        assertThat(defendantDetails, iterableWithSize(0));
    }

    private UpdatedDefendantDetails createCaseDetail(
            final PersonalDetails personalDetails,
            final ProsecutingAuthority prosecutingAuthority) {

        DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withId(UUID.randomUUID())
                .withPersonalDetails(personalDetails)
                .build();

        final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withCaseId(UUID.randomUUID())
                .withProsecutingAuthority(prosecutingAuthority)
                .addDefendantDetail(defendantDetail)
                .build();

        final DefendantDetail defendant = caseRepository.save(caseDetail).getDefendant();

        return new UpdatedDefendantDetails(
                defendant.getPersonalDetails().getFirstName(),
                defendant.getPersonalDetails().getLastName(),
                defendant.getPersonalDetails().getDateOfBirth(),
                defendant.getPersonalDetails().getAddressUpdatedAt(),
                defendant.getPersonalDetails().getDateOfBirthUpdatedAt(),
                defendant.getPersonalDetails().getNameUpdatedAt(),
                defendant.getCaseDetail().getUrn(),
                defendant.getCaseDetail().getId());
    }

}
