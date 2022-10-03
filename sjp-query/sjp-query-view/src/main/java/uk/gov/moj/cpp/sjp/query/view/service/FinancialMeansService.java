package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.FinancialMeansConverter;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class FinancialMeansService {

    @Inject
    private FinancialMeansRepository financialMeansRepository;

    @Inject
    private OnlinePleaRepository.LegalEntityDetailsOnlinePleaRepository legalEntityDetailsOnlinePleaRepository;

    @Inject
    private FinancialMeansConverter financialMeansConverter;

    public Optional<uk.gov.moj.cpp.sjp.domain.FinancialMeans> getFinancialMeans(final UUID defendantId) {

        final FinancialMeans financialMeans = financialMeansRepository.findBy(defendantId);
        final OnlinePlea onlinePlea = legalEntityDetailsOnlinePleaRepository.findBy(defendantId);
        if (nonNull(financialMeans)) {
            return Optional.ofNullable(financialMeansConverter.convertToFinancialMeans(financialMeans, onlinePlea));
        } else {
            return Optional.empty();
        }
    }
}
