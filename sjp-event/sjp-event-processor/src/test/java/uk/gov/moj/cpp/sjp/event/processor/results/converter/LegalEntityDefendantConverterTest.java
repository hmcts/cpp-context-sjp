package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ContactDetails;
import uk.gov.justice.json.schemas.domains.sjp.LegalEntityDetails;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LegalEntityDefendantConverterTest {
    @InjectMocks
    LegalEntityDefendantConverter legalEntityDefendantConverter;
    @Mock
    private AddressConverter addressConverter;
    @Mock
    private LegalEntityContactConverter legalEntityContactConverter;


    @Test
    public void shouldConverterLegalEntityDetailsWithValues() {

        when(addressConverter.getAddress(any())).thenCallRealMethod();
        when(legalEntityContactConverter.getContact(any())).thenCallRealMethod();

        String companyName = "Company ltd";
        final LegalEntityDetails legalEntityDetails = LegalEntityDetails.legalEntityDetails()
                .withLegalEntityName(companyName)
                .withAddress(Address.address()
                        .withAddress1("address1")
                        .withAddress2("address2")
                        .withAddress3("address3")
                        .withAddress4("address4")
                        .withAddress5("address5")
                        .withPostcode("postcode1")
                        .build())
                .withAddressChanged(false)
                .withLegalEntityNameChanged(false)
                .withContactDetails(ContactDetails.contactDetails()
                        .withBusiness("business")
                        .withHome("home")
                        .withMobile("mobile")
                        .withEmail("email1@email.com")
                        .withEmail2("email2@email.com")
                        .build())
                .build();

        final LegalEntityDefendant legalEntityDefendant = legalEntityDefendantConverter.getLegalEntityDefendant(legalEntityDetails);

        assertThat(legalEntityDefendant.getOrganisation().getName(), is(companyName));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress1(), is(legalEntityDetails.getAddress().getAddress1()));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress2(), is(legalEntityDetails.getAddress().getAddress2()));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress3(), is(legalEntityDetails.getAddress().getAddress3()));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress4(), is(legalEntityDetails.getAddress().getAddress4()));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress5(), is(legalEntityDetails.getAddress().getAddress5()));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getPostcode(), is(legalEntityDetails.getAddress().getPostcode()));
        assertThat(legalEntityDefendant.getOrganisation().getContact().getHome(), is(legalEntityDetails.getContactDetails().getHome()));
        assertThat(legalEntityDefendant.getOrganisation().getContact().getMobile(), is(legalEntityDetails.getContactDetails().getMobile()));
        assertThat(legalEntityDefendant.getOrganisation().getContact().getWork(), is(legalEntityDetails.getContactDetails().getBusiness()));
        assertThat(legalEntityDefendant.getOrganisation().getContact().getPrimaryEmail(), is(legalEntityDetails.getContactDetails().getEmail()));
        assertThat(legalEntityDefendant.getOrganisation().getContact().getSecondaryEmail(), is(legalEntityDetails.getContactDetails().getEmail2()));

    }

    @Test
    public void shouldConverterLegalEntityDetailsWithNullValue() {
        final LegalEntityDetails legalEntityDetails = null;
        final LegalEntityDefendant legalEntityDefendant = legalEntityDefendantConverter.getLegalEntityDefendant(legalEntityDetails);
        assertThat(legalEntityDefendant, nullValue());
    }


}
