package uk.gov.moj.cpp.sjp.query.view.service;


import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.DatesToAvoidsView;

import java.util.List;

import javax.inject.Inject;

public class DatesToAvoidService {

    @Inject
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @Inject
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Inject
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    public DatesToAvoidsView findCasesPendingDatesToAvoid(final JsonEnvelope envelope) {
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope);
        final String prosecutingAuthorityFilterValue = prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess);
        final List<PendingDatesToAvoid> pendingDatesToAvoidList = pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid(prosecutingAuthorityFilterValue);
        return new DatesToAvoidsView(pendingDatesToAvoidList);
    }

}
