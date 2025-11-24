package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class DefendantService {

    private static final String LIMIT_QUERY_PARAM = "limit";
    private static final int UPDATES_DAYS_HISTORY = 10;

    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Inject
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    @Inject
    private ReferenceDataService referenceDataService;

    public DefendantDetailsUpdatesView findDefendantDetailUpdates(final JsonEnvelope envelope) {
        final String prosecutingAuthorityFilterValue = prosecutingAuthorityAccessFilterConverter
                .convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityProvider
                        .getCurrentUsersProsecutingAuthorityAccess(envelope));

        final List<UpdatedDefendantDetails> updatedDefendantDetails = defendantRepository.findUpdatedByCaseProsecutingAuthority(
                prosecutingAuthorityFilterValue,
                ZonedDateTime.now().minusDays(UPDATES_DAYS_HISTORY),
                ZonedDateTime.now());

        List<UpdatedDefendantDetails> results = updatedDefendantDetails;

        final String filterCriteria = getRegionFilterCriteria(envelope);

        if (isFilterByUnknownRegion(filterCriteria)) {
            results = filterByUnknownRegion(updatedDefendantDetails);
        }

        if (isFilterByRegionId(filterCriteria)) {
            results = filterByRegionId(envelope, updatedDefendantDetails, filterCriteria);
        }

        return DefendantDetailsUpdatesView.of(updatedDefendantDetails.size(), sortByMostRecentUpdated(envelope, results));
    }

    private List<UpdatedDefendantDetails> filterByRegionId(final JsonEnvelope envelope, final List<UpdatedDefendantDetails> updatedDefendantDetails, final String filterCriteria) {
        final List<UpdatedDefendantDetails> sortedDefendantDetailsResult;
        final List<RegionalOrganisation> regions = referenceDataService.getRegionalOrganisations(envelope);
        final String RegionValue = getRegionNameById(filterCriteria, regions);

        sortedDefendantDetailsResult = updatedDefendantDetails
                .stream()
                .filter(line -> RegionValue.equalsIgnoreCase(line.getRegion()))
                .collect(Collectors.toList());
        return sortedDefendantDetailsResult;
    }

    private boolean isFilterByRegionId(final String filterCriteria) {
        return !isBlankRegion(filterCriteria) && nonNull(filterCriteria);
    }

    private List<UpdatedDefendantDetails> filterByUnknownRegion(final List<UpdatedDefendantDetails> updatedDefendantDetails) {
        return updatedDefendantDetails
                .stream()
                .filter(line -> isBlank(line.getRegion()))
                .collect(Collectors.toList());
    }

    private boolean isFilterByUnknownRegion(final String filterCriteria) {
        return nonNull(filterCriteria) && isBlankRegion(filterCriteria);
    }

    private String getRegionFilterCriteria(final JsonEnvelope envelope) {
        return envelope.payloadAsJsonObject().getString("regionId", null);
    }

    private List<UpdatedDefendantDetails> sortByMostRecentUpdated(final JsonEnvelope envelope, final List<UpdatedDefendantDetails> updatedDefendantDetails) {
        return updatedDefendantDetails.stream()
                .sorted(comparing(defendantDetails -> defendantDetails.getMostRecentUpdateDate().get()))
                .limit(envelope.payloadAsJsonObject().getInt(LIMIT_QUERY_PARAM))
                .collect(toList());
    }

    private String getRegionNameById(String regionId, List<RegionalOrganisation> regions) {
        return regions
                .stream()
                .filter(region -> regionId.equalsIgnoreCase(region.getId().toString()))
                .findAny()
                .orElseThrow(IllegalArgumentException::new)
                .getRegionName();
    }

    private boolean isBlankRegion(String regionId) {
        return "UNKNOWN".equalsIgnoreCase(regionId) || isBlank(regionId);
    }

    public DefendantDetail findDefendantDetailById(final UUID defendantId) {
        return defendantRepository.findBy(defendantId);
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
                                .withDateOfBirth(defendantDetail.getPersonalDetails().getDateOfBirth() != null ?
                                        defendantDetail.getPersonalDetails().getDateOfBirth().toString() : null)
                                .withNationalInsuranceNumber(defendantDetail.getPersonalDetails().getNationalInsuranceNumber())
                                .build()
                )
                .collect(toList());
        return new DefendantOutstandingFineRequestsQueryResult(defendantDetails);
    }
}
