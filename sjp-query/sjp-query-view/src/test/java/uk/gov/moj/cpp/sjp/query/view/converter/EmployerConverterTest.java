package uk.gov.moj.cpp.sjp.query.view.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.persistence.entity.Employer;

import java.util.UUID;

import org.junit.Test;

public class EmployerConverterTest {

    private final EmployerConverter employerConverter = new EmployerConverter();

    @Test
    public void shouldConvertEmployer() {

        final Employer entity = new Employer(UUID.randomUUID());
        entity.setName("name");
        entity.setEmployeeReference("employeeReference");
        entity.setPhone("phone");
        entity.setAddress1("address4");
        entity.setAddress2("address2");
        entity.setAddress3("address3");
        entity.setAddress4("address4");
        entity.setPostCode("postCode");
        final uk.gov.moj.cpp.sjp.domain.Employer employer = employerConverter.convertToEmployer(entity);

        assertThat(employer.getName(), is(entity.getName()));
        assertThat(employer.getEmployeeReference(), is(entity.getEmployeeReference()));
        assertThat(employer.getPhone(), is(entity.getPhone()));
        assertThat(employer.getAddress().getAddress1(), is(entity.getAddress1()));
        assertThat(employer.getAddress().getAddress2(), is(entity.getAddress2()));
        assertThat(employer.getAddress().getAddress3(), is(entity.getAddress3()));
        assertThat(employer.getAddress().getAddress4(), is(entity.getAddress4()));
        assertThat(employer.getAddress().getPostcode(), is(entity.getPostCode()));
    }

    @Test
    public void shouldConvertBlankEmployer() {

        final uk.gov.moj.cpp.sjp.domain.Employer employer = employerConverter.convertToEmployer(new Employer());

        assertThat(employer.getAddress(), nullValue());
    }

}