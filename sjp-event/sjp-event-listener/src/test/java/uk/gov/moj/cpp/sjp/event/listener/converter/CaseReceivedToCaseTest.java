package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;

public class CaseReceivedToCaseTest {

    private final CaseReceivedToCase caseConverter = new CaseReceivedToCase();

    private DefendantToDefendantDetails defendantToDefendantDetails = new DefendantToDefendantDetails(null);

    @Test
    public void shouldCaseReceivedToCase() {
        CaseReceived event = buildCaseReceived();

        CaseDetail actualCaseDetails = caseConverter.convert(event);

        CaseDetail expectedCaseDetails = buildExpectedCaseDetail(event);

        assertThat(actualCaseDetails.getDefendant().getCaseDetail(), is(actualCaseDetails));

        assertTrue(reflectionEquals(actualCaseDetails, expectedCaseDetails, "defendant"));

        DefendantDetail actualDefendantDetail = actualCaseDetails.getDefendant();
        DefendantDetail expectedDefendantDetail = expectedCaseDetails.getDefendant();

        assertTrue(reflectionEquals(actualDefendantDetail, expectedDefendantDetail, "caseDetail", "personalDetails", "offences"));

        assertTrue(reflectionEquals(actualDefendantDetail.getPersonalDetails(), expectedDefendantDetail.getPersonalDetails(), "contactDetails", "address"));
        assertTrue(reflectionEquals(actualDefendantDetail.getPersonalDetails().getContactDetails(), expectedDefendantDetail.getPersonalDetails().getContactDetails()));
        assertTrue(reflectionEquals(actualDefendantDetail.getPersonalDetails().getAddress(), expectedDefendantDetail.getPersonalDetails().getAddress()));

        assertTrue(reflectionEquals(actualDefendantDetail.getOffences(), expectedDefendantDetail.getOffences()));
        actualDefendantDetail.getOffences().forEach(o -> assertThat(o.getDefendantDetail(), is(actualDefendantDetail)));
    }

    private CaseDetail buildExpectedCaseDetail(CaseReceived event) {
        return new CaseDetail(event.getCaseId(), event.getUrn(),
                event.getProsecutingAuthority(),
                null,
                false,
                null,
                event.getCreatedOn(),
                defendantToDefendantDetails.convert(event.getDefendant()),
                event.getCosts(),
                event.getPostingDate());
    }

    private static CaseReceived buildCaseReceived() {
        Offence offence = new Offence(UUID.randomUUID(), 1, "PS00001",
                LocalDate.of(2016, 1, 1), 6,
                LocalDate.of(2016, 1, 2), "Committed some offence",
                "Prosecution facts", "Witness statement", BigDecimal.ONE);

        Defendant defendant = new Defendant(randomUUID(), "Mr", "Roger", "Smith",
                LocalDate.of(1980, 1, 1), "M",
                new Address("a1", "a2", "a3", "a4", "pc"),
                30, singletonList(offence));

        return new CaseReceived(UUID.randomUUID(), "TFL243179", TFL, BigDecimal.valueOf(33.5),
                LocalDate.of(2016, 1, 3), defendant, ZonedDateTime.now(UTC));
    }

}
