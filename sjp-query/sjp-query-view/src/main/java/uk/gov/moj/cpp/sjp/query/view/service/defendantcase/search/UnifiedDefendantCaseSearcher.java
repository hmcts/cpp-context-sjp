package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.CASES_DEFAULT_PAGE_SIZE;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.CASES_QUERY_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCaseQuery.CASES_START_FROM;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;

public class UnifiedDefendantCaseSearcher implements DefendantCaseSearcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnifiedDefendantCaseSearcher.class);

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    @Override
    public List<DefendantCase> searchDefendantCases(Envelope<?> envelope,
                                                    DefendantDetail defendant) {
        final List<DefendantCase> defendantCases = new ArrayList<>();
        if(!hasMandatoryDetails(defendant)){
            return defendantCases;
        }
        int page = 0;
        int totalResult = 0;
        final DefendantCaseQuery defendantQuery = new DefendantCaseQuery(defendant);
        LOGGER.info("Querying unified search defendant cases - defendantQuery={}", defendantQuery);


        while (true) {
            final JsonObjectBuilder criteriaBuilder = Json.createObjectBuilder();
            defendantQuery.getCriteria().forEach(criteriaBuilder::add);
            criteriaBuilder.add(CASES_START_FROM, page);

            LOGGER.info("Sending unified search defendant cases query - page={}", page);
            final Envelope<JsonObject> caseQueryEnvelope = envelop(criteriaBuilder.build()).
                    withName(CASES_QUERY_NAME).
                    withMetadataFrom(envelope);
            final Envelope<DefendantCaseQueryResult> response =
                    requester.requestAsAdmin(caseQueryEnvelope, DefendantCaseQueryResult.class);

            if (response != null && response.payload() != null) {
                defendantCases.addAll(response.payload().getCases());
                totalResult = response.payload().getTotalResults();
                LOGGER.info("Received unified search defendant cases result - numOfCases={}", totalResult);
            }

            page++;
            if (page * CASES_DEFAULT_PAGE_SIZE + 1 > totalResult) {
                break;
            }
        }
        LOGGER.info("All defendant cases matching query - defendantCases={}", defendantCases);

        return defendantCases;
    }

    private boolean hasMandatoryDetails(final DefendantDetail defendant) {
        final PersonalDetails personalDetails = defendant.getPersonalDetails();
        if(personalDetails != null) {
            final String firstName = personalDetails.getFirstName();
            final String lastName = personalDetails.getLastName();
            final LocalDate dateOfBirth = personalDetails.getDateOfBirth();
            final String addressLine1 = defendant.getAddress() != null ? defendant.getAddress().getAddress1() : null;
            final String postCode = defendant.getAddress() != null ? defendant.getAddress().getPostcode() : null;
            return isNotBlank(firstName) && isNotBlank(lastName) && isNotBlank(addressLine1) && isNotBlank(postCode) && null != dateOfBirth;
        }
        return false;

    }
}
