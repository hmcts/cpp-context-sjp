package uk.gov.moj.cpp.sjp.event.listener.service;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.UUID;

public class CaseService {

    @Inject
    private CaseRepository caseRepository;

    public CaseDetail findById(final UUID caseId) {
        return caseRepository.findBy(caseId);
    }

    @Transactional
    public void saveCaseDetail(final CaseDetail caseDetail) {
        caseRepository.save(caseDetail);
    }

}