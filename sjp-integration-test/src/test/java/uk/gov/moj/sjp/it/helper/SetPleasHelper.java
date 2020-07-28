package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.POSTAL;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SET_PLEAS;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.model.PleaInfo;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import com.jayway.restassured.path.json.JsonPath;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SetPleasHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetPleasHelper.class);

    private static final String WRITE_URL_PATTERN = "/cases/%s/set-pleas";

    public static void setPleas(final UUID caseId, final JsonObject payload) {
        Objects.requireNonNull(caseId);

        LOGGER.info("Request payload: {}", new JsonPath(payload.toString()).prettify());

        makePostCall(format(WRITE_URL_PATTERN, caseId),
                "application/vnd.sjp.set-pleas+json",
                payload.toString(),
                Response.Status.ACCEPTED);
    }

    public static SetPleasPayloadBuilder setPleasPayloadBuilder() {
        return new SetPleasPayloadBuilder();
    }

    public static CreateCase.CreateCasePayloadBuilder createCase(
            final UUID caseId,
            final CreateCase.DefendantBuilder defendantBuilder,
            final UUID offence1Id,
            final UUID offence2Id,
            final UUID offence3Id,
            final LocalDate postingDate) {
        CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withDefendantBuilder(defendantBuilder)
                .withOffenceBuilders(CreateCase.OffenceBuilder.withDefaults().withId(offence1Id),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence2Id),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence3Id))
                .withPostingDate(postingDate);

        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
        return createCasePayloadBuilder;
    }

    public static void verifyEventEmittedForSetPleas(final EventListener eventListener,
                                                     final UUID caseId,
                                                     final UUID defendantId,
                                                     final Boolean welshHearing,
                                                     final String language,
                                                     final Boolean interpreter,
                                                     final String disabilityNeeds,
                                                     final Map<UUID, PleaType> pleaTypeByOffence,
                                                     final String... eventsToBeEmitted) {

        for (final String expectedEventName : eventsToBeEmitted) {
            final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(expectedEventName);
            assertThat(jsonEnvelope.isPresent(), is(true));

            final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
            matchers.add(withJsonPath("caseId", is(caseId.toString())));
            switch (expectedEventName) {
                case PleasSet.EVENT_NAME:
                case PUBLIC_EVENT_SET_PLEAS:
                    if (welshHearing != null) {
                        matchers.add(withJsonPath("defendantCourtOptions.welshHearing", is(welshHearing)));
                    }
                    if (interpreter != null) {
                        if (interpreter) {
                            matchers.add(withJsonPath("defendantCourtOptions.interpreter.language", is(language)));
                        }
                        matchers.add(withJsonPath("defendantCourtOptions.interpreter.needed", is(interpreter)));
                    }

                    if(disabilityNeeds !=null) {
                        matchers.add(withJsonPath("disabilityNeeds.needed", is(true)));
                        matchers.add(withJsonPath("disabilityNeeds.disabilityNeeds", is(disabilityNeeds)));
                    } else {
                        matchers.add(withJsonPath("disabilityNeeds.needed", is(false)));
                    }
                    matchers.add(allOf(
                            pleaTypeByOffence.entrySet().stream()
                                    .map(entry -> withJsonPath("$.pleas.*",
                                            hasItem(
                                                    JsonPathMatchers.isJson(
                                                            allOf(
                                                                    withJsonPath("offenceId", is(entry.getKey().toString())),
                                                                    withJsonPath("pleaType", entry.getValue() != null ? is(entry.getValue().toString()) : nullValue()),
                                                                    withJsonPath("defendantId", is(defendantId.toString()))
                                                            )
                                                    )
                                            )
                                            )
                                    )
                                    .collect(toList())
                            )
                    );
                    break;
                case HearingLanguagePreferenceUpdatedForDefendant.EVENT_NAME:
                    matchers.add(withJsonPath("speakWelsh", is(welshHearing)));
                    matchers.add(withJsonPath("defendantId", is(defendantId.toString())));
                    break;
                case HearingLanguagePreferenceCancelledForDefendant.EVENT_NAME:
                    matchers.add(withJsonPath("defendantId", is(defendantId.toString())));
                    break;
                case InterpreterUpdatedForDefendant.EVENT_NAME:
                    matchers.add(withJsonPath("defendantId", is(defendantId.toString())));
                    matchers.add(withJsonPath("interpreter.language", is(language)));
                    break;
                case InterpreterCancelledForDefendant.EVENT_NAME:
                    matchers.add(withJsonPath("defendantId", is(defendantId.toString())));
                    break;
                case PleaCancelled.EVENT_NAME:
                    matchers.add(withJsonPath("defendantId", is(defendantId.toString())));
                    break;
            }

            assertThat(jsonEnvelope.get(), jsonEnvelope(
                    metadata().withName(expectedEventName),
                    payload(isJson(allOf(matchers)))
            ));
        }
    }

    public static void verifySetPleasEventEmitted(final EventListener eventListener,
                                                  final UUID caseId,
                                                  final UUID defendantId,
                                                  final Map<UUID, PleaType> pleaTypeByOffence) {
        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(PleasSet.EVENT_NAME);
        matchers.add(withJsonPath("caseId", is(caseId.toString())));
        matchers.add(allOf(
                pleaTypeByOffence.entrySet().stream()
                        .map(entry -> withJsonPath("$.pleas.*",
                                hasItem(
                                        JsonPathMatchers.isJson(
                                                allOf(
                                                        withJsonPath("offenceId", is(entry.getKey().toString())),
                                                        withJsonPath("pleaType", entry.getValue() != null ? is(entry.getValue().toString()) : nullValue()),
                                                        withJsonPath("defendantId", is(defendantId.toString()))
                                                )
                                        )
                                )
                                )
                        )
                        .collect(toList())
                )
        );

        assertThat(jsonEnvelope.get(), jsonEnvelope(
                metadata().withName(PleasSet.EVENT_NAME),
                payload(isJson(allOf(matchers)))
        ));
    }

    public static void verifyPleadedNotGuiltyEventEmitted(EventListener eventListener, UUID caseId, UUID defendantId, UUID offenceId) {
        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.add(withJsonPath("caseId", is(caseId.toString())));
        matchers.add(withJsonPath("defendantId", is(defendantId.toString())));
        matchers.add(withJsonPath("offenceId", is(offenceId.toString())));
        final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(PleadedNotGuilty.EVENT_NAME);

        assertThat(jsonEnvelope.get(), jsonEnvelope(
                metadata().withName(PleadedNotGuilty.EVENT_NAME),
                payload(isJson(allOf(matchers)))
        ));
    }

    public static void verifyPleaCancelledEventEmitted(EventListener eventListener, UUID caseId, UUID defendantId, UUID offenceId) {
        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.add(withJsonPath("caseId", is(caseId.toString())));
        matchers.add(withJsonPath("defendantId", is(defendantId.toString())));
        matchers.add(withJsonPath("offenceId", is(offenceId.toString())));
        final Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent(PleaCancelled.EVENT_NAME);

        assertThat(jsonEnvelope.get(), jsonEnvelope(
                metadata().withName(PleaCancelled.EVENT_NAME),
                payload(isJson(allOf(matchers)))
        ));
    }

    public static JsonPath verifyCaseDefendantUpdated(final UUID caseId,
                                                      final Map<UUID, PleaType> pleaTypeByOffence,
                                                      final String disabilityNeeds) {
        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.add(withJsonPath("onlinePleaReceived", is(false)));
        matchers.add(allOf(
                pleaTypeByOffence.entrySet().stream()
                        .filter(entry -> Objects.nonNull(entry.getValue()))
                        .map(entry -> withJsonPath("$.defendant.offences.*",
                                hasItem(JsonPathMatchers.isJson(
                                        allOf(
                                                withJsonPath("plea", is(entry.getValue().toString())),
                                                withJsonPath("pleaMethod", is(POSTAL.toString())),
                                                withJsonPath("pleaDate", notNullValue()))
                                        )
                                )))
                        .collect(toList())));

        if(disabilityNeeds!=null){
            matchers.add(withJsonPath("$.defendant.disabilityNeeds.needed", is(true)));
            matchers.add(withJsonPath("$.defendant.disabilityNeeds.disabilityNeeds", is(disabilityNeeds)));
        } else {
            matchers.add(withJsonPath("$.defendant.disabilityNeeds.needed", is(false)));
        }
        return CasePoller.pollUntilCaseByIdIsOk(caseId, allOf(matchers));
    }

    public static JsonPath verifyCaseStatus(final UUID caseId, CaseStatus caseStatus) {
        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();
        matchers.add(allOf(withJsonPath("$.status", is(caseStatus.toString()))));
        return CasePoller.pollUntilCaseByIdIsOk(caseId, allOf(matchers));
    }

    public static class SetPleasPayloadBuilder {

        private JsonObjectBuilder rootPayloadBuilder;

        private JsonObjectBuilder defendantCourtOptionsBuilder;

        private JsonArrayBuilder pleasArrayBuilder;

        private SetPleasPayloadBuilder() {
            rootPayloadBuilder = createObjectBuilder();
        }

        public SetPleasPayloadBuilder withPlea(final UUID offenceId, final UUID defendantId, final PleaType pleaType) {
            if (pleasArrayBuilder == null) {
                pleasArrayBuilder = createArrayBuilder();
            }
            pleasArrayBuilder.add(PleaPayloadBuilder.plea(offenceId, defendantId, pleaType).build());
            return this;
        }

        public SetPleasPayloadBuilder withInterpreter(final String language, final Boolean needed) {
            if (defendantCourtOptionsBuilder == null) {
                defendantCourtOptionsBuilder = createObjectBuilder();
                rootPayloadBuilder.add("defendantCourtOptions", defendantCourtOptionsBuilder);
            }

            JsonObjectBuilder interpreter = createObjectBuilder();
            if (language != null) {
                interpreter.add("language", language);
            }

            if (needed != null) {
                interpreter.add("needed", needed);
            }

            defendantCourtOptionsBuilder.add("interpreter", interpreter);

            return this;
        }

        public SetPleasPayloadBuilder welshHearing(final Boolean welshHearing) {
            if (defendantCourtOptionsBuilder == null) {
                defendantCourtOptionsBuilder = createObjectBuilder();
                rootPayloadBuilder.add("defendantCourtOptions", defendantCourtOptionsBuilder);
            }

            if (welshHearing != null) {
                defendantCourtOptionsBuilder.add("welshHearing", welshHearing);
            }

            return this;
        }

        public SetPleasPayloadBuilder disabilityNeeds(final String disabilityNeeds) {
            rootPayloadBuilder.add("disabilityNeeds", disabilityNeeds);
            return this;
        }

        public JsonObject build() {
            if (defendantCourtOptionsBuilder != null) {
                rootPayloadBuilder.add("defendantCourtOptions", defendantCourtOptionsBuilder.build());
            }
            if (pleasArrayBuilder != null) {
                rootPayloadBuilder.add("pleas", pleasArrayBuilder.build());
            }
            return rootPayloadBuilder.build();
        }
    }

    public static class PleaPayloadBuilder {

        private JsonObjectBuilder jsonPayloadBuilder;

        private PleaPayloadBuilder() {
            this.jsonPayloadBuilder = createObjectBuilder();
        }

        PleaPayloadBuilder withOffenceId(final UUID offenceId) {
            if (offenceId != null) {
                jsonPayloadBuilder.add("offenceId", offenceId.toString());
            }
            return this;
        }

        PleaPayloadBuilder withDefendantId(final UUID defendantId) {
            if (defendantId != null) {
                jsonPayloadBuilder.add("defendantId", defendantId.toString());
            }
            return this;
        }

        PleaPayloadBuilder withPleaType(final PleaType pleaType) {
            if (pleaType != null) {
                jsonPayloadBuilder.add("pleaType", pleaType.toString());
            } else {
                jsonPayloadBuilder.addNull("pleaType");
            }
            return this;
        }

        public JsonObject build() {
            return jsonPayloadBuilder.build();
        }

        public static PleaPayloadBuilder plea(final UUID offenceId, final UUID defendantId, final PleaType pleaType) {
            return new PleaPayloadBuilder()
                    .withDefendantId(defendantId)
                    .withOffenceId(offenceId)
                    .withPleaType(pleaType);
        }
    }

    public static void requestSetPleas(
            final UUID caseId,
            final EventListener eventListener,
            final boolean welshHearingEnabled,
            final boolean welshHearing,
            final boolean interpreterEnabled,
            final String language,
            final boolean needed,
            final String disabilityNeeds,
            final List<Triple<UUID, UUID, PleaType>> pleaInfoList,
            final String... eventNames
    ) {
        final SetPleasHelper.SetPleasPayloadBuilder setPleasPayloadBuilder = SetPleasHelper.setPleasPayloadBuilder();
        if (welshHearingEnabled) {
            setPleasPayloadBuilder.welshHearing(welshHearing);
        }
        if (interpreterEnabled) {
            setPleasPayloadBuilder.withInterpreter(language, needed);
        }

        if(disabilityNeeds!=null){
            setPleasPayloadBuilder.disabilityNeeds(disabilityNeeds);
        }

        pleaInfoList.forEach(pleaInfo -> setPleasPayloadBuilder
                .withPlea(pleaInfo.getLeft(),
                        pleaInfo.getMiddle(),
                        pleaInfo.getRight()));

        eventListener
                .subscribe(eventNames)
                .run(() -> SetPleasHelper.setPleas(caseId, setPleasPayloadBuilder.build()));
    }


    public static void setPleas(final UUID caseId, final UUID defendantId, final PleaInfo... pleas) {
        final SetPleasHelper.SetPleasPayloadBuilder setPleasPayloadBuilder = setPleasPayloadBuilder()
                .welshHearing(false)
                .withInterpreter(null, false);

        stream(pleas).forEach(plea -> setPleasPayloadBuilder.withPlea(plea.offenceId, defendantId, plea.pleaType));

        new EventListener()
                .subscribe(PleasSet.EVENT_NAME)
                .run(() -> setPleas(caseId, setPleasPayloadBuilder.build()));
    }
}
