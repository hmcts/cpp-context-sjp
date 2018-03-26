package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class CaseSearchResultList {

    private final List<CaseSearchResult> caseSearchResults;

    public CaseSearchResultList(List<CaseSearchResult> caseSearchResults) {
        this.caseSearchResults = caseSearchResults;
    }

    private Optional<CaseSearchResult> getCurrent() {
        return caseSearchResults.stream().filter(r -> !r.isDeprecated()).findFirst();
    }

    public void setName(UUID caseId, String newFirstName, String newLastName, LocalDate newDateOfBirth, ZonedDateTime dateAdded) {
        Optional<CaseSearchResult> latest = getCurrent();

        caseSearchResults.forEach(entry -> {
            entry.setDeprecated(true);
            entry.setCurrentFirstName(newFirstName);
            entry.setCurrentLastName(newLastName);
        });

        CaseSearchResult newEntry = new CaseSearchResult(caseId, newFirstName, newLastName, newDateOfBirth, dateAdded);

        latest.ifPresent(r -> {
            newEntry.setPleaDate(r.getPleaDate());
            newEntry.setWithdrawalRequestedDate(r.getWithdrawalRequestedDate());
        });

        caseSearchResults.add(newEntry);
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        caseSearchResults.forEach(entry -> entry.setDateOfBirth(dateOfBirth));
    }

    public void forEach(Consumer<CaseSearchResult> consumer) {
        caseSearchResults.forEach(consumer);
    }

    public boolean hasDateOfBirthChanged(LocalDate newDateOfBirth) {
        return getCurrent()
                .map(old ->
                        !newDateOfBirth.equals(old.getDateOfBirth()))
                .orElse(true);
    }

    public boolean hasNameChanged(final String newFirstName, final String newLastName) {
        return getCurrent()
                .map(old ->
                        !newLastName.equalsIgnoreCase(old.getLastName()) ||
                                !newFirstName.equalsIgnoreCase(old.getFirstName()))
                .orElse(true);
    }
}
