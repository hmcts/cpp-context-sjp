package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpCaseCreatedToCaseTest {

    private static final UUID caseId = UUID.randomUUID();
    private static final String urn = "TFL243179";
    private static final UUID defendantId = UUID.randomUUID();

    private static final UUID offenceId = UUID.randomUUID();
    private static final int offenceSequenceNo = 1;
    private static final String libraOffenceCode = "PS00001";
    private static final LocalDate chargeDate = LocalDate.of(2016, 1, 1);
    private static final int libraOffenceDateCode = 6;
    private static final LocalDate offenceCommittedDate = LocalDate.of(2016, 1, 2);
    private static final String offenceWording = "Committed some offence";
    private static final String prosecutionOfFacts = "Prosecution facts";
    private static final String witnessStatement = "Witness statement";
    private static final BigDecimal compensation = BigDecimal.ONE;
    private static final int numPreviousConvictions = 30;
    private static final BigDecimal costs = BigDecimal.valueOf(33.5);
    private static final LocalDate postingDate = LocalDate.of(2016, 12, 3);
    private static final ZonedDateTime createdOn = ZonedDateTime.now(UTC);

    private static final Offence offence = new Offence(offenceId, offenceSequenceNo, libraOffenceCode, chargeDate,
            libraOffenceDateCode, offenceCommittedDate, offenceWording, prosecutionOfFacts, witnessStatement,
            compensation);

    @SuppressWarnings("deprecation")
    private static final SjpCaseCreated event = new SjpCaseCreated(caseId, urn, "TFL", defendantId,
            numPreviousConvictions, costs, postingDate, singletonList(offence), createdOn);

    @Spy
    @SuppressWarnings("unused")
    private final OffenceToOffenceDetail offenceToOffenceDetailConverter = new OffenceToOffenceDetail();

    @InjectMocks
    private final SjpCaseCreatedToCase caseConverter = new SjpCaseCreatedToCase();

    @Test
    public void shouldConvertToCase() {
        final CaseDetail convertedCase = caseConverter.convert(event);

        assertThat(convertedCase, is(notNullValue()));

        assertThat(convertedCase.getId(), is(event.getId()));
        assertThat(convertedCase.getUrn(), is(event.getUrn()));
        assertThat(convertedCase.getProsecutingAuthority(), is(event.getProsecutingAuthority()));
        assertThat(convertedCase.getCosts(), is(event.getCosts()));
        assertThat(convertedCase.getPostingDate(), is(event.getPostingDate()));
        assertThat(convertedCase.getDateTimeCreated(), is(event.getCreatedOn()));
        assertThat(convertedCase.getReopenedInLibraReason(), nullValue());
        assertThat(convertedCase.getEnterpriseId(), nullValue());
        assertThat(convertedCase.getLibraCaseNumber(), nullValue());
        assertThat(convertedCase.getDatesToAvoid(), nullValue());
        assertThat(convertedCase.getAssigneeId(), nullValue());
        assertThat(convertedCase.getCaseDocuments(), empty());
        assertThat(convertedCase.getCaseSearchResults(), empty());
        assertThat(convertedCase.isCompleted(), is(false));
        assertThat(convertedCase.getOnlinePleaReceived(), is(false));

        // Defendant
        final DefendantDetail defendant = convertedCase.getDefendant();
        assertThat(defendant, notNullValue());

        assertThat(defendant.getId(), is(event.getDefendantId()));
        assertThat(defendant.getCaseDetail(), is(convertedCase));
        assertThat(defendant.getNumPreviousConvictions(), is(event.getNumPreviousConvictions()));
        assertThat(defendant.getInterpreter(), nullValue());

        assertThat(defendant.getPersonalDetails(), not(nullValue()));
        assertThat(defendant.getPersonalDetails().getFirstName(), nullValue());

        // offences
        final List<OffenceDetail> offences = defendant.getOffences();
        assertThat(offences, hasSize(1));

        final OffenceDetail offenceDetail = offences.iterator().next();
        assertThat(offenceDetail.getId(), is(offence.getId()));
        assertThat(offenceDetail.getCode(), is(offence.getLibraOffenceCode()));
        assertThat(offenceDetail.getSequenceNumber(), is(offence.getOffenceSequenceNo()));
        assertThat(offenceDetail.getWording(), is(offence.getOffenceWording()));
        assertThat(offenceDetail.getStartDate(), is(offence.getOffenceCommittedDate()));
        assertThat(offenceDetail.getWitnessStatement(), is(offence.getWitnessStatement()));
        assertThat(offenceDetail.getProsecutionFacts(), is("Prosecution facts"));
        assertThat(offenceDetail.getLibraOffenceDateCode(), is(offence.getLibraOffenceDateCode()));
        assertThat(offenceDetail.getDefendantDetail(), is(defendant));
    }

}
