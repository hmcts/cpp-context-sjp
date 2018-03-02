package uk.gov.moj.sjp.it.verifier;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;

public class PersonInfoVerifier {

    private final UUID caseId;

    private final PersonalDetails personalDetails = new PersonalDetails("Mr", "David", "LLOYD", LocalDates.from("1980-07-15"),
            "Male", "nationalInsuranceNumber",
            new Address("14 Tottenham Court Road", "London", "England", "UK", "W1T 1JY"),
            new ContactDetails(null, null, null)
    );

    public PersonInfoVerifier(final UUID caseId) {
        this.caseId = caseId;
    }

    public void verifyPersonInfo() {
        verifyPersonInfo(this.personalDetails, false);
    }

    @SuppressWarnings("unchecked")
    public void verifyPersonInfo(final PersonalDetails personalDetails, final boolean includeContactsAndNiNumberFields) {
        List<Matcher> fieldMatchers = getCommonFieldMatchers(personalDetails);
        if (includeContactsAndNiNumberFields) {
            fieldMatchers.addAll(getContactsAndNiNumberMatchers(personalDetails));
        }
 
         poll(getCaseById(caseId.toString()))
                .until(status().is(OK), payload().isJson(allOf(
                        fieldMatchers.toArray(new Matcher[fieldMatchers.size()])
                )));
    }

    private List<Matcher> getCommonFieldMatchers(final PersonalDetails personalDetails) {
        return Lists.newArrayList(
                withJsonPath("$.defendant.personalDetails.title", equalTo(personalDetails.getTitle())),
                withJsonPath("$.defendant.personalDetails.firstName", equalTo(personalDetails.getFirstName())),
                withJsonPath("$.defendant.personalDetails.lastName", equalTo(personalDetails.getLastName())),
                withJsonPath("$.defendant.personalDetails.gender", equalTo(personalDetails.getGender())),
                withJsonPath("$.defendant.personalDetails.dateOfBirth", equalTo(LocalDates.to(personalDetails.getDateOfBirth()))),
                withJsonPath("$.defendant.personalDetails.address.address1", equalTo(personalDetails.getAddress().getAddress1())),
                withJsonPath("$.defendant.personalDetails.address.address2", equalTo(personalDetails.getAddress().getAddress2())),
                withJsonPath("$.defendant.personalDetails.address.address3", equalTo(personalDetails.getAddress().getAddress3())),
                withJsonPath("$.defendant.personalDetails.address.address4", equalTo(personalDetails.getAddress().getAddress4())),
                withJsonPath("$.defendant.personalDetails.address.postcode", equalTo(personalDetails.getAddress().getPostcode()))
        );
    }

    private List<Matcher> getContactsAndNiNumberMatchers(final PersonalDetails personalDetails) {
        return Lists.newArrayList(
                withJsonPath("$.defendant.personalDetails.nationalInsuranceNumber", equalTo(personalDetails.getNationalInsuranceNumber())),
                withJsonPath("$.defendant.personalDetails.contactDetails.email", equalTo(personalDetails.getContactDetails().getEmail())),
                withJsonPath("$.defendant.personalDetails.contactDetails.home", equalTo(personalDetails.getContactDetails().getHome())),
                withJsonPath("$.defendant.personalDetails.contactDetails.mobile", equalTo(personalDetails.getContactDetails().getMobile()))
        );
    }

    public PersonalDetails getPersonalDetails() {
        return personalDetails;
    }
}
