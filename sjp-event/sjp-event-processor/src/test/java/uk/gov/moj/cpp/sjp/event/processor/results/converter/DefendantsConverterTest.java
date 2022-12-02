package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.json.schemas.domains.sjp.LegalEntityDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantsConverterTest {

    @InjectMocks
    DefendantsConverter defendantsConverter;
    @Mock
    OffencesConverter offencesConverter;
    @Mock
    PersonDefendantConverter personDefendantConverter;
    @Mock
    LegalEntityDefendantConverter legalEntityDefendantConverter;
    @Mock
    ReferenceDataService referenceDataService;
    @Mock
    ProsecutionCaseFileService prosecutionCaseFileService;

    @Test
    public void shouldConverterDefendantWithLegalEntityDefendant() {

        String companyName = "Company ltd";
        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withDefendant(uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant()
                        .withId(randomUUID())
                        .withLegalEntityDetails(LegalEntityDetails.legalEntityDetails()
                                .withLegalEntityName(companyName)
                                .build())
                        .build())
                .withId(randomUUID())
                .build();
        final DecisionAggregate resultsAggregate = mock(DecisionAggregate.class);
        final JsonObject sjpSessionPayload = createObjectBuilder()
                .add("startedAt", new StoppedClock(ZonedDateTime.now(UTC)).now().toString()).build();
        final Metadata metadata = metadataWithRandomUUID("sjp.event").build();


        when(prosecutionCaseFileService.getCaseFileDefendantDetails(any(), any())).thenReturn(Optional.empty());
        when(legalEntityDefendantConverter.getLegalEntityDefendant(any())).thenReturn(LegalEntityDefendant.legalEntityDefendant()
                .withOrganisation(Organisation.organisation()
                        .withName(companyName)
                        .build())
                .build());

        final List<Defendant> defendants = defendantsConverter.getDefendants(sjpSessionPayload, caseDetails, metadata, resultsAggregate);

        assertThat(defendants.size(), is(1));
        assertThat(defendants.get(0).getId(), is(caseDetails.getDefendant().getId()));
        assertThat(defendants.get(0).getLegalEntityDefendant().getOrganisation().getName(), is(caseDetails.getDefendant().getLegalEntityDetails().getLegalEntityName()));

    }

    @Test
    public void shouldConverterDefendantWithoutLegalEntityDefendant() {

        final CaseDetails caseDetails = CaseDetails.caseDetails()
                .withDefendant(uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant()
                        .withId(randomUUID())
                        .build())
                .withId(randomUUID())
                .build();
        final DecisionAggregate resultsAggregate = mock(DecisionAggregate.class);
        final JsonObject sjpSessionPayload = createObjectBuilder()
                .add("startedAt", new StoppedClock(ZonedDateTime.now(UTC)).now().toString()).build();
        final Metadata metadata = metadataWithRandomUUID("sjp.event").build();


        when(prosecutionCaseFileService.getCaseFileDefendantDetails(any(), any())).thenReturn(Optional.empty());
        when(legalEntityDefendantConverter.getLegalEntityDefendant(any())).thenReturn(null);

        final List<Defendant> defendants = defendantsConverter.getDefendants(sjpSessionPayload, caseDetails, metadata, resultsAggregate);

        assertThat(defendants.size(), is(1));
        assertThat(defendants.get(0).getId(), is(caseDetails.getDefendant().getId()));
        assertThat(defendants.get(0).getLegalEntityDefendant(), nullValue());

    }

}
