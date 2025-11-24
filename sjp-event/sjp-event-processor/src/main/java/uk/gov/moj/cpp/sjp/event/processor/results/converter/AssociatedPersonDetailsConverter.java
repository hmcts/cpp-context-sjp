package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AssociatedPersonDetailsConverter {

    @Inject
    private PersonDetailsConverter personDetailsConverter;

    public List<AssociatedPerson> getAssociatedPersons(final PersonalDetails personalDetails) {
        final List<AssociatedPerson> associatedPersons = new ArrayList<>();
        if (personalDetails != null) {
            final AssociatedPerson.Builder associatedPersonBuilder = AssociatedPerson.associatedPerson();
            final Person personDetails = personDetailsConverter.getPersonDetails(personalDetails, null);
            associatedPersonBuilder.withPerson(personDetails);//Mandatory
            associatedPersonBuilder.withRole("ParentGuardian");
            associatedPersons.add(associatedPersonBuilder.build());
            return associatedPersons;
        }
        return associatedPersons;
    }
}
