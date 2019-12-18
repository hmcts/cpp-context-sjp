package uk.gov.moj.cpp.sjp.query.view.converter.prompts;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class EmployerPromptConverter extends PromptConverter {

    private final String fieldName;

    public EmployerPromptConverter(final String fieldName) {
        this.fieldName = fieldName;
    }

    private Optional<String> getValueFromEmployer(final OffenceDataSupplier offenceDataSupplier){
        final Employer employer = offenceDataSupplier.getEmployerSupplier();

        if(nonNull(employer)){
            if("name".equals(fieldName)) {
                return ofNullable(employer.getName());
            } else if("defendantId".equals(fieldName)) {
                return ofNullable(employer.getDefendantId()).map(UUID::toString);
            } else if("phone".equals(fieldName)) {
                return ofNullable(employer.getPhone());
            } else if("employeeReference".equals(fieldName)) {
                return ofNullable(employer.getEmployeeReference());
            }

            if(nonNull(employer.getAddress())) {
                return getValueFromEmployerAddress(employer.getAddress());
            }
        }
        return empty();
    }

    private Optional<String> getValueFromEmployerAddress(Address address) {
        if("address.address1".equals(fieldName)) {
            return ofNullable(address.getAddress1());
        } else if("address.address2".equals(fieldName)) {
            return ofNullable(address.getAddress2());
        }else if("address.address3".equals(fieldName)) {
            return ofNullable(address.getAddress3());
        }else if("address.address4".equals(fieldName)) {
            return ofNullable(address.getAddress4());
        }else if("address.address5".equals(fieldName)) {
            return ofNullable(address.getAddress5());
        } else if("address.postcode".equals(fieldName)) {
            return ofNullable(address.getPostcode());
        }
        return empty();
    }

    @Override
    public void createPrompt(final JsonArrayBuilder promptsPayloadBuilder, final JsonObject terminalEntry, final Prompt prompt, final OffenceDataSupplier offenceDataSupplier) {
        getValueFromEmployer(offenceDataSupplier).ifPresent(value -> addPrompt(promptsPayloadBuilder, prompt.getId(), value));
    }
}
