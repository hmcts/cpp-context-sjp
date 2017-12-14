package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepositoryImpl;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class RemovePersonInfoListener {

    @Inject
    CaseSearchResultRepository caseSearchResultRepository;

    @Inject
    CaseSearchResultRepositoryImpl caseSearchResultRepositoryImpl;

    @Transactional
    @Handles("sjp.events.person-info-removed")
    public void removePersonInfo(JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID personInfoId = UUID.fromString(payload.getString("personInfoId"));
        final CaseSearchResult caseSearchResult = caseSearchResultRepository.findBy(personInfoId);

        final String mappingIdFromSearchResult = caseSearchResult.getId().toString();
        caseSearchResultRepositoryImpl.removePersonInfo(mappingIdFromSearchResult);
    }
}