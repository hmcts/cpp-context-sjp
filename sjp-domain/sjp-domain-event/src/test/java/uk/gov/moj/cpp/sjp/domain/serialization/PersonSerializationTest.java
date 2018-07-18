package uk.gov.moj.cpp.sjp.domain.serialization;

import static org.hamcrest.CoreMatchers.equalTo;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Person;

import java.time.LocalDate;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;

public class PersonSerializationTest extends AbstractSerializationTest<Person> {

    private static final Person FULL_PERSON = new Person(
            "Mr", "Fred", "Smith",
            LocalDate.of(1965, 12, 27), "Male",
            new Address("Flat 1", "1 Old Road", "London","United Kingdom", "SW99 1AA")
    );

    private static final String EXPECTED_FULL_PERSON_SERIALIZATION = "{\"title\":\"Mr\",\"firstName\":\"Fred\",\"lastName\":\"Smith\",\"dateOfBirth\":\"1965-12-27\",\"gender\":\"Male\",\"address\":{\"address1\":\"Flat 1\",\"address2\":\"1 Old Road\",\"address3\":\"London\",\"address4\":\"United Kingdom\",\"postcode\":\"SW99 1AA\"}}";

    @Override
    Map<Person, Matcher<String>> getParams() {
        return ImmutableMap.of(
                FULL_PERSON, equalTo(EXPECTED_FULL_PERSON_SERIALIZATION)
        );
    }

}