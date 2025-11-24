package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseDocumentRepository extends EntityRepository<CaseDocument, UUID> {

    @Query(value = "SELECT cd FROM CaseDocument cd WHERE cd.documentType = :documentType AND cd.addedAt >= :fromDate AND cd.addedAt < :toDate ORDER BY cd.addedAt DESC")
    List<CaseDocument> findCaseDocumentsOrderedByAddedByDescending(
            @QueryParam("fromDate") final ZonedDateTime fromDate,
            @QueryParam("toDate") final ZonedDateTime toDate,
            @QueryParam("documentType") final String documentType);

    @Query(value = "FROM CaseDocument cd WHERE cd.materialId = :materialId")
    CaseDocument findByMaterialId(@QueryParam("materialId") UUID materialId);
}
