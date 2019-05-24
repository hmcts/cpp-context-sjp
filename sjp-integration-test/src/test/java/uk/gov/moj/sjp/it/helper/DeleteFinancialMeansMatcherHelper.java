package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import uk.gov.moj.cpp.sjp.domain.Income;

import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.json.JSONObject;

public class DeleteFinancialMeansMatcherHelper {

    public static Matcher<Object> getSavedOnlinePleaPayloadContentMatcher(final JSONObject onlinePleaPayload,
                                                                    final String caseId, final String defendantId, boolean isFinancialMeansDeleted) {
        final List<Matcher> fieldMatchers = getOnlinePleaMatchers(onlinePleaPayload, caseId, defendantId);

        if (isFinancialMeansDeleted) {
            fieldMatchers.addAll(getOutgoingsMatcherAfterDelete());
        } else {
            fieldMatchers.addAll(getOutgoingsMatcherBeforeDelete(onlinePleaPayload));
        }
        return isJson(allOf(
                fieldMatchers.<Matcher<ReadContext>>toArray(new Matcher[fieldMatchers.size()])
        ));
    }


    public static  Matcher<Object> getExpectedFinancialMeanDataBeforeDeletionMatcher(Income originalIncome, String employmentStatus) {
        return isJson(allOf(
                withJsonPath("$.income.frequency", is(originalIncome.getFrequency().name())),
                withJsonPath("$.income.amount", is(originalIncome.getAmount().doubleValue())),
                withoutJsonPath("$.benefits.claimed"),
                withoutJsonPath("$.benefits.type"),
                withJsonPath("$.employmentStatus", is(employmentStatus))
        ));
    }

    public static  Matcher<Object> getExpectedFinancialMeanDataAfterDeletionMatcher() {
        return isJson(allOf(
                withoutJsonPath("$.income.frequency"),
                withoutJsonPath("$.income.amount"),
                withoutJsonPath("$.benefits.claimed"),
                withoutJsonPath("$.benefits.type"),
                withoutJsonPath("$.employmentStatus")
        ));
    }

    private static List<Matcher> getOutgoingsMatcherBeforeDelete(final JSONObject onlinePleaPayload) {
        final JSONObject accommodationOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(0);
        final JSONObject councilTaxOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(1);
        final JSONObject householdBillsOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(2);
        final JSONObject travelExpensesOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(3);
        final JSONObject childMaintenanceOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(4);
        final JSONObject otherOutgoing = onlinePleaPayload.getJSONArray("outgoings").getJSONObject(5);
        return asList(
                //outgoings
                withJsonPath("$.outgoings.accommodationAmount", equalTo(accommodationOutgoing.getDouble("amount"))),
                withJsonPath("$.outgoings.councilTaxAmount", equalTo(councilTaxOutgoing.getDouble("amount"))),
                withJsonPath("$.outgoings.householdBillsAmount", equalTo(householdBillsOutgoing.getDouble("amount"))),
                withJsonPath("$.outgoings.travelExpensesAmount", equalTo(travelExpensesOutgoing.getDouble("amount"))),
                withJsonPath("$.outgoings.childMaintenanceAmount", equalTo(childMaintenanceOutgoing.getDouble("amount"))),
                withJsonPath("$.outgoings.otherDescription", equalTo(otherOutgoing.getString("description"))),
                withJsonPath("$.outgoings.otherAmount", equalTo(otherOutgoing.getDouble("amount"))),
                withJsonPath("$.outgoings.monthlyAmount", equalTo(1772.3))
        );
    }


    private static List<Matcher> getOutgoingsMatcherAfterDelete() {
        return asList(
                withoutJsonPath("$.outgoings.accommodationAmount"),
                withoutJsonPath("$.outgoings.councilTaxAmount"),
                withoutJsonPath("$.outgoings.householdBillsAmount"),
                withoutJsonPath("$.outgoings.travelExpensesAmount"),
                withoutJsonPath("$.outgoings.childMaintenanceAmount"),
                withoutJsonPath("$.outgoings.otherDescription"),
                withoutJsonPath("$.outgoings.otherAmount"),
                withoutJsonPath("$.outgoings.monthlyAmount")
        );
    }


