package uk.gov.moj.sjp.it.helper;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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