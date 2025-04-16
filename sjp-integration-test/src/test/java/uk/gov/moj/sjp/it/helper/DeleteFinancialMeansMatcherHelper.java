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


    public static Matcher<Object> getExpectedFinancialMeanDataBeforeDeletionMatcher(Income originalIncome, String employmentStatus) {
        return isJson(allOf(
                withJsonPath("$.income.frequency", is(originalIncome.getFrequency().name())),
                withJsonPath("$.income.amount", is(originalIncome.getAmount().doubleValue())),
                withoutJsonPath("$.benefits.claimed"),
                withoutJsonPath("$.benefits.type"),
                withJsonPath("$.employmentStatus", is(employmentStatus))
        ));
    }

    public static Matcher<Object> getExpectedFinancialMeanDataAfterDeletionMatcher() {
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
        final JSONObject financialMeans = onlinePleaPayload.getJSONObject("financialMeans");
        final JSONObject employer = onlinePleaPayload.getJSONObject("employer");
        final JSONObject employerAddress = employer.getJSONObject("address");

        final List<Matcher> matchers = new ArrayList<>(asList(
                //outgoings
                withJsonPath("$.pleas[0].outgoings.accommodationAmount", equalTo(accommodationOutgoing.getDouble("amount"))),
                withJsonPath("$.pleas[0].outgoings.councilTaxAmount", equalTo(councilTaxOutgoing.getDouble("amount"))),
                withJsonPath("$.pleas[0].outgoings.householdBillsAmount", equalTo(householdBillsOutgoing.getDouble("amount"))),
                withJsonPath("$.pleas[0].outgoings.travelExpensesAmount", equalTo(travelExpensesOutgoing.getDouble("amount"))),
                withJsonPath("$.pleas[0].outgoings.childMaintenanceAmount", equalTo(childMaintenanceOutgoing.getDouble("amount"))),
                withJsonPath("$.pleas[0].outgoings.otherDescription", equalTo(otherOutgoing.getString("description"))),
                withJsonPath("$.pleas[0].outgoings.otherAmount", equalTo(otherOutgoing.getDouble("amount"))),
                withJsonPath("$.pleas[0].outgoings.monthlyAmount", equalTo(1772.3))
        ));

        //Financial data matchers
        matchers.addAll(asList(
                //financial-means
                withJsonPath("$.pleas[0].employment.incomePaymentFrequency", equalTo(financialMeans.getJSONObject("income").getString("frequency"))),
                withJsonPath("$.pleas[0].employment.incomePaymentAmount", equalTo(financialMeans.getJSONObject("income").getDouble("amount"))),
                withJsonPath("$.pleas[0].employment.benefitsClaimed", equalTo(financialMeans.getJSONObject("benefits").getBoolean("claimed"))),
                withJsonPath("$.pleas[0].employment.benefitsType", equalTo(financialMeans.getJSONObject("benefits").getString("type"))),
                withJsonPath("$.pleas[0].employment.benefitsDeductPenaltyPreference", equalTo(financialMeans.getJSONObject("benefits").getBoolean("deductPenaltyPreference"))),

                //employer
                withJsonPath("$.pleas[0].employer.name", equalTo(employer.getString("name"))),
                withJsonPath("$.pleas[0].employer.employeeReference", equalTo(employer.getString("employeeReference"))),
                withJsonPath("$.pleas[0].employer.phone", equalTo(employer.getString("phone"))),
                withJsonPath("$.pleas[0].employer.address.address1", equalTo(employerAddress.getString("address1"))),
                withJsonPath("$.pleas[0].employer.address.address2", equalTo(employerAddress.getString("address2"))),
                withJsonPath("$.pleas[0].employer.address.address3", equalTo(employerAddress.getString("address3"))),
                withJsonPath("$.pleas[0].employer.address.address4", equalTo(employerAddress.getString("address4"))),
                withJsonPath("$.pleas[0].employer.address.address5", equalTo(employerAddress.getString("address5"))),
                withJsonPath("$.pleas[0].employer.address.postcode", equalTo(employerAddress.getString("postcode")))
        ));
        return matchers;

    }

    private static List<Matcher> getOutgoingsMatcherAfterDelete() {
        final List<Matcher> matchers = new ArrayList<>(asList(
                withoutJsonPath("$.pleas[0].outgoings.accommodationAmount"),
                withoutJsonPath("$.pleas[0].outgoings.councilTaxAmount"),
                withoutJsonPath("$.pleas[0].outgoings.householdBillsAmount"),
                withoutJsonPath("$.pleas[0].outgoings.travelExpensesAmount"),
                withoutJsonPath("$.pleas[0].outgoings.childMaintenanceAmount"),
                withoutJsonPath("$.pleas[0].outgoings.otherDescription"),
                withoutJsonPath("$.pleas[0].outgoings.otherAmount"),
                withoutJsonPath("$.pleas[0].outgoings.monthlyAmount")
        ));

        //Financial means matchers
        matchers.addAll(asList(
                //financial-means
                withoutJsonPath("$.pleas[0].employment.incomePaymentFrequency"),
                withoutJsonPath("$.pleas[0].employment.incomePaymentAmount"),
                withoutJsonPath("$.pleas[0].employment.benefitsClaimed"),
                withoutJsonPath("$.pleas[0].employment.benefitsType"),
                withoutJsonPath("$.pleas[0].employment.benefitsDeductPenaltyPreference"),

                //employer
                withoutJsonPath("$.pleas[0].employer.name"),
                withoutJsonPath("$.pleas[0].employer.employeeReference"),
                withoutJsonPath("$.pleas[0].employer.phone"),
                withoutJsonPath("$.pleas[0].employer.address.address1"),
                withoutJsonPath("$.pleas[0].employer.address.address2"),
                withoutJsonPath("$.pleas[0].employer.address.address3"),
                withoutJsonPath("$.pleas[0].employer.address.address4"),
                withoutJsonPath("$.pleas[0].employer.address.address5"),
                withoutJsonPath("$.pleas[0].employer.address.postcode")
        ));

        return matchers;
    }


    private static List<Matcher> getOnlinePleaMatchers(final JSONObject onlinePleaPayload, final String caseId, final String defendantId) {
        final JSONObject person = onlinePleaPayload.getJSONObject("personalDetails");
        final JSONObject personContactDetails = person.getJSONObject("contactDetails");

        //personal details


        return new ArrayList<>(asList(
                withJsonPath("$.pleas[0].caseId", equalTo(caseId)),
                withJsonPath("$.pleas[0].defendantId", equalTo(defendantId)),

                //personal details
                withJsonPath("$.pleas[0].personalDetails.firstName", equalTo(person.getString("firstName"))),
                withJsonPath("$.pleas[0].personalDetails.lastName", equalTo(person.getString("lastName"))),
                withJsonPath("$.pleas[0].personalDetails.homeTelephone", equalTo(personContactDetails.getString("home"))),
                withJsonPath("$.pleas[0].personalDetails.mobile", equalTo(personContactDetails.getString("mobile"))),
                withJsonPath("$.pleas[0].personalDetails.email", equalTo(personContactDetails.getString("email"))),
                withJsonPath("$.pleas[0].personalDetails.dateOfBirth", equalTo(person.getString("dateOfBirth"))),
                withJsonPath("$.pleas[0].personalDetails.nationalInsuranceNumber", equalTo(person.getString("nationalInsuranceNumber")))
        ));
    }


}
