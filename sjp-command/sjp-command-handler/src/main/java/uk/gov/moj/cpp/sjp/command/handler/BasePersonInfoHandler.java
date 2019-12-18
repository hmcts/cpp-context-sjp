package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.messaging.JsonObjects.getJsonObject;
import static uk.gov.justice.services.messaging.JsonObjects.getString;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.Address;

import java.util.UUID;

import javax.json.JsonObject;

public class BasePersonInfoHandler {

    protected Address createAddressFrom(final JsonObject command) {
        return getJsonObject(command, "address").map(address -> new Address(
                getStringOrNull(address, "address1"),
                getStringOrNull(address, "address2"),
                getStringOrNull(address, "address3"),
                getStringOrNull(address, "address4"),
                getStringOrNull(address, "address5"),
                getStringOrNull(address, "postcode"))).orElse(Address.UNKNOWN);
    }

    protected String getStringOrNull(final JsonObject object, final String fieldName) {
        return getString(object, fieldName).orElse(null);
    }

    protected UUID getUserId(final Envelope<?> command) {
        return command.metadata().userId()
                .map(UUID::fromString)
                .orElseThrow(RuntimeException::new);
    }
}
