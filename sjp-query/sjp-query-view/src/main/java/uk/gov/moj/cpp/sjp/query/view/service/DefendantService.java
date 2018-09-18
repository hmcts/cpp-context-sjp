package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.view.UpdatedDefendantDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantDetailsUpdatesView;

import java.time.ZonedDateTime;
import java.util.List;

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

}
