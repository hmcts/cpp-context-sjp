package uk.gov.moj.cpp.sjp.query.view.service;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.persistence.repository.EmployerRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.EmployerConverter;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmployerServiceTest {

    private static final UUID DEFENDANT_ID = UUID.randomUUID();

    private static final Employer EMPTY_EMPLOYER =  new Employer(DEFENDANT_ID, null, null, null, null);

    private uk.gov.moj.cpp.sjp.persistence.entity.Employer entityEmployer;

    @Mock
    private EmployerRepository employerRepository;

    @Spy
    private EmployerConverter employerConverter = new EmployerConverter();

    @InjectMocks
    private EmployerService employerService;

    @Before
    public void setup() {
        // given
        entityEmployer = new uk.gov.moj.cpp.sjp.persistence.entity.Employer(DEFENDANT_ID);
        when(employerRepository.findBy(eq(DEFENDANT_ID))).thenReturn(entityEmployer);
    }

    @Test
    public void shouldRetrieveEmployer() {
        // when
        Optional<Employer> employer = employerService.getEmployer(DEFENDANT_ID);

        // then
        assertTrue(EqualsBuilder.reflectionEquals(employer.get(), EMPTY_EMPLOYER));
    }

}
