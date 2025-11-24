package uk.gov.moj.cpp.sjp.command.api.service;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isWhitespace;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ContactDetailsService {

    private static final String EMAIL = "email";
    private static final String EMAIL2 = "email2";

    private ContactDetailsService() {

    }

    public static JsonObject convertBlankEmailsToNull(final JsonObject contactDetailsJsonObject) {
        if (isNull(contactDetailsJsonObject)) {
            return null;
        }

        return convertBlankEmailToNull(convertBlankEmailToNull(contactDetailsJsonObject, EMAIL), EMAIL2);
    }

    private static JsonObject convertBlankEmailToNull(final JsonObject contactDetailsJsonObject, final String email) {
        if (!contactDetailsJsonObject.containsKey(email)) {
            return contactDetailsJsonObject;
        }

        final String emailAddress = contactDetailsJsonObject.getString(email);
        if(isWhitespace(emailAddress)) {
            final JsonObjectBuilder contactDetailsObjectBuilder = createObjectBuilderWithFilter(contactDetailsJsonObject, field -> !field.equals(email));
            return contactDetailsObjectBuilder.build();
        }
        return contactDetailsJsonObject;
    }
}
