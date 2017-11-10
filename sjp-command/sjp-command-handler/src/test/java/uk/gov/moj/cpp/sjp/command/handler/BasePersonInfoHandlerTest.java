package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.PersonInfoDetails;

import javax.json.JsonObject;
import java.util.UUID;

public class BasePersonInfoHandlerTest {


    protected final String ADDRESS_1 = "14 Tottenham Court Road";
    protected final String ADDRESS_2 = "London";
    protected final String ADDRESS_3 = "England";
    protected final String ADDRESS_4 = "UK";
    protected final String POSTCODE = "W1T 1JY";

    final UUID personId = UUID.randomUUID();
    final UUID defendantId = randomUUID();
    final UUID caseId = randomUUID();
    final String firstName = "test";
    final String lastName = "lastName";
    final String email = "email";
    final String gender = "gender";
    final String nationalInsuranceNumber = "nationalInsuranceNumber";
    final String homeNumber = "homeNumber";
    final String mobileNumber = "mobileNumber";
    final Address address = new Address(ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE);
    final String dateOfBirth = "1980-07-15";

    protected String getStringOrNull(final JsonObject object, final String fieldName) {
        return getString(object, fieldName).orElse(null);
    }


    protected JsonObject addAddressToPayload(Address address){
        return createObjectBuilder()
                .add("address1", address.getAddress1())
                .add("address2", address.getAddress2())
                .add("address3", address.getAddress3())
                .add("address4", address.getAddress4())
                .add("postCode", address.getPostCode()).build();
    }

    protected JsonObject addPersonInfoDetailsToPayload(PersonInfoDetails personInfoDetails){
        return createObjectBuilder()
                .add("personId", personInfoDetails.getPersonId().toString())
                .add("title", personInfoDetails.getTitle())
                .add("firstName", personInfoDetails.getFirstName())
                .add("lastName", personInfoDetails.getLastName())
                .add("dateOfBirth", personInfoDetails.getDateOfBirth().toString())
                .add("address1", address.getAddress1())
                .add("address2", address.getAddress2())
                .add("address3", address.getAddress3())
                .add("address4", address.getAddress4())
                .add("postCode", address.getPostCode()).build();
    }
}