    private static List<Matcher> getOnlinePleaMatchers(final JSONObject onlinePleaPayload, final String caseId, final String defendantId) {
        final JSONObject person = onlinePleaPayload.getJSONObject("personalDetails");
        final JSONObject personAddress = person.getJSONObject("address");
        final JSONObject personContactDetails = person.getJSONObject("contactDetails");
        final JSONObject financialMeans = onlinePleaPayload.getJSONObject("financialMeans");
        final JSONObject employer = onlinePleaPayload.getJSONObject("employer");
        final JSONObject employerAddress = employer.getJSONObject("address");

        final List<Matcher> matchers = new ArrayList<>(asList(
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.defendantId", equalTo(defendantId)),

                //personal details
                withJsonPath("$.personalDetails.firstName", equalTo(person.getString("firstName"))),
                withJsonPath("$.personalDetails.lastName", equalTo(person.getString("lastName"))),
                withJsonPath("$.personalDetails.homeTelephone", equalTo(personContactDetails.getString("home"))),
                withJsonPath("$.personalDetails.mobile", equalTo(personContactDetails.getString("mobile"))),
                withJsonPath("$.personalDetails.email", equalTo(personContactDetails.getString("email"))),
                withJsonPath("$.personalDetails.dateOfBirth", equalTo(person.getString("dateOfBirth"))),
                withJsonPath("$.personalDetails.nationalInsuranceNumber", equalTo(person.getString("nationalInsuranceNumber"))),
                withJsonPath("$.personalDetails.address.address1", equalTo(personAddress.getString("address1"))),
                withJsonPath("$.personalDetails.address.address2", equalTo(personAddress.getString("address2"))),
                withJsonPath("$.personalDetails.address.address3", equalTo(personAddress.getString("address3"))),
                withJsonPath("$.personalDetails.address.address4", equalTo(personAddress.getString("address4"))),
                withJsonPath("$.personalDetails.address.address5", equalTo(personAddress.getString("address5"))),
                withJsonPath("$.personalDetails.address.postcode", equalTo(personAddress.getString("postcode")))
        ));

        //Financial data matchers
        matchers.addAll(asList(
                //financial-means
                withJsonPath("$.employment.incomePaymentFrequency", equalTo(financialMeans.getJSONObject("income").getString("frequency"))),
                withJsonPath("$.employment.incomePaymentAmount", equalTo(financialMeans.getJSONObject("income").getDouble("amount"))),
                withJsonPath("$.employment.benefitsClaimed", equalTo(financialMeans.getJSONObject("benefits").getBoolean("claimed"))),
                withJsonPath("$.employment.benefitsType", equalTo(financialMeans.getJSONObject("benefits").getString("type"))),
                withJsonPath("$.employment.benefitsDeductPenaltyPreference", equalTo(financialMeans.getJSONObject("benefits").getBoolean("deductPenaltyPreference"))),

                //employer
                withJsonPath("$.employer.name", equalTo(employer.getString("name"))),
                withJsonPath("$.employer.employeeReference", equalTo(employer.getString("employeeReference"))),
                withJsonPath("$.employer.phone", equalTo(employer.getString("phone"))),
                withJsonPath("$.employer.address.address1", equalTo(employerAddress.getString("address1"))),
                withJsonPath("$.employer.address.address2", equalTo(employerAddress.getString("address2"))),
                withJsonPath("$.employer.address.address3", equalTo(employerAddress.getString("address3"))),
                withJsonPath("$.employer.address.address4", equalTo(employerAddress.getString("address4"))),
                withJsonPath("$.employer.address.address5", equalTo(employerAddress.getString("address5"))),
                withJsonPath("$.employer.address.postcode", equalTo(employerAddress.getString("postcode")))
        ));
        return matchers;
    }


}
