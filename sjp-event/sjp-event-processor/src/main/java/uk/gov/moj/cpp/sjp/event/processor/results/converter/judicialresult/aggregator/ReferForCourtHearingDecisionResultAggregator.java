package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_DURATION_IN_MINUTES;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_ADDR_LINE1;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_ADDR_LINE2;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_ADDR_LINE3;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_ADDR_LINE4;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_ADDR_LINE5;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_ADDR_POSTCODE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_HEARING_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_HEARING_TIME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_ORG_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.NHMC_HOUSE_ROOM_NAME;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.SUMRCC;

import java.util.Comparator;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.CaseOffenceListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.ConvictionInfo;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.CourtCentreConverter;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCachedReferenceData;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("squid:S1188")
public class ReferForCourtHearingDecisionResultAggregator extends DecisionResultAggregator {

    @Inject
    private CourtCentreConverter courtCentreConverter;

    @Inject
    public ReferForCourtHearingDecisionResultAggregator(final JCachedReferenceData cachedReferenceData) {
        super(cachedReferenceData);
    }

    public void aggregate(final OffenceDecision offenceDecision,
                          final JsonEnvelope sjpSessionEnvelope,
                          final DecisionAggregate decisionAggregate,
                          final CaseListedInCriminalCourtsV2 caseListedInCcForReferToCourt,
                          final ZonedDateTime resultedOn) {
        final ReferForCourtHearing referForCourtHearing = (ReferForCourtHearing) offenceDecision;

        // refer for court hearing
        final Map<UUID, List<JudicialResult>> judicialResultsMap = referForCourtHearing(referForCourtHearing, sjpSessionEnvelope, caseListedInCcForReferToCourt, resultedOn);

        // press restriction
        final List<JudicialResult> pressRestrictionResults = (pressRestriction(referForCourtHearing.getPressRestriction(), sjpSessionEnvelope, resultedOn));

        referForCourtHearing
                .getOffenceDecisionInformation()
                .forEach(o -> {
                    final List<JudicialResult> judicialResults = new ArrayList<>();
                    judicialResults.addAll(pressRestrictionResults);
                    judicialResults.addAll(judicialResultsMap.get(o.getOffenceId()));

                    decisionAggregate.putResults(o.getOffenceId(), judicialResults);
                    setFinalOffence(decisionAggregate, o.getOffenceId(), judicialResults);
                });

        // conviction information
        referForCourtHearing
                .getOffenceDecisionInformation()
                .forEach(oi -> {
                    final Optional<CourtCentre> convictingCourtOptional = courtCentreConverter.convertByOffenceId(oi.getOffenceId(), sjpSessionEnvelope.metadata());


                    decisionAggregate.putConvictionInfo(oi.getOffenceId(),
                            new ConvictionInfo(oi.getOffenceId(),
                                    oi.getVerdict(),
                                    referForCourtHearing.getConvictionDate(),
                                    convictingCourtOptional.orElse(null))
                    );
                });
    }

    private Map<UUID, List<JudicialResult>> referForCourtHearing(final ReferForCourtHearing referForCourtHearing,
                                                                 final JsonEnvelope sjpSessionEnvelope,
                                                                 final CaseListedInCriminalCourtsV2 caseListedInCcForReferToCourt,
                                                                 final ZonedDateTime resultedOn) {
        final UUID resultId = SUMRCC.getResultDefinitionId();
        final JsonObject resultDefinition = getResultDefinition(sjpSessionEnvelope, resultId);
        final Map<UUID, List<JudicialResult>> judicialResultsMap = new HashMap<>();

        referForCourtHearing.offenceDecisionInformationAsList().forEach(e -> {
            final UUID offenceId = e.getOffenceId();
            final List<JudicialResult> judicialResults = new ArrayList<>();
            final List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
            populateReferralReason(referForCourtHearing, sjpSessionEnvelope, resultDefinition, judicialResultPrompts);
            final CaseOffenceListedInCriminalCourts caseOffenceListedInCriminalCourts = caseListedInCcForReferToCourt
                    .getOffenceHearings()
                    .stream()
                    .filter(a -> a.getDefendantOffences().contains(offenceId))
                    .findFirst()
                    .get();
            populateCourtDetails(resultDefinition, judicialResultPrompts, caseOffenceListedInCriminalCourts);
            populateCourtTimingDetails(resultDefinition, judicialResultPrompts, caseOffenceListedInCriminalCourts);
            populateListingCourtHouseDetails(judicialResultPrompts, resultDefinition, caseOffenceListedInCriminalCourts.getCourtCentre().getAddress());
            judicialResults.add(
                    populateResultDefinitionAttributes(resultId, sjpSessionEnvelope)
                            .withOrderedDate(resultedOn.format(DATE_FORMAT))
                            .withResultText(getResultText(judicialResultPrompts, resultDefinition.getString(LABEL)))
                            .withJudicialResultPrompts(getCheckJudicialPromptsEmpty(judicialResultPrompts))
                            .withNextHearing(getNextHearing(caseOffenceListedInCriminalCourts))
                            .build());
            judicialResultsMap.put(offenceId, judicialResults);
        });

        return judicialResultsMap;
    }

    private NextHearing getNextHearing(final CaseOffenceListedInCriminalCourts caseOffenceListedInCriminalCourts) {
        final HearingDay hearingDay = caseOffenceListedInCriminalCourts.getHearingDays().stream().min(Comparator.comparing(HearingDay::getSittingDay))
                .orElseGet(() -> HearingDay.hearingDay().build());

        return NextHearing.nextHearing()
                .withCourtCentre(caseOffenceListedInCriminalCourts.getCourtCentre())
                .withListedStartDateTime(hearingDay.getSittingDay())
                .withType(caseOffenceListedInCriminalCourts.getHearingType())
                .withEstimatedMinutes(hearingDay.getListedDurationMinutes())
                .build();
    }

