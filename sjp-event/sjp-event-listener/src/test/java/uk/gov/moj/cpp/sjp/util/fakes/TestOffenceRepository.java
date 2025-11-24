package uk.gov.moj.cpp.sjp.util.fakes;

import static java.util.Arrays.asList;

import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.metamodel.SingularAttribute;

public class TestOffenceRepository implements OffenceRepository {

    private final List<OffenceDetail> offences;

    public TestOffenceRepository() {
        this.offences = new ArrayList();
    }

    @Override
    public List<OffenceDetail> findByDefendantDetail(final DefendantDetail defendantDetail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OffenceDetail> findByIds(final List<UUID> offenceIds) {
        return offences.stream()
                .filter(o -> offenceIds.contains(o.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public OffenceDetail save(final OffenceDetail offenceDetail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OffenceDetail saveAndFlush(final OffenceDetail offenceDetail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OffenceDetail saveAndFlushAndRefresh(final OffenceDetail offenceDetail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(final OffenceDetail offenceDetail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAndFlush(final OffenceDetail offenceDetail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachAndRemove(final OffenceDetail offenceDetail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(final OffenceDetail offenceDetail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<OffenceDetail> findOptionalBy(final UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public OffenceDetail findBy(final UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OffenceDetail> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OffenceDetail> findAll(final int i, final int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OffenceDetail> findBy(final OffenceDetail offenceDetail, final SingularAttribute<OffenceDetail, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OffenceDetail> findBy(final OffenceDetail offenceDetail, final int i, final int i1, final SingularAttribute<OffenceDetail, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OffenceDetail> findByLike(final OffenceDetail offenceDetail, final SingularAttribute<OffenceDetail, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OffenceDetail> findByLike(final OffenceDetail offenceDetail, final int i, final int i1, final SingularAttribute<OffenceDetail, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count(final OffenceDetail offenceDetail, final SingularAttribute<OffenceDetail, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long countLike(final OffenceDetail offenceDetail, final SingularAttribute<OffenceDetail, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UUID getPrimaryKey(final OffenceDetail offenceDetail) {
        throw new UnsupportedOperationException();
    }

    public void addOffences(final OffenceDetail... offences) {
        this.offences.addAll(asList(offences));
    }
}
