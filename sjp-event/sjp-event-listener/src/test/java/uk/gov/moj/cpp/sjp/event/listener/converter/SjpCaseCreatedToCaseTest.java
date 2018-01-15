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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpCaseCreatedToCaseTest {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @SuppressWarnings("deprecation")
    private SjpCaseCreated event;
    @SuppressWarnings("deprecation")
    private SjpCaseCreatedToCase caseConverter;

    private String caseId = UUID.randomUUID().toString();
    private String urn = "TFL243179";
    private String ptiUrn = "TFL243179";
    private String initiationCode = "J";
    private String summonsCode = "M";
    private String libraOriginatingOrg = "GAFTL00";
    private String libraHearingLocation = "C01CE03";
    private LocalDate dateOfHearing = LocalDate.parse("2016-01-01", formatter);
    private String timeOfHearing = "11:00";
    private UUID defendantId = UUID.randomUUID();
    private List<Offence> offences = new ArrayList<>();

    private UUID offenceId = UUID.randomUUID();
    private String prosecutorCaseId = "TFL21345";
    private int offenceSequenceNo = 1;
    private String libraOffenceCode = "PS00001";
    private LocalDate chargeDate = LocalDate.parse("2016-01-01", formatter);
    private int libraOffenceDateCode = 6;
    private LocalDate offenceDate = LocalDate.parse("2016-01-01", formatter);
    private String offenceWording = "Committed some offence";
    private BigDecimal compensation = BigDecimal.ONE;

    private ZonedDateTime createdOn = ZonedDateTime.now(UTC);

    @Before
    @SuppressWarnings("deprecation")
    public void setup() {
        Offence offence = new Offence(offenceId, offenceSequenceNo, libraOffenceCode, chargeDate,
                libraOffenceDateCode, offenceDate, offenceWording, "Prosecution facts", "Witness statement", compensation);

        offences.clear();
        offences.add(offence);

        caseConverter = new SjpCaseCreatedToCase();
        event = null;

        int numPreviousConvictions = 30;
        BigDecimal costs = BigDecimal.valueOf(33.5);
        LocalDate postingDate = LocalDate.parse("2016-12-03", formatter);
        event = new SjpCaseCreated(caseId, urn, ptiUrn, initiationCode, summonsCode, TFL, libraOriginatingOrg,
                libraHearingLocation, dateOfHearing, timeOfHearing, defendantId,
                numPreviousConvictions, costs, postingDate, offences, createdOn);
    }

    @Test
    public void shouldConvertToCase() {
        CaseDetail kase = caseConverter.convert(event);

        assertThat(kase, is(notNullValue()));
        assertThat(kase.getId(), is(UUID.fromString(caseId)));
        assertThat(kase.getUrn(), is(urn));
        assertThat(kase.getProsecutingAuthority(), is("TFL"));
        assertThat(kase.getInitiationCode(), is(initiationCode));
        assertThat(kase.getCompleted(), is(false));
        assertThat(kase.getAssigneeId(), nullValue());

        assertThat(kase.getDefendant().getNumPreviousConvictions(), is(30)); // assuming there is just one defendant for now
        assertThat(kase.getCosts(), is(BigDecimal.valueOf(33.5)));
        assertThat(kase.getPostingDate(), is(LocalDate.parse("2016-12-03", formatter)));
        assertThat(kase.getDateTimeCreated(), is(createdOn));
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
        assertThat(offenceDetail.getStartDate(), is(offenceDate));
        assertThat(offenceDetail.getWitnessStatement(), is("Witness statement"));
        assertThat(offenceDetail.getProsecutionFacts(), is("Prosecution facts"));
        assertThat(offenceDetail.getLibraOffenceDateCode(), is(6));
        assertThat(offenceDetail.getDefendantDetail(), is(defendant));
    }
}
