package uk.gov.moj.cpp.sjp.persistence.repository;

import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class BaseJpaRepositoryTest {

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

}
