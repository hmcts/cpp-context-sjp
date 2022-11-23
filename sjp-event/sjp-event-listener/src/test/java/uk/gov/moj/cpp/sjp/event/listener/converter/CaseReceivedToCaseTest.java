package uk.gov.moj.cpp.sjp.event.listener.converter;

import static java.time.ZoneOffset.UTC;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_PLEA;

import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseReceivedToCaseTest {

    @Mock
    private DefendantToDefendantDetails defendantToDefendantDetailsConverter;

    @InjectMocks
    private final CaseReceivedToCase caseConverter = new CaseReceivedToCase();

    @Test
    public void shouldCaseReceivedToCase() {
        final CaseReceived event = buildCaseReceived();

        final DefendantDetail mockedDefendantDetails = mock(DefendantDetail.class);
        when(defendantToDefendantDetailsConverter.convert(event.getDefendant())).thenReturn(mockedDefendantDetails);

        final CaseDetail actualCaseDetails = caseConverter.convert(event);

        final CaseDetail expectedCaseDetails = buildExpectedCaseDetail(event, mockedDefendantDetails);

        assertTrue(reflectionEquals(actualCaseDetails, expectedCaseDetails));
    }

    private static CaseDetail buildExpectedCaseDetail(final CaseReceived event, final DefendantDetail defendantDetail) {
        return new CaseDetail(
                event.getCaseId(),
                event.getUrn(),
                event.getEnterpriseId(),
                event.getProsecutingAuthority(),
                false,
                null,
                event.getCreatedOn(),
                defendantDetail,
                event.getCosts(),
                event.getPostingDate(),
                null);
    }

    private static CaseReceived buildCaseReceived() {
        final LocalDate postingDate = LocalDate.of(2016, 1, 3);
        return new CaseReceived(UUID.randomUUID(), "TFL243179", "2K2SLYFC743H", "TFL", BigDecimal.valueOf(33.5),
                postingDate, mock(Defendant.class), postingDate.plusDays(NUMBER_DAYS_WAITING_FOR_PLEA), ZonedDateTime.now(UTC));
    }

}
