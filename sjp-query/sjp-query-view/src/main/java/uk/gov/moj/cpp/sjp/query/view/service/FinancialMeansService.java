package uk.gov.moj.cpp.sjp.query.view.service;

import uk.gov.moj.cpp.sjp.query.view.converter.FinancialMeansConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class FinancialMeansService {

    @Inject
    private FinancialMeansRepository financialMeansRepository;

    @Inject
    private FinancialMeansConverter financialMeansConverter;

    public Optional<uk.gov.moj.cpp.sjp.domain.FinancialMeans> getFinancialMeans(final UUID defendantId) {

        final FinancialMeans financialMeans = financialMeansRepository.findBy(defendantId);
        return Optional.ofNullable(financialMeans).map(financialMeansConverter::convertToFinancialMeans);
    }
}
