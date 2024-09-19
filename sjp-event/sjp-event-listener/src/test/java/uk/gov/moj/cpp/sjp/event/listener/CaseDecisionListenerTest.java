package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static java.util.UUID.randomUUID;

import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.listener.converter.ApplicationDecisionSavedToApplicationDecision;
import uk.gov.moj.cpp.sjp.event.listener.converter.DecisionSavedToCaseDecision;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseApplicationService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PressRestriction;
import uk.gov.moj.cpp.sjp.persistence.entity.SetAsideOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseAccountNoteRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDecisionListenerTest {

    private final UUID caseId = randomUUID();
    private final UUID decisionId = randomUUID();
    private final UUID offence1Id = randomUUID();
    private final UUID offence2Id = randomUUID();
    private final UUID offence3Id = randomUUID();

    private final ZonedDateTime now = now();

    @InjectMocks
    private CaseDecisionListener listener;

    @Mock
    private Envelope<DecisionSaved> envelope;

    @Mock
    private Envelope<ApplicationDecisionSaved> applicationEnvelope;

    @Mock
    private DecisionSaved decisionSavedEvent;

    @Mock
    private ApplicationDecisionSaved applicationDecisionSaved;

    @Mock
    private DecisionSavedToCaseDecision eventConverter;

    @Mock
    private ApplicationDecisionSavedToApplicationDecision applicationDecisionConverter;

    @Mock
    private CaseDecisionRepository caseDecisionRepository;

    @Mock
    CaseAccountNoteRepository caseAccountNoteRepository;

    @Mock
    private CaseApplicationService caseApplicationService;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    @Test
    public void shouldEnrichOffenceDecisionsWithPleaInformation() {
        final OffenceDetail offence1Details = new OffenceDetail();
        offence1Details.setId(offence1Id);
        offence1Details.setSequenceNumber(1);
        offence1Details.setPlea(GUILTY);
        offence1Details.setPleaDate(now.minusDays(2));

        final OffenceDetail offence2Details = new OffenceDetail();
        offence2Details.setId(offence2Id);
        offence2Details.setSequenceNumber(2);
        offence2Details.setPlea(NOT_GUILTY);
        offence2Details.setPleaDate(now.minusDays(1));

        final OffenceDetail offence3Details = new OffenceDetail();
        offence3Details.setId(offence3Id);
        offence3Details.setSequenceNumber(3);

        final DefendantDetail defendantDetails = new DefendantDetail();
        defendantDetails.setOffences(asList(offence1Details, offence2Details, offence3Details));

        final CaseDetail caseDetails = new CaseDetail();
        caseDetails.setId(caseId);
        caseDetails.setDefendant(defendantDetails);

        final OffenceDecision offenceDecision1 = new DismissOffenceDecision(offence1Id, decisionId, NO_VERDICT, null);
        final OffenceDecision offenceDecision2 = new DismissOffenceDecision(offence2Id, decisionId, NO_VERDICT, null);
        final OffenceDecision offenceDecision3 = new DismissOffenceDecision(offence3Id, decisionId, NO_VERDICT, null);

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(decisionId);
        caseDecision.setCaseId(caseId);
        caseDecision.setOffenceDecisions(asList(offenceDecision1, offenceDecision2, offenceDecision3));

        when(envelope.payload()).thenReturn(decisionSavedEvent);
        when(eventConverter.convert(decisionSavedEvent)).thenReturn(caseDecision);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetails);

        listener.handleCaseDecisionSaved(envelope);

        assertThat(offenceDecision1.getPleaAtDecisionTime(), equalTo(offence1Details.getPlea()));
        assertThat(offenceDecision1.getPleaDate(), equalTo(offence1Details.getPleaDate()));

        assertThat(offenceDecision2.getPleaAtDecisionTime(), equalTo(offence2Details.getPlea()));
        assertThat(offenceDecision2.getPleaDate(), equalTo(offence2Details.getPleaDate()));

        assertThat(offenceDecision3.getPleaAtDecisionTime(), nullValue());
        assertThat(offenceDecision3.getPleaDate(), nullValue());

        verify(caseDecisionRepository).save(caseDecision);
    }

    @Test
    public void shouldSaveSetAsideOffenceDecision() {
        final OffenceDetail offence1Details = new OffenceDetail();
        offence1Details.setId(offence1Id);
        offence1Details.setSequenceNumber(1);
        offence1Details.setPlea(GUILTY);
        offence1Details.setPleaDate(now.minusDays(2));
        offence1Details.setConviction(FOUND_GUILTY);

        final OffenceDetail offence2Details = new OffenceDetail();
        offence2Details.setId(offence2Id);
        offence2Details.setSequenceNumber(2);
        offence2Details.setPlea(NOT_GUILTY);
        offence2Details.setPleaDate(now.minusDays(1));
        offence2Details.setConviction(FOUND_GUILTY);

        final OffenceDetail offence3Details = new OffenceDetail();
        offence3Details.setId(offence3Id);
        offence3Details.setSequenceNumber(3);
        offence3Details.setConviction(FOUND_GUILTY);

        final DefendantDetail defendantDetails = new DefendantDetail();
        defendantDetails.setOffences(asList(offence1Details, offence2Details, offence3Details));

        final CaseDetail caseDetails = new CaseDetail();
        caseDetails.setId(caseId);
        caseDetails.setDefendant(defendantDetails);

        final OffenceDecision offenceDecision1 = new SetAsideOffenceDecision(offence1Id, decisionId, null);
        final OffenceDecision offenceDecision2 = new SetAsideOffenceDecision(offence2Id, decisionId, null);
        final OffenceDecision offenceDecision3 = new SetAsideOffenceDecision(offence3Id, decisionId, null);

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(decisionId);
        caseDecision.setCaseId(caseId);
        caseDecision.setOffenceDecisions(asList(offenceDecision1, offenceDecision2, offenceDecision3));

        when(envelope.payload()).thenReturn(decisionSavedEvent);
        when(eventConverter.convert(decisionSavedEvent)).thenReturn(caseDecision);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetails);

        listener.handleCaseDecisionSaved(envelope);

        assertThat(caseDetails.getSetAside(), is(true));
        assertThat(caseDetails
                .getDefendant()
                .getOffences()
                .stream()
                .allMatch(offence -> offence.getConviction() == null), is(true));

        verify(caseDecisionRepository).save(caseDecision);
    }

    @Test
    public void shouldSavePressRestrictionAndCompleted() {
        final OffenceDetail offence1Details = new OffenceDetail();
        offence1Details.setId(offence1Id);
        offence1Details.setSequenceNumber(1);

        final OffenceDetail offence2Details = new OffenceDetail();
        offence2Details.setId(offence2Id);
        offence2Details.setSequenceNumber(2);

        final OffenceDetail offence3Details = new OffenceDetail();
        offence3Details.setId(offence3Id);
        offence3Details.setSequenceNumber(3);

        final DefendantDetail defendantDetails = new DefendantDetail();
        defendantDetails.setOffences(asList(offence1Details, offence2Details, offence3Details));

        final CaseDetail caseDetails = new CaseDetail();
        caseDetails.setId(caseId);
        caseDetails.setDefendant(defendantDetails);

        final OffenceDecision offenceDecision1 = new DischargeOffenceDecision(offence1Id, decisionId, FOUND_GUILTY, new DischargePeriod(MONTH, 2), false, new BigDecimal(20), null, ABSOLUTE, null,null);
        offenceDecision1.setPressRestriction(new PressRestriction("Person 1", true));
        final OffenceDecision offenceDecision2 = new DischargeOffenceDecision(offence2Id, decisionId, FOUND_GUILTY, new DischargePeriod(MONTH, 2), false, new BigDecimal(20), null, ABSOLUTE, null,null);
        offenceDecision2.setPressRestriction(new PressRestriction("Person 2", true));
        final OffenceDecision offenceDecision3 = new DischargeOffenceDecision(offence3Id, decisionId, FOUND_GUILTY, new DischargePeriod(MONTH, 2), false, new BigDecimal(20), null, ABSOLUTE, null,null);
        offenceDecision3.setPressRestriction(new PressRestriction("Person 3", false));

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(decisionId);
        caseDecision.setCaseId(caseId);
        caseDecision.setSavedAt(now());
        caseDecision.setOffenceDecisions(asList(offenceDecision1, offenceDecision2, offenceDecision3));

        when(envelope.payload()).thenReturn(decisionSavedEvent);
        when(eventConverter.convert(decisionSavedEvent)).thenReturn(caseDecision);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetails);

        listener.handleCaseDecisionSaved(envelope);

        final List<OffenceDetail> offences = caseDetails.getDefendant().getOffences();

        assertThat(offences
                .stream()
                .allMatch(offence -> offence.getPressRestriction() != null), is(true));

        assertThat(offences.get(0).getPressRestriction(), is(new PressRestriction("Person 1", true)));
        assertThat(offences.get(0).getCompleted(), is(true));
        assertThat(offences.get(1).getPressRestriction(), is(new PressRestriction("Person 2", true)));
        assertThat(offences.get(1).getCompleted(), is(true));
        assertThat(offences.get(2).getPressRestriction(), is(new PressRestriction("Person 3", false)));
        assertThat(offences.get(2).getCompleted(), is(true));

        verify(caseDecisionRepository).save(caseDecision);
    }

    @Test
    public void shouldSaveApplicationDecision() {
        when(applicationEnvelope.payload()).thenReturn(applicationDecisionSaved);
        final CaseApplicationDecision applicationDecision = new CaseApplicationDecision();
        when(applicationDecisionConverter.convert(applicationDecisionSaved)).thenReturn(applicationDecision);
        listener.handleApplicationDecisionSaved(applicationEnvelope);

        verify(caseApplicationService).saveCaseApplicationDecision(applicationDecision);
    }

    @Test
    public void shouldSaveDecisionThroughAocpAndAddAccountNote() {
        final OffenceDetail offence1Details = new OffenceDetail();
        offence1Details.setId(offence1Id);
        offence1Details.setSequenceNumber(1);
        offence1Details.setPleaDate(now.minusDays(2));
        offence1Details.setAocpStandardPenalty(BigDecimal.valueOf(100));
        offence1Details.setCompensation(BigDecimal.valueOf(5));

        final DefendantDetail defendantDetails = new DefendantDetail();
        defendantDetails.setOffences(asList(offence1Details));

        final CaseDetail caseDetails = new CaseDetail();
        caseDetails.setId(caseId);
        caseDetails.setDefendant(defendantDetails);

        final OffenceDecision offenceDecision1 = new FinancialPenaltyOffenceDecision();

        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(decisionId);
        caseDecision.setCaseId(caseId);
        caseDecision.setOffenceDecisions(asList(offenceDecision1));
        caseDecision.setResultedThroughAOCP(true);

        final DecisionSaved decisionSaved = new DecisionSaved(decisionId, randomUUID(), caseId, null, null, null, null, null, null, true);
        final Envelope<DecisionSaved> decisionSavedEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.events.case-note-added"), decisionSaved);

        when(eventConverter.convert(any())).thenReturn(caseDecision);
        when(caseRepository.findBy(caseId)).thenReturn(caseDetails);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantId(any(), any())).thenReturn(asList(new OnlinePleaDetail()));

        listener.handleCaseDecisionSaved(decisionSavedEnvelope);

        verify(caseDecisionRepository).save(caseDecision);
        verify(caseAccountNoteRepository).save(any());
        verify(onlinePleaDetailRepository).save(any());
    }
}
