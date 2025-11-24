package uk.gov.moj.cpp.sjp.query.view;


import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.view.response.RegionView;
import uk.gov.moj.cpp.sjp.query.view.response.RegionsView;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.view.service.RegionalOrganisation;

import java.util.List;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_VIEW)
public class RegionsQueryView {

    @Inject
    private ReferenceDataService referenceDataService;

    @Handles("sjp.query.regions")
    public Envelope<RegionsView> getRegions(final JsonEnvelope envelope) {
        final List<RegionalOrganisation> regionalOrganisations = referenceDataService.getRegionalOrganisations(envelope);

        final List<RegionView> regions = regionalOrganisations.stream()
                .map(this::toRegionView)
                .collect(toList());

        return Enveloper.envelop(new RegionsView(regions))
                .withName("sjp.query.regions")
                .withMetadataFrom(envelope);
    }

    private RegionView toRegionView(final RegionalOrganisation regionalOrganisation) {
        return new RegionView(regionalOrganisation.getId(), regionalOrganisation.getRegionName());
    }
}
