package uk.gov.moj.cpp.sjp.event.listener.converter;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty.createNoSeparatePenalty;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredForFutureSJPSession;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredToOpenCourt;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.persistence.entity.NoSeparatePenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferredForFutureSJPSessionDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferredToOpenCourtDecision;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class OffenceDecisionConverterTest {

    private UUID caseDecisionId = UUID.randomUUID();

    @Test
    public void shouldConvertTheReferredToOpenCourtEventToEntity() {
        final UUID offenceId = UUID.randomUUID();

        final OffenceDecisionInformation offenceDecisionInformation =
                new OffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT);

        final ReferredToOpenCourt referredToOpenCourt = new ReferredToOpenCourt(caseDecisionId,
                Arrays.asList(offenceDecisionInformation),
                "South West London Magistrates' Court",
                25,
                ZonedDateTime.parse("2018-11-02T09:30:00.000Z"),
                "For a case management hearing (no appearance)",
                "Lavender Hill Magistrates' Court"
        );

        final List<OffenceDecision> offenceDecisions =
                OffenceDecisionConverter.convert(caseDecisionId, referredToOpenCourt);

        final ReferredToOpenCourtDecision offenceDecision = (ReferredToOpenCourtDecision) offenceDecisions.get(0);
        assertThat(offenceDecision, notNullValue());

        assertThat(offenceDecision.getReferredToCourt(), equalTo("South West London Magistrates' Court"));
        assertThat(offenceDecision.getReferredToRoom(), equalTo(25));
        assertThat(offenceDecision.getReferredToDateTime(), equalTo(ZonedDateTime.parse("2018-11-02T09:30:00.000Z")));
        assertThat(offenceDecision.getReason(), equalTo("For a case management hearing (no appearance)"));
        assertThat(offenceDecision.getMagistratesCourt(), equalTo("Lavender Hill Magistrates' Court"));

        assertThat(offenceDecision.getOffenceId(), equalTo(offenceId));
        assertThat(offenceDecision.getVerdictType(), equalTo(VerdictType.NO_VERDICT));
    }

    @Test
    public void shouldConvertTheReferredForFutureSJPSessionToEntity() {
        final UUID offenceId = UUID.randomUUID();
        final OffenceDecisionInformation offenceDecisionInformation =
                new OffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT);

        final ReferredForFutureSJPSession referredForFutureSJPSession =
                new ReferredForFutureSJPSession(caseDecisionId, Arrays.asList(offenceDecisionInformation));

        final List<OffenceDecision> offenceDecisions =
                OffenceDecisionConverter.convert(caseDecisionId, referredForFutureSJPSession);

        final ReferredForFutureSJPSessionDecision offenceDecision = (ReferredForFutureSJPSessionDecision) offenceDecisions.get(0);
        assertThat(offenceDecision, notNullValue());

        assertThat(offenceDecision.getOffenceId(), equalTo(offenceId));
        assertThat(offenceDecision.getVerdictType(), equalTo(VerdictType.NO_VERDICT));
    }

    @Test
    public void shouldConvertNoSeparatePenaltyEntities() {
        final UUID offenceId = UUID.randomUUID();
        LocalDate convictionDate = LocalDate.now();
        final NoSeparatePenalty noSeparatePenalty = createNoSeparatePenalty(null,
                createOffenceDecisionInformation(offenceId, PROVED_SJP), true, true);
        noSeparatePenalty.setConvictionDate(convictionDate);

        final List<OffenceDecision> converter = OffenceDecisionConverter.convert(caseDecisionId, noSeparatePenalty);

        assertThat(converter, hasSize(1));
        assertThat(converter.get(0).getDecisionType(), equalTo(DecisionType.NO_SEPARATE_PENALTY));
        NoSeparatePenaltyOffenceDecision offenceDecision = (NoSeparatePenaltyOffenceDecision) converter.get(0);
        assertThat(offenceDecision.getOffenceId(), equalTo(offenceId));
        assertThat(offenceDecision.getCaseDecisionId(), equalTo(caseDecisionId));
        assertThat(offenceDecision.getVerdictType(), equalTo(PROVED_SJP));
        assertThat(offenceDecision.getConvictionDate(), equalTo(convictionDate));
        assertThat(offenceDecision.getGuiltyPleaTakenIntoAccount(), equalTo(true));
        assertThat(offenceDecision.getLicenceEndorsement(), equalTo(true));
    }
}