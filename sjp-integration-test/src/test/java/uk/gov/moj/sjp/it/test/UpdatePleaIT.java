package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.POSTAL;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_DATES_TO_AVOID_ADDED;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.helper.UpdatePleaHelper.getPleaPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import java.util.Optional;

import javax.json.JsonValue;

import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;

public class UpdatePleaIT extends BaseIntegrationTest {

    private UpdatePleaHelper updatePleaHelper;

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        this.updatePleaHelper = new UpdatePleaHelper();
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME, CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));
    }

    @Test
    public void shouldAddUpdateAndCancelPlea() {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(),
                     EVENT_SELECTOR_PLEA_CANCELLED, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED)
        ) {
            final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(createCasePayloadBuilder.getId(),
                    createCasePayloadBuilder.getUrn(),
                    createCasePayloadBuilder.getDefendantBuilder().getLastName(),
                    createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth());

            caseSearchResultHelper.verifyPersonInfoByUrn();

            final EventListener eventListener = new EventListener();

            final PleaType guiltyPlea = GUILTY;

            eventListener
                    .subscribe(PleaUpdated.EVENT_NAME, CaseMarkedReadyForDecision.EVENT_NAME, PUBLIC_EVENT_SELECTOR_PLEA_UPDATED)
                    .run(() -> updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(guiltyPlea)));

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            verifyPrivatePleaUpdatedEventEmitted(eventListener, guiltyPlea);
            verifyPublicPleaUpdatedEventEmitted(eventListener, guiltyPlea);
            verifyPrivateCaseMarkedReadyEventEmitted(eventListener, PLEADED_GUILTY);
            updatePleaHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), guiltyPlea, POSTAL);
            caseSearchResultHelper.verifyPleaReceivedDate();

            final PleaType notGuiltyPlea = NOT_GUILTY;

            eventListener.reset()
                    .subscribe(PleaUpdated.EVENT_NAME, PUBLIC_EVENT_SELECTOR_PLEA_UPDATED, CaseMarkedReadyForDecision.EVENT_NAME)
                    .run(() -> updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(notGuiltyPlea)));

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION);
            verifyPrivatePleaUpdatedEventEmitted(eventListener, notGuiltyPlea);
            verifyPublicPleaUpdatedEventEmitted(eventListener, notGuiltyPlea);

            assertThat(eventListener.popEvent(CaseMarkedReadyForDecision.EVENT_NAME).isPresent(), is(false));

            eventListener.reset()
                    .subscribe(EVENT_SELECTOR_DATES_TO_AVOID_ADDED, "public.sjp.dates-to-avoid-added", CaseMarkedReadyForDecision.EVENT_NAME)
                    .run(() -> addDatesToAvoid(createCasePayloadBuilder.getId(), "my-dates-to-avoid"));

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION);
            verifyDatesToAvoid(eventListener, "my-dates-to-avoid");

            verifyPrivateCaseMarkedReadyEventEmitted(eventListener, PLEADED_NOT_GUILTY);
            updatePleaHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), notGuiltyPlea, POSTAL);

            eventListener.reset()
                    .subscribe(PleaCancelled.EVENT_NAME, CaseMarkedReadyForDecision.EVENT_NAME, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED)
                    .run(cancelPleaHelper::cancelPlea);

            caseSearchResultHelper.verifyCaseStatus(CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION);
            verifyPrivatePleaCancelledEventEmitted(eventListener);
            verifyPublicPleaCancelledEventEmitted(eventListener);
            verifyPrivateCaseMarkedReadyEventEmitted(eventListener, PIA);

            cancelPleaHelper.verifyPleaCancelled();
            caseSearchResultHelper.verifyNoPleaReceivedDate();
        }
    }

    private void verifyPublicPleaUpdatedEventEmitted(final EventListener eventListener, final PleaType pleaType) {
        verifyEventEmitted(eventListener, PUBLIC_EVENT_SELECTOR_PLEA_UPDATED, isJson(allOf(
                withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                withJsonPath("offenceId", equalTo(createCasePayloadBuilder.getOffenceId().toString())),
                withJsonPath("plea", equalTo(pleaType.toString()))
        )));
    }

    private void verifyPublicPleaCancelledEventEmitted(final EventListener eventListener) {
        verifyEventEmitted(eventListener, PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED, isJson(allOf(
                withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                withJsonPath("offenceId", equalTo(createCasePayloadBuilder.getOffenceId().toString()))
        )));
    }

    private void verifyPrivatePleaCancelledEventEmitted(final EventListener eventListener) {
        verifyEventEmitted(eventListener, PleaCancelled.EVENT_NAME, isJson(allOf(
                withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                withJsonPath("offenceId", equalTo(createCasePayloadBuilder.getOffenceId().toString()))
        )));
    }

    private void verifyPrivatePleaUpdatedEventEmitted(final EventListener eventListener, final PleaType pleaType) {
        verifyEventEmitted(eventListener, PleaUpdated.EVENT_NAME, isJson(allOf(
                withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                withJsonPath("offenceId", equalTo(createCasePayloadBuilder.getOffenceId().toString())),
                withJsonPath("plea", equalTo(pleaType.toString()))
        )));
    }

    private void verifyPrivateCaseMarkedReadyEventEmitted(final EventListener eventListener, final CaseReadinessReason readinessReason) {
        verifyEventEmitted(eventListener, CaseMarkedReadyForDecision.EVENT_NAME, isJson(allOf(
                withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                withJsonPath("reason", equalTo(readinessReason.toString()))
        )));
    }

    private void verifyDatesToAvoid(final EventListener eventListener, final String expectedDatesToAvoid) {
        verifyEventEmitted(eventListener, EVENT_SELECTOR_DATES_TO_AVOID_ADDED, isJson(allOf(
                withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                withJsonPath("datesToAvoid", equalTo(expectedDatesToAvoid))
        )));
    }

    private void verifyEventEmitted(final EventListener eventListener, final String eventName, final TypeSafeDiagnosingMatcher<JsonValue> paylaodMatcher) {
        final Optional<JsonEnvelope> event = eventListener.popEvent(eventName);

        assertThat(event.isPresent(), is(true));

        assertThat(event.get(), jsonEnvelope(
                metadata().withName(eventName),
                payload(paylaodMatcher)
        ));
    }

}
