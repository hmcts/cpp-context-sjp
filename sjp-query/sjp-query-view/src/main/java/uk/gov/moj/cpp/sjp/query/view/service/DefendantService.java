package uk.gov.moj.cpp.sjp.query.view.service;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequest;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequestsQueryResult;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantDetailsUpdatesView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantProfilingView;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class DefendantService {

    private static final String LIMIT_QUERY_PARAM = "limit";
    private static final int UPDATES_DAYS_HISTORY = 10;

    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Inject
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    public DefendantDetailsUpdatesView findDefendantDetailUpdates(final JsonEnvelope envelope) {
        final String prosecutingAuthorityFilterValue = prosecutingAuthorityAccessFilterConverter
                                                               .convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityProvider
                                                                                                                  .getCurrentUsersProsecutingAuthorityAccess(envelope));

        final List<UpdatedDefendantDetails> updatedDefendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(
                prosecutingAuthorityFilterValue,
                ZonedDateTime.now().minusDays(UPDATES_DAYS_HISTORY),
                ZonedDateTime.now());

        final List<UpdatedDefendantDetails> sortedDefendantDetails = updatedDefendantDetails.stream()
                                                                             .sorted(comparing(defendantDetails -> defendantDetails.getMostRecentUpdateDate().get()))
                                                                             .limit(envelope.payloadAsJsonObject().getInt(LIMIT_QUERY_PARAM))
                                                                             .collect(toList());

        return DefendantDetailsUpdatesView.of(
                updatedDefendantDetails.size(),
                sortedDefendantDetails);
    }

    public DefendantProfilingView getDefendantProfilingView(final UUID defendantId) {
        final DefendantDetail defendant = defendantRepository.findBy(defendantId);
        if (defendant == null) {
            return null;
        } else {
            return DefendantProfilingView.newBuilder()
                           .withId(defendantId)
                           .withFirstName(defendant.getPersonalDetails().getFirstName())
                           .withLastName(defendant.getPersonalDetails().getLastName())
                           .withDateOfBirth(defendant.getPersonalDetails().getDateOfBirth())
                           .withNationalInsuranceNumber(defendant.getPersonalDetails().getNationalInsuranceNumber())
                           .build();
        }
    }

    public DefendantOutstandingFineRequestsQueryResult getOutstandingFineRequests() {
        final List<DefendantDetail> byCaseDetails = defendantRepository.findByReadyCases();
        if (byCaseDetails == null) {
            return new DefendantOutstandingFineRequestsQueryResult();
        }
        final List<DefendantOutstandingFineRequest> defendantDetails = byCaseDetails.stream()
                                                                               .map(
                                                                                       defendantDetail -> DefendantOutstandingFineRequest.newBuilder()
                                                                                                                  .withDefendantId(defendantDetail.getId())
                                                                                                                  .withCaseId(defendantDetail.getCaseDetail().getId())
                                                                                                                  .withFirstName(defendantDetail.getPersonalDetails().getFirstName())
                                                                                                                  .withLastName(defendantDetail.getPersonalDetails().getLastName())
                                                                                                                  .withDateOfBirth(defendantDetail.getPersonalDetails().getDateOfBirth().toString())
                                                                                                                  .withNationalInsuranceNumber(defendantDetail.getPersonalDetails().getNationalInsuranceNumber())
                                                                                                                  .build()
                                                                               )
                                                                               .collect(toList());
        return new DefendantOutstandingFineRequestsQueryResult(defendantDetails);
    }
}
