package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class CaseSearchResultList {

    private final List<CaseSearchResult> caseSearchResults;

    public CaseSearchResultList(List<CaseSearchResult> caseSearchResults) {
        this.caseSearchResults = caseSearchResults;
    }

    public Optional<CaseSearchResult> getLatest() {
        return caseSearchResults.stream().filter(r -> !r.isDeprecated()).findFirst();
    }

    public void setName(String firstName, String lastName) {
        caseSearchResults.forEach(entry -> {
            entry.setDeprecated(true);
            entry.setCurrentFirstName(firstName);
            entry.setCurrentLastName(lastName);
        });
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        caseSearchResults.forEach(entry -> entry.setDateOfBirth(dateOfBirth));
    }

    public void add(UUID caseId, String newFirstName, String newLastName, LocalDate newDateOfBirth) {
        caseSearchResults.add(new CaseSearchResult(caseId, newFirstName, newLastName, newDateOfBirth));
    }

    public void forEach(Consumer<CaseSearchResult> consumer) {
        caseSearchResults.forEach(consumer);
    }

    public boolean hasDateOfBirthChanged(LocalDate newDateOfBirth) {
        return getLatest()
                .map(old ->
                        !newDateOfBirth.equals(old.getDateOfBirth()))
                .orElse(true);
    }

    public boolean hasNameChanged(final String newFirstName, final String newLastName) {
        return getLatest()
                .map(old ->
                        !newLastName.equalsIgnoreCase(old.getLastName()) ||
                                !newFirstName.equalsIgnoreCase(old.getFirstName()))
                .orElse(true);
    }
}
