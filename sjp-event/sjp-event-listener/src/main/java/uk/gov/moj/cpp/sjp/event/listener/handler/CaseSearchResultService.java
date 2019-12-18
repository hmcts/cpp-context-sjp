package uk.gov.moj.cpp.sjp.event.listener.handler;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResultList;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

public class CaseSearchResultService {

    @Inject
    private CaseSearchResultRepository repository;

    @Transactional
    public void onDefendantDetailsUpdated(final UUID caseId, final UUID defendantId, final String newFirstName, final String newLastName, final LocalDate newDateOfBirth, final ZonedDateTime dateAdded) {
        final CaseSearchResultList existingEntryList = new CaseSearchResultList(repository.findByCaseId(caseId));

        if (existingEntryList.hasDateOfBirthChanged(newDateOfBirth)) {
            existingEntryList.setDateOfBirth(newDateOfBirth);
        }
        if (existingEntryList.hasNameChanged(newFirstName, newLastName)) {
            existingEntryList.setName(caseId, defendantId, newFirstName, newLastName, newDateOfBirth, dateAdded);
        }

        existingEntryList.forEach(repository::save);
    }

    @Transactional
    public void caseAssigned(final UUID caseId) {
        updateCaseAssignment(caseId, true);
    }

    @Transactional
    public void caseUnassigned(final UUID caseId) {
        updateCaseAssignment(caseId, false);
    }

    private void updateCaseAssignment(final UUID caseId, final boolean assigned) {
        repository.findByCaseId(caseId).forEach(result -> result.setAssigned(assigned));
    }
}
