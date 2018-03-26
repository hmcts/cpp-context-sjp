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
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;

public class PersonInfoVerifier {

    private final UUID caseId;

    private final PersonalDetails personalDetails;

    private PersonInfoVerifier(final UUID caseId, final PersonalDetails personalDetails) {
        this.caseId = caseId;
        this.personalDetails = personalDetails;
    }

    public static PersonInfoVerifier personInfoVerifierForPersonalDetails(final UUID caseId, final PersonalDetails personalDetails) {
        return new PersonInfoVerifier(caseId, personalDetails);
    }

    public static PersonInfoVerifier personInfoVerifierForCasePayload(CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder) {
        final CreateCase.DefendantBuilder defendantBuilder = createCasePayloadBuilder.getDefendantBuilder();
        final PersonalDetails personalDetails = new PersonalDetails(
            defendantBuilder.getTitle(),
                defendantBuilder.getFirstName(),
                defendantBuilder.getLastName(),
                defendantBuilder.getDateOfBirth(),
                defendantBuilder.getGender(),
                null,
                new Address(
                        defendantBuilder.getAddressBuilder().getAddress1(),
                        defendantBuilder.getAddressBuilder().getAddress2(),
                        defendantBuilder.getAddressBuilder().getAddress3(),
                        defendantBuilder.getAddressBuilder().getAddress4(),
                        defendantBuilder.getAddressBuilder().getPostcode()
                ),
                new ContactDetails(null, null, null)
        );

        return new PersonInfoVerifier(createCasePayloadBuilder.getId(), personalDetails);
    }

    public static PersonInfoVerifier personInfoVerifierForDefendantUpdatedPayload(UUID caseId, UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder) {
        final PersonalDetails personalDetails = new PersonalDetails(
                payloadBuilder.getTitle(),
                payloadBuilder.getFirstName(),
                payloadBuilder.getLastName(),
                payloadBuilder.getDateOfBirth(),
                payloadBuilder.getGender(),
                payloadBuilder.getNationalInsuranceNumber(),
                new Address(
                  payloadBuilder.getAddressBuilder().getAddress1(),
                  payloadBuilder.getAddressBuilder().getAddress2(),
                  payloadBuilder.getAddressBuilder().getAddress3(),
                  payloadBuilder.getAddressBuilder().getAddress4(),
                  payloadBuilder.getAddressBuilder().getPostcode()
                ),
                new ContactDetails(
                        payloadBuilder.getContactDetailsBuilder().getEmail(),
                        payloadBuilder.getContactDetailsBuilder().getHomeNumber(),
                        payloadBuilder.getContactDetailsBuilder().getMobile()
                )
        );

        return new PersonInfoVerifier(caseId, personalDetails);
    }


    public void verifyPersonInfo() {
        verifyPersonInfo(false);
    }

    @SuppressWarnings("unchecked")
    public void verifyPersonInfo(final boolean includeContactsAndNiNumberFields) {
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
