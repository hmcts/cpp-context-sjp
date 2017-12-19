package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.listener.converter.FinancialMeansConverter;
import uk.gov.moj.cpp.sjp.event.listener.converter.OnlinePleaConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class FinancialMeansListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private FinancialMeansRepository financialMeansRepository;

    @Inject
    private FinancialMeansConverter financialMeansConverter;

    @Inject
    private OnlinePleaConverter onlinePleaConverter;

    @Inject
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Inject
    private DefendantRepository defendantRepository;

    @Handles("sjp.events.financial-means-updated")
    public void updateFinancialMeans(final JsonEnvelope event) {
        final FinancialMeansUpdated financialMeansUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), FinancialMeansUpdated.class);
        final FinancialMeans financialMeans = financialMeansConverter.convertToFinancialMeansEntity(financialMeansUpdated);
        financialMeansRepository.save(financialMeans);

        if (financialMeansUpdated.isUpdatedByOnlinePlea()) {
            final DefendantDetail defendantDetail = defendantRepository.findBy(financialMeansUpdated.getDefendantId());
            final OnlinePlea onlinePlea = onlinePleaConverter.convertToOnlinePleaEntity(defendantDetail, financialMeansUpdated);
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
        }
    }
}
