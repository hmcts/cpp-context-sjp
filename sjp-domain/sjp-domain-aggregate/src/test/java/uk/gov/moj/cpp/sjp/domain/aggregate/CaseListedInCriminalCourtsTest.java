package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;


import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.CaseOffenceListedInCriminalCourts;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseListedInCriminalCourtsTest extends CaseAggregateBaseTest {

    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    final UUID decisionId = randomUUID();
    final UUID sessionId = randomUUID();
    final ZonedDateTime savedAt = ZonedDateTime.now();
    final UUID REFERRAL_REASON_ID = randomUUID();
    final UUID userId = randomUUID();
    final HearingType hearingType = new HearingType(null, null, null);


    @BeforeEach
    public void setUp() {
        super.setUp();
        caseAggregate = new CaseAggregate();
        objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCreateCaseListingInCriminalCourtsEvent() {
        caseReceivedEvent = collectFirstEvent(caseAggregate.receiveCase(buildCaseReceived(), clock.now()), CaseReceived.class);
        final UUID offenceId = caseAggregate.getState().getOffences().stream().findFirst().get();


        final User savedBy = new User("John", "Smith", userId);
        final DefendantCourtOptions courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false, NO_DISABILITY_NEEDS);

        final List<OffenceDecisionInformation> offenceDecisionInformation = new ArrayList<>();
        offenceDecisionInformation.add(OffenceDecisionInformation.createOffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT));
        final List<OffenceDecision> offenceDecisions = new ArrayList<>();
        final OffenceDecision offenceDecision =
                new ReferForCourtHearing(
                        randomUUID(),
                        offenceDecisionInformation,
                        REFERRAL_REASON_ID,
                        "Note",
                        30,
                        courtOptions, null);
        offenceDecisions.add(offenceDecision);

        final Decision decision = new Decision(decisionId, sessionId, caseId, "duplicate conviction", savedAt, savedBy, offenceDecisions, null,null);
        caseAggregate.assignCase(savedBy.getUserId(), ZonedDateTime.now(), CaseAssignmentType.MAGISTRATE_DECISION);

        caseAggregate.saveDecision(decision, session);

        final String hearingCourtName = "Court Name";
        final String roomName = "Court Room Name";
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<UUID> defendantOffences = new ArrayList<>();
        defendantOffences.add(offenceId);
        final List<HearingDay> hearingDayList = new ArrayList<>();

        final Stream<Object> eventsStream = caseAggregate.updateCaseListedInCriminalCourts(caseId, defendantId, defendantOffences, hearingId, createCourtCenter(), hearingDayList, hearingType);
        final List<Object> events = eventsStream.collect(Collectors.toList());

        assertThat(events, hasSize(2));

        assertThat(events.get(0), instanceOf(CaseOffenceListedInCriminalCourts.class));
        assertThat(events.get(1), instanceOf(CaseListedInCriminalCourtsV2.class));
        final CaseOffenceListedInCriminalCourts caseListedInCcForReferToCourt = (CaseOffenceListedInCriminalCourts) events.get(0);
        assertThat(caseListedInCcForReferToCourt.getCaseId(), equalTo(caseId));
        assertThat(caseListedInCcForReferToCourt.getCourtCentre().getName(), equalTo(hearingCourtName));
        assertThat(caseListedInCcForReferToCourt.getCourtCentre().getRoomName(), equalTo(roomName));
        assertThat(caseListedInCcForReferToCourt.getHearingType(), equalTo(hearingType));
    }

    @Test
    public void shouldNotCreateCaseListingInCriminalCourtsEventWhenTheCaseIsNotAReferredToCourt() {
        caseReceivedEvent = collectFirstEvent(caseAggregate.receiveCase(buildCaseReceived(), clock.now()), CaseReceived.class);

        final String hearingCourtName = "Court Name";
        final String roomName = "Court Room Name";
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final List<UUID> defendantOffences = new ArrayList<>();
        final List<HearingDay> hearingDayList = new ArrayList<>();

        final Stream<Object> eventsStream = caseAggregate.updateCaseListedInCriminalCourts(caseId, defendantId, defendantOffences, hearingId, createCourtCenter(), hearingDayList, hearingType);
        final List<Object> events = eventsStream.collect(Collectors.toList());

        assertThat(events, hasSize(0));
    }

    @Test
    public void shouldNotCreateCaseListingInCriminalCourtsEvent() {
        final UUID hearingId = randomUUID();
        final List<HearingDay> hearingDayList = new ArrayList<>();

        final Stream<Object> eventsStream = caseAggregate.updateCaseListedInCriminalCourts(caseId, null, null, hearingId, createCourtCenter(), hearingDayList, hearingType);
        final List<Object> events = eventsStream.collect(Collectors.toList());
        assertThat(events, hasSize(0));
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
