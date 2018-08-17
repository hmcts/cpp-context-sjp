package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;

import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpCaseCreatedToCaseTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @SuppressWarnings("deprecation")
    private SjpCaseCreated event;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private OffenceToOffenceDetail offenceToOffenceDetailConverter;

    @InjectMocks
    private SjpCaseCreatedToCase caseConverter = new SjpCaseCreatedToCase();

    private UUID caseId = UUID.randomUUID();
    private String urn = "TFL243179";
    private UUID defendantId = UUID.randomUUID();
    private List<Offence> offences = new ArrayList<>();

    private UUID offenceId = UUID.randomUUID();
    private int offenceSequenceNo = 1;
    private String libraOffenceCode = "PS00001";
    private LocalDate chargeDate = LocalDate.parse("2016-01-01", formatter);
    private final int libraOffenceDateCode = 6;
    private LocalDate offenceCommittedDate = LocalDate.parse("2016-01-01", formatter);
    private String offenceWording = "Committed some offence";
    private BigDecimal compensation = BigDecimal.ONE;

    private ZonedDateTime createdOn = ZonedDateTime.now(UTC);

    @Before
    @SuppressWarnings("deprecation")
    public void setup() {
        Offence offence = new Offence(offenceId, offenceSequenceNo, libraOffenceCode, chargeDate,
                libraOffenceDateCode, offenceCommittedDate, offenceWording, "Prosecution facts", "Witness statement", compensation);

        offences.clear();
        offences.add(offence);

        event = null;

        int numPreviousConvictions = 30;
        BigDecimal costs = BigDecimal.valueOf(33.5);
        LocalDate postingDate = LocalDate.parse("2016-12-03", formatter);
        event = new SjpCaseCreated(caseId, urn, TFL, defendantId, numPreviousConvictions, costs,
                postingDate, offences, createdOn);
    }

    @Test
    public void shouldConvertToCase() {
        CaseDetail aCase = caseConverter.convert(event);

        assertThat(aCase, is(notNullValue()));
        assertThat(aCase.getId(), is(caseId));
        assertThat(aCase.getUrn(), is(urn));
        assertThat(aCase.getProsecutingAuthority(), is(TFL));
        assertThat(aCase.getCompleted(), is(false));
        assertThat(aCase.getAssigneeId(), nullValue());

        assertThat(aCase.getDefendant().getNumPreviousConvictions(), is(30)); // assuming there is just one defendant for now
        assertThat(aCase.getCosts(), is(BigDecimal.valueOf(33.5)));
        assertThat(aCase.getPostingDate(), is(LocalDate.parse("2016-12-03", formatter)));
        assertThat(aCase.getDateTimeCreated(), is(createdOn));
    }

    @Test
    public void shouldHaveDefendant() {
        DefendantDetail defendant = caseConverter.convert(event).getDefendant();

        assertThat(defendant, is(notNullValue()));
        assertThat(defendant.getId(), is(defendantId));
    }

    @Test
    public void shouldHaveOffences() {
        DefendantDetail defendant = caseConverter.convert(event).getDefendant();
        Set<OffenceDetail> offences = defendant.getOffences();

        assertThat(offences, is(notNullValue()));
        assertThat(offences.size(), is(1));

        OffenceDetail offenceDetail = offences.iterator().next();
        assertThat(offenceDetail.getId(), is(offenceId));
        assertThat(offenceDetail.getCode(), is(libraOffenceCode));
        assertThat(offenceDetail.getSequenceNumber(), is(offenceSequenceNo));
        assertThat(offenceDetail.getWording(), is(offenceWording));
        assertThat(offenceDetail.getStartDate(), is(offenceCommittedDate));
        assertThat(offenceDetail.getWitnessStatement(), is("Witness statement"));
        assertThat(offenceDetail.getProsecutionFacts(), is("Prosecution facts"));
        assertThat(offenceDetail.getLibraOffenceDateCode(), is(libraOffenceDateCode));
        assertThat(offenceDetail.getDefendantDetail(), is(defendant));
    }
}
