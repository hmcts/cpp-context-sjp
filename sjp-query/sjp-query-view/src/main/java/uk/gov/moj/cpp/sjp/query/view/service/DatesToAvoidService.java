package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CasesPendingDatesToAvoidView;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class DatesToAvoidService {

    @Inject
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @Inject
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    /**
     * Fetch pending cases by filter criteria. Filter Criteria can be: Null = do not apply any
     * filters. Returns all pending cases UNKNOWN = filter by defendant's with no region regionId =
     * filter by defendant's region
     */
    public CasesPendingDatesToAvoidView findCasesPendingDatesToAvoid(final JsonEnvelope envelope) {
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope);
        final String prosecutingAuthorityFilterValue = prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess);

        List<PendingDatesToAvoid> pendingCases = pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid(prosecutingAuthorityFilterValue,
                prosecutingAuthorityAccess.getAgentProsecutorAuthorityAccess());
        final int total = pendingCases.size();
        final String regionFilter = getRegionFilterCriteria(envelope);

        if (isFilterByUnknownRegion(regionFilter)) {
            pendingCases = filterByUnknownRegion(pendingCases);
        }

        if (isFilterByRegionId(regionFilter)) {
            pendingCases = filterByRegionId(envelope, pendingCases, regionFilter);
        }

        return new CasesPendingDatesToAvoidView(pendingCases, total);
    }

    private String getRegionFilterCriteria(final JsonEnvelope envelope) {
        return envelope.payloadAsJsonObject().getString("regionId", null);
    }

    private boolean isFilterByUnknownRegion(final String regionFilter) {
        return nonNull(regionFilter) && isBlankRegion(regionFilter);
    }

    private boolean isBlankRegion(String regionId) {
        return "UNKNOWN".equalsIgnoreCase(regionId) || isBlank(regionId);
    }

    private boolean isFilterByRegionId(final String regionFilter) {
        return nonNull(regionFilter) && !isBlankRegion(regionFilter);
    }

    private List<PendingDatesToAvoid> filterByRegionId(final JsonEnvelope envelope, final List<PendingDatesToAvoid> pendingCases, final String regionFilter) {
        final List<RegionalOrganisation> regions = referenceDataService.getRegionalOrganisations(envelope);
        final String regionName = getRegionNameById(regionFilter, regions);

        return pendingCases
                .stream()
                .filter(pendingCase -> regionName.equalsIgnoreCase(pendingCase.getCaseDetail().getDefendant().getRegion()))
                .collect(Collectors.toList());
    }

    private List<PendingDatesToAvoid> filterByUnknownRegion(final List<PendingDatesToAvoid> pendingCases) {
        return pendingCases
                .stream()
                .filter(line -> isBlank(line.getCaseDetail().getDefendant().getRegion()))
                .collect(Collectors.toList());
    }

    private String getRegionNameById(String regionId, List<RegionalOrganisation> regions) {
        return regions.stream()
                .filter(region -> regionId.equalsIgnoreCase(region.getId().toString()))
                .findAny()
                .orElseThrow(IllegalArgumentException::new)
                .getRegionName();
    }
}