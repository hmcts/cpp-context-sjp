package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.model.PleaInfo;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;
import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SetPleasHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetPleasHelper.class);

    private static final String WRITE_URL_PATTERN = "/cases/%s/set-pleas";
    private static final String SET_PLEAS_MEDIA_TYPE = "application/vnd.sjp.set-pleas+json";

    public static void setPleas(final UUID caseId, final JsonObject payload) {
        setPleasAsUser(caseId, payload, DEFAULT_USER_ID);
    }

    public static void setPleasAsUser(final UUID caseId, final JsonObject setPleasPayload, final UUID userId) {
        Objects.requireNonNull(caseId);
        LOGGER.info("Request payload: {}", new JsonPath(setPleasPayload.toString()).prettify());
        makePostCall(userId, format(WRITE_URL_PATTERN, caseId),
                SET_PLEAS_MEDIA_TYPE,
                setPleasPayload.toString(),
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
        CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = withDefaults()
                .withId(caseId)
                .withDefendantBuilder(defendantBuilder)
                .withOffenceBuilders(CreateCase.OffenceBuilder.withDefaults().withId(offence1Id),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence2Id),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence3Id))
                .withPostingDate(postingDate);

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
        return createCasePayloadBuilder;
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

    public static void requestSetPleasAndConfirm(
            final UUID caseId,
            final boolean welshHearingEnabled,
            final boolean welshHearing,
            final boolean interpreterEnabled,
            final String language,
            final boolean needed,
            final String disabilityNeeds,
            final List<Triple<UUID, UUID, PleaType>> pleaInfoList
    ) {
        final SetPleasHelper.SetPleasPayloadBuilder setPleasPayloadBuilder = SetPleasHelper.setPleasPayloadBuilder();
        if (welshHearingEnabled) {
            setPleasPayloadBuilder.welshHearing(welshHearing);
        }
        if (interpreterEnabled) {
            setPleasPayloadBuilder.withInterpreter(language, needed);
        }

        if (disabilityNeeds != null) {
            setPleasPayloadBuilder.disabilityNeeds(disabilityNeeds);
        }

        pleaInfoList.forEach(pleaInfo -> setPleasPayloadBuilder
                .withPlea(pleaInfo.getLeft(),
                        pleaInfo.getMiddle(),
                        pleaInfo.getRight()));

        SetPleasHelper.setPleas(caseId, setPleasPayloadBuilder.build());

        final List<Matcher> pleaMatchers = Lists.newArrayList();
        pleaInfoList.forEach(p -> {
            if (null != p.getRight()) {
                pleaMatchers.add(withJsonPath("$.defendant.offences[?(@.id == '" + p.getLeft() + "')].plea", hasItem(p.getRight().toString())));
            } else {
                pleaMatchers.add(withJsonPath("$.defendant.offences[?(@.id == '" + p.getLeft() + "')].plea", hasSize(0)));
            }
        });
        pollForCase(caseId, pleaMatchers.toArray(new Matcher[0]));
    }

    public static void setPleas(final UUID caseId, final UUID defendantId, final PleaInfo... pleas) {
        final SetPleasHelper.SetPleasPayloadBuilder setPleasPayloadBuilder = setPleasPayloadBuilder()
                .welshHearing(false)
                .withInterpreter(null, false);

        stream(pleas).forEach(plea -> setPleasPayloadBuilder.withPlea(plea.offenceId, defendantId, plea.pleaType));

        final List<? extends Matcher<? super ReadContext>> pleaMatchers = stream(pleas)
                .map(p -> withJsonPath("$.defendant.offences[?(@.id == '" + p.offenceId + "')].plea", hasItem(p.pleaType.toString())))
                .toList();

        setPleas(caseId, setPleasPayloadBuilder.build());
        pollForCase(caseId, pleaMatchers.toArray(new Matcher[0]));
    }
}
