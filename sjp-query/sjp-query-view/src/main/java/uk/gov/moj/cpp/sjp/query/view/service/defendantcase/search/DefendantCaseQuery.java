package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class DefendantCaseQuery {

    public static final String CASES_QUERY_NAME = "unifiedsearch.query.cases";
    public static final String CASES_PAGE_SIZE = "pageSize";
    public static final String CASES_START_FROM = "startFrom";
    public static final int CASES_DEFAULT_PAGE_SIZE = 25;
    public static final String FIRSTNAME_QUERY_PARAM = "partyFirstAndOrMiddleName";
    public static final String LASTNAME_QUERY_PARAM = "partyLastNameOrOrganisationName";
    public static final String PARTY_NAME_QUERY_PARAM = "partyName";
    public static final String DOB_QUERY_PARAM = "partyDateOfBirth";
    public static final String ADDRESS_LINE1_QUERY_PARAM = "partyAddress";
    public static final String POSTCODE_QUERY_PARAM = "partyPostcode";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final String ASN_QUERY_PARAM = "partyArrestSummonsNumber";

    private final DefendantDetail defendant;
    private JsonObject criteria;

    public DefendantCaseQuery(DefendantDetail defendant) {
        this.defendant = defendant;
        populateDefendantCriteria();
    }

    private void populateDefendantCriteria() {
        final JsonObjectBuilder criteriaBuilder = createObjectBuilder();
        criteriaBuilder.add(CASES_PAGE_SIZE, CASES_DEFAULT_PAGE_SIZE);

        final PersonalDetails personalDetails = defendant.getPersonalDetails();
        addQueryParam(criteriaBuilder, FIRSTNAME_QUERY_PARAM, personalDetails.getFirstName());
        addQueryParam(criteriaBuilder, LASTNAME_QUERY_PARAM, personalDetails.getLastName());
        addQueryParam(criteriaBuilder, DOB_QUERY_PARAM, personalDetails.getDateOfBirth());

        final Address address = defendant.getAddress();
        if (address != null) {
            addQueryParam(criteriaBuilder, ADDRESS_LINE1_QUERY_PARAM, address.getAddress1());
            addQueryParam(criteriaBuilder, POSTCODE_QUERY_PARAM, address.getPostcode());
        }

        criteria = criteriaBuilder.build();
    }

    public JsonObject getCriteria() {
        return criteria;
    }

    private void addQueryParam(JsonObjectBuilder defendantCriteriaBuilder,
                               String queryName,
                               LocalDate queryDate) {
        if (queryDate != null) {
            final String queryDateStr = DATE_FORMATTER.format(queryDate);
            addQueryParam(defendantCriteriaBuilder, queryName, queryDateStr);
        }
    }

    private void addQueryParam(JsonObjectBuilder defendantCriteriaBuilder,
                               String queryName,
                               String queryVal) {
        if (isNotEmpty(queryVal)) {
            defendantCriteriaBuilder.add(queryName, queryVal);
        }
    }

    @Override
    public String toString() {
        return "DefendantPotentialCaseCriteria{" +
                "criteria=" + criteria +
                '}';
    }
}
