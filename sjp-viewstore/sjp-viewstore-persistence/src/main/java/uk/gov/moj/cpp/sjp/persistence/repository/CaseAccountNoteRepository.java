package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.AccountNote;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseAccountNoteRepository extends EntityRepository<AccountNote, UUID> {

    List<AccountNote> findByCaseUrn(final String caseUrn);

}