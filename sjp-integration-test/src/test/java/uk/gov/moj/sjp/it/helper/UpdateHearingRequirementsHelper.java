package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;

public class UpdateHearingRequirementsHelper implements AutoCloseable {

    private MessageConsumer messageConsumer;

    public UpdateHearingRequirementsHelper() {
        messageConsumer = TopicUtil.publicEvents.createConsumer("public.sjp.case-update-rejected");
    }

    /**
     * Makes a post call to update the hearing requirements. The hearing requirements consists of
     * hearing language and interpreter
     */
    private void updateHearingRequirements(final UUID caseId, final String defendantId, final JsonObject payload) {
        final String resource = String.format("/cases/%s/defendants/%s", caseId, defendantId);
        final String contentType = "application/vnd.sjp.update-hearing-requirements+json";

        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    public void updateHearingRequirements(final UUID caseId, final String defendantId, final String interpreterLanguage, final Boolean speakWelsh) {
        updateHearingRequirements(caseId, defendantId, buildUpdateHearingRequirementsPayload(interpreterLanguage, speakWelsh));
    }

    private Response getCase(final String caseId) {
        final String resource = format("/cases/%s", caseId);
        final String contentType = "application/vnd.sjp.query.case+json";
        return HttpClientUtil.makeGetCall(resource, contentType);
    }

    public String pollForEmptyInterpreter(final UUID caseId, final String defendantId) {
        return pollForInterpreter(caseId, defendantId, null);
    }

    public String pollForInterpreter(final UUID caseId, final String defendantId, final String expectedInterpreterLanguage) {
        final Interpreter interpreter = Interpreter.of(expectedInterpreterLanguage);

        final Matcher<? super ReadContext> languageMatcher =
                interpreter.isNeeded() ?
                        withJsonPath("language", equalTo(expectedInterpreterLanguage)) :
                        withoutJsonPath("language");

        final Matcher<? super ReadContext> neededMatcher = withJsonPath("needed", equalTo(interpreter.isNeeded()));

        return await().atMost(20, TimeUnit.SECONDS).until(() -> getCase(caseId.toString()).readEntity(String.class),
                isJson(withJsonPath("$.defendant",
                        isJson(allOf(
                                withJsonPath("id", is(defendantId)),
                                withJsonPath("interpreter", isJson(allOf(languageMatcher, neededMatcher)))
                        )))));
    }

    public String pollForSpeakWelsh(final UUID caseId, final String defendantId, final Boolean expectedSpeakWelsh) {
        return pollForSpeakWelsh(caseId, defendantId, withJsonPath("speakWelsh", is(expectedSpeakWelsh)));
    }

    public String pollForEmptySpeakWelsh(final UUID caseId, final String defendantId) {
        return pollForSpeakWelsh(caseId, defendantId, withoutJsonPath("speakWelsh"));
    }

    private String pollForSpeakWelsh(final UUID caseId, final String defendantId, final Matcher<? super ReadContext> speakWelshMatcher) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> getCase(caseId.toString()).readEntity(String.class),
                isJson(withJsonPath("$.defendant",
                        isJson(speakWelshMatcher))));
    }

    public static JsonObject buildUpdateHearingRequirementsPayload(final String interpreterLanguage, final Boolean speakWelsh) {
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        ofNullable(interpreterLanguage).ifPresent(language -> payloadBuilder.add("interpreterLanguage", language));
        ofNullable(speakWelsh).ifPresent(sw -> payloadBuilder.add("speakWelsh", sw));

        return payloadBuilder.build();
    }


    @Override
    public void close() throws Exception {
        messageConsumer.close();
    }

}