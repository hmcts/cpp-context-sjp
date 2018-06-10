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
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SELECTOR_PLEA_UPDATED;
import static uk.gov.moj.sjp.it.helper.UpdatePleaHelper.getPleaPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import java.util.Optional;

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
                .subscribe(CaseReceived.EVENT_NAME)
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

            final PleaMethod pleaMethod = PleaMethod.POSTAL;
            final PleaType guiltyPlea = GUILTY;

            final Optional<JsonEnvelope> pleadedGuiltyPublicEvent = new EventListener()
                    .subscribe(PleaUpdated.EVENT_NAME)
                    .subscribe(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED)
                    .run(() -> updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(guiltyPlea)))
                    .popEvent(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED);

            assertThat(pleadedGuiltyPublicEvent.isPresent(), is(true));
            assertThat(pleadedGuiltyPublicEvent.get(), jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED),
                    payload(isJson(allOf(
                            withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                            withJsonPath("offenceId", equalTo(createCasePayloadBuilder.getOffenceId().toString())),
                            withJsonPath("plea", equalTo(guiltyPlea.toString()))
                    )))
            ));

            updatePleaHelper.verifyPleaUpdated(createCasePayloadBuilder.getId(), guiltyPlea, pleaMethod);

            final PleaType notGuiltyPlea = NOT_GUILTY;

            caseSearchResultHelper.verifyPleaReceivedDate();

            final Optional<JsonEnvelope> pleadedNotGuiltyPublicEvent = new EventListener()
                    .subscribe(PleaUpdated.EVENT_NAME)
                    .subscribe(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED)
                    .run(() -> updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), getPleaPayload(notGuiltyPlea)))
                    .popEvent(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED);

            assertThat(pleadedNotGuiltyPublicEvent.isPresent(), is(true));
            assertThat(pleadedNotGuiltyPublicEvent.get(), jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_SELECTOR_PLEA_UPDATED),
                    payload(isJson(allOf(
                            withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                            withJsonPath("offenceId", equalTo(createCasePayloadBuilder.getOffenceId().toString())),
                            withJsonPath("plea", equalTo(notGuiltyPlea.toString()))
                    )))
            ));

            final Optional<JsonEnvelope> pleaCancelledPublicEvent = new EventListener()
                    .subscribe(PleaCancelled.EVENT_NAME)
                    .subscribe(PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED)
                    .run(() -> cancelPleaHelper.cancelPlea())
                    .popEvent(PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED);

            assertThat(pleaCancelledPublicEvent.isPresent(), is(true));
            assertThat(pleaCancelledPublicEvent.get(), jsonEnvelope(
                    metadata().withName(PUBLIC_EVENT_SELECTOR_PLEA_CANCELLED),
                    payload(isJson(allOf(
                            withJsonPath("caseId", equalTo(createCasePayloadBuilder.getId().toString())),
                            withJsonPath("offenceId", equalTo(createCasePayloadBuilder.getOffenceId().toString()))
                    )))
            ));

            cancelPleaHelper.verifyPleaCancelled();

            caseSearchResultHelper.verifyNoPleaReceivedDate();
        }
    }

}