    private void populateCourtTimingDetails(final JsonObject resultDefinition,
                                            final List<JudicialResultPrompt> judicialResultPrompts,
                                            final CaseOffenceListedInCriminalCourts caseOffenceListedInCriminalCourts) {

        judicialResultPrompts.add(getPrompt(NHMC_DURATION_IN_MINUTES, resultDefinition)
                .withJudicialResultPromptTypeId(NHMC_DURATION_IN_MINUTES.getId())
                .withValue(caseOffenceListedInCriminalCourts.getHearingDays().get(0).getListedDurationMinutes().toString()).build());

        final ZonedDateTime sittingDateTime = caseOffenceListedInCriminalCourts.getHearingDays().get(0).getSittingDay();

        judicialResultPrompts.add(getPrompt(NHMC_HOUSE_HEARING_TIME, resultDefinition)
                .withJudicialResultPromptTypeId(NHMC_HOUSE_HEARING_TIME.getId())
                .withValue(sittingDateTime.toLocalTime().toString()).build());

        judicialResultPrompts.add(getPrompt(NHMC_HOUSE_HEARING_DATE, resultDefinition)
                .withJudicialResultPromptTypeId(NHMC_HOUSE_HEARING_DATE.getId())
                .withValue(sittingDateTime.toLocalDate().toString()).build());
    }

    private void populateCourtDetails(final JsonObject resultDefinition,
                                      final List<JudicialResultPrompt> judicialResultPrompts,
                                      final CaseOffenceListedInCriminalCourts caseOffenceListedInCriminalCourts) {

        judicialResultPrompts.add(getPrompt(NHMC_HOUSE_ORG_NAME, resultDefinition)
                .withJudicialResultPromptTypeId(NHMC_HOUSE_ORG_NAME.getId())
                .withValue(caseOffenceListedInCriminalCourts.getCourtCentre().getName()).build());

        ofNullable(caseOffenceListedInCriminalCourts.getCourtCentre().getRoomName())
                .ifPresent(roomName -> judicialResultPrompts.add(getPrompt(NHMC_HOUSE_ROOM_NAME, resultDefinition)
                        .withJudicialResultPromptTypeId(NHMC_HOUSE_ROOM_NAME.getId())
                        .withValue(roomName).build()));
    }

    private void populateReferralReason(final ReferForCourtHearing referForCourtHearing,
                                        final JsonEnvelope sjpSessionEnvelope,
                                        final JsonObject resultDefinition,
                                        final List<JudicialResultPrompt> judicialResultPrompts) {
        final JudicialResultPrompt referralReason = getPrompt(SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT, resultDefinition)
                .withJudicialResultPromptTypeId(SUMRCC_REASONS_IDS_FOR_REFERRING_TO_COURT.getId())
                .withValue(jCachedReferenceData.getReferralReason(referForCourtHearing.getReferralReasonId(), envelopeFrom(sjpSessionEnvelope.metadata(), null)))
                .build();
        judicialResultPrompts.add(referralReason);
    }

    private void populateListingCourtHouseDetails(final List<JudicialResultPrompt> judicialResultPrompts,
                                                  final JsonObject resultDefinition,
                                                  final Address address) {
        ofNullable(address)
                .ifPresent(addr -> {
                            judicialResultPrompts.add(getPrompt(NHMC_HOUSE_ADDR_LINE1, resultDefinition).
                                    withJudicialResultPromptTypeId(NHMC_HOUSE_ADDR_LINE1.getId())
                                    .withValue(addr.getAddress1()).build());
                            ofNullable(addr.getAddress2()).filter(adr -> !adr.isEmpty()).ifPresent(addrLine -> judicialResultPrompts
                                    .add(getPrompt(NHMC_HOUSE_ADDR_LINE2, resultDefinition)
                                            .withJudicialResultPromptTypeId(NHMC_HOUSE_ADDR_LINE2.getId()).withValue(addrLine).build()));
                            ofNullable(addr.getAddress3()).filter(adr -> !adr.isEmpty()).ifPresent(addrLine -> judicialResultPrompts
                                    .add(getPrompt(NHMC_HOUSE_ADDR_LINE3, resultDefinition)
                                            .withJudicialResultPromptTypeId(NHMC_HOUSE_ADDR_LINE3.getId()).withValue(addrLine).build()));
                            ofNullable(addr.getAddress4()).filter(adr -> !adr.isEmpty()).ifPresent(addrLine -> judicialResultPrompts
                                    .add(getPrompt(NHMC_HOUSE_ADDR_LINE4, resultDefinition)
                                            .withJudicialResultPromptTypeId(NHMC_HOUSE_ADDR_LINE4.getId()).withValue(addrLine).build()));
                            ofNullable(addr.getAddress5()).filter(adr -> !adr.isEmpty()).ifPresent(addrLine -> judicialResultPrompts
                                    .add(getPrompt(NHMC_HOUSE_ADDR_LINE5, resultDefinition)
                                            .withJudicialResultPromptTypeId(NHMC_HOUSE_ADDR_LINE5.getId()).withValue(addrLine).build()));
                            ofNullable(addr.getPostcode()).filter(adr -> !adr.isEmpty()).ifPresent(addrLine -> judicialResultPrompts
                                    .add(getPrompt(NHMC_HOUSE_ADDR_POSTCODE, resultDefinition)
                                            .withJudicialResultPromptTypeId(NHMC_HOUSE_ADDR_POSTCODE.getId()).withValue(addrLine).build()));
                        }
                );
    }
}
