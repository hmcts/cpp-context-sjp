package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.CaseOffenceListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.CourtCentreConverter;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferForCourtHearingDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private ReferForCourtHearingDecisionResultAggregator aggregator;

    private final UUID REFERRAL_REASON_ID = randomUUID();

    @Mock
    private CourtCentreConverter courtCentreConverter;

    private CourtCentre courtCentre = courtCentre().build();
    private HearingType hearingType = new HearingType(null, null, null);

    private final DefendantCourtOptions courtOptions =
            new DefendantCourtOptions(
                    new DefendantCourtInterpreter("EN", true),
                    false, NO_DISABILITY_NEEDS);

    @BeforeEach
    public void setUp() {
        super.setUp();
        aggregator = new ReferForCourtHearingDecisionResultAggregator(jCachedReferenceData);

        setField(aggregator, "courtCentreConverter", courtCentreConverter);

        when(courtCentreConverter.convertByOffenceId(any(), any())).thenReturn(Optional.of(courtCentre));

        when(referenceDataService.getReferralReasons(any(JsonEnvelope.class)))
                .thenReturn(createObjectBuilder().
                                add("referralReasons",
                                        createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("id", REFERRAL_REASON_ID.toString())
                                                        .add("reason", "referral reason")
                                                        .add("subReason", "referral sub reason")
                                                        .build())).build());
    }

    @Test
    public void shouldPopulateResultWithRightPrompts() {

        final ReferForCourtHearing offenceDecision
                = new ReferForCourtHearing(null,
                Collections.singletonList(createOffenceDecisionInformation(offence1Id, PROVED_SJP)),
                REFERRAL_REASON_ID,
                "Note",
                30,
                courtOptions, null);
        final ZonedDateTime zonedDateTime = ZonedDateTime.now().plusDays(1);
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().
                withSittingDay(zonedDateTime)
                .withListedDurationMinutes(123456)
                .build());
        final CourtCentre courtCentre = createCourtCenter();
        final CaseOffenceListedInCriminalCourts caseOffenceListedInCriminalCourts =
                new CaseOffenceListedInCriminalCourts(caseId, defendantId, Arrays.asList(offence1Id), hearingId, courtCentre, hearingDays, hearingType);

        final CaseListedInCriminalCourtsV2 caseListedInCcForReferToCourt =
                new CaseListedInCriminalCourtsV2(Arrays.asList(caseOffenceListedInCriminalCourts), null, caseId);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, caseListedInCcForReferToCourt, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(1));
        JudicialResult judicialResult =  resultsAggregate.getResults(offence1Id).get(0);
        assertThat(judicialResult.getJudicialResultId().toString(), is("600edfc3-a584-4f9f-a52e-5bb8a99646c1"));
        assertThat(judicialResult.getOrderedDate(), is(resultedOn.format(DATE_FORMAT)));

        assertThat(judicialResult.getResultText(), is("Refer for a full court hearing\n" +"Reasons for referring to court referral reason (referral sub reason)\nCourthouse organisation name Court Name\nCourtroom Court Room Name\nEstimated duration 123456\nTime of hearing "+zonedDateTime.toLocalTime()+"\nDate of hearing "+zonedDateTime.toLocalDate()+"\nCourthouse address line 1 Address 1\nCourthouse address line 2 Address 2\nCourthouse address line 3 Address 3\nCourthouse address line 4 Address 4\nCourthouse address line 5 Address 5\nCourthouse post code DD4 4DD"));


        assertThat(judicialResult.getJudicialResultPrompts().size(), is(12));
        assertThat(judicialResult.getJudicialResultPrompts(),
                hasItem(anyOf(
                        Matchers.hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("bca4e07c-17e0-48f1-84f4-7b6ff8bab5e2"))),
                        Matchers.hasProperty("value", Matchers.is("referral reason (referral sub reason)")),
                        Matchers.hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("66868c04-72c4-46d9-a4fc-860a82107475"))),
                        Matchers.hasProperty("promptReference", Matchers.is("hCHOUSEOrganisationName")),
                        Matchers.hasProperty("value", Matchers.is("Court Name")))
        ));

        assertThat(judicialResult.getNextHearing(), notNullValue());
        assertThat(judicialResult.getNextHearing().getCourtCentre(), is(courtCentre));
        assertThat(judicialResult.getNextHearing().getListedStartDateTime(), is(hearingDays.get(0).getSittingDay()));
        assertThat(judicialResult.getNextHearing().getType(), is(hearingType));
        assertThat(judicialResult.getNextHearing().getEstimatedMinutes(), is(hearingDays.get(0).getListedDurationMinutes()));
        assertThat(judicialResult.getNextHearing().getAdjournmentReason(), is("referral reason (referral sub reason)"));
    }

    @Test
    public void shouldNotPopulateResultWithEmptyValuePrompts() {

        final ReferForCourtHearing offenceDecision
                = new ReferForCourtHearing(null,
                Collections.singletonList(createOffenceDecisionInformation(offence1Id, PROVED_SJP)),
                REFERRAL_REASON_ID,
                "Note",
                30,
                courtOptions, null);
        final ZonedDateTime zonedDateTime = ZonedDateTime.now().plusDays(1);
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final List<HearingDay> hearingDays = Arrays.asList(HearingDay.hearingDay().
                withSittingDay(zonedDateTime)
                .withListedDurationMinutes(123456)
                .build());
        final CourtCentre courtCentre = createCourtCenterWithEmptyAddresses();

        final CaseOffenceListedInCriminalCourts caseOffenceListedInCriminalCourts =
                new CaseOffenceListedInCriminalCourts(caseId, defendantId, Arrays.asList(offence1Id), hearingId, courtCentre, hearingDays, hearingType);

        final CaseListedInCriminalCourtsV2 caseListedInCcForReferToCourt =
                new CaseListedInCriminalCourtsV2(Arrays.asList(caseOffenceListedInCriminalCourts), null, caseId);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, caseListedInCcForReferToCourt, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(1));
        final JudicialResult judicialResult =  resultsAggregate.getResults(offence1Id).get(0);
        assertThat(judicialResult.getJudicialResultId().toString(), is("600edfc3-a584-4f9f-a52e-5bb8a99646c1"));
        assertThat(judicialResult.getOrderedDate(), is(resultedOn.format(DATE_FORMAT)));

        assertThat(judicialResult.getResultText(), is("Refer for a full court hearing\n" +"Reasons for referring to court referral reason (referral sub reason)\nCourthouse organisation name Court Name\nCourtroom Court Room Name\nEstimated duration 123456\nTime of hearing "+zonedDateTime.toLocalTime()+"\nDate of hearing "+zonedDateTime.toLocalDate()+"\nCourthouse address line 1 Address 1"));
        assertThat(resultsAggregate.getFinalOffence(offence1Id),is(nullValue()));
        assertThat(judicialResult.getJudicialResultPrompts().size(), is(7));
        assertThat(judicialResult.getJudicialResultPrompts(),
                hasItem(anyOf(
                        Matchers.hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("bca4e07c-17e0-48f1-84f4-7b6ff8bab5e2"))),
                        Matchers.hasProperty("value", Matchers.is("referral reason (referral sub reason)")),
                        Matchers.hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("66868c04-72c4-46d9-a4fc-860a82107475"))),
                        Matchers.hasProperty("promptReference", Matchers.is("hCHOUSEOrganisationName")),
                        Matchers.hasProperty("value", Matchers.is("Court Name"))
        )));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)),Matchers.is(nullValue()));

        assertThat(judicialResult.getNextHearing(), notNullValue());
        assertThat(judicialResult.getNextHearing().getCourtCentre(), is(courtCentre));
        assertThat(judicialResult.getNextHearing().getListedStartDateTime(), is(hearingDays.get(0).getSittingDay()));
        assertThat(judicialResult.getNextHearing().getType(), is(hearingType));
        assertThat(judicialResult.getNextHearing().getEstimatedMinutes(), is(hearingDays.get(0).getListedDurationMinutes()));
        assertThat(judicialResult.getNextHearing().getAdjournmentReason(), is("referral reason (referral sub reason)"));

    }

    private CourtCentre createCourtCenterWithEmptyAddresses() {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName("Court Name")
                .withRoomId(randomUUID())
                .withRoomName("Court Room Name")
                .withWelshName("Welsh Name")
                .withWelshRoomName("Welsh Room Name")
                .withAddress(Address.address()
                        .withAddress1("Address 1")
                        .withAddress2("")
                        .withAddress3("")
                        .withAddress4("")
                        .withAddress5("")
                        .build())
                .build();
    }
    private CourtCentre createCourtCenter() {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName("Court Name")
                .withRoomId(randomUUID())
                .withRoomName("Court Room Name")
                .withWelshName("Welsh Name")
                .withWelshRoomName("Welsh Room Name")
                .withAddress(Address.address()
                        .withAddress1("Address 1")
                        .withAddress2("Address 2")
                        .withAddress3("Address 3")
                        .withAddress4("Address 4")
                        .withAddress5("Address 5")
                        .withPostcode("DD4 4DD")
                        .build())
                .build();
    }

}