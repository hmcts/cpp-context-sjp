package uk.gov.moj.cpp.sjp.query.view.service;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.query.view.util.JsonHelper.readJsonFromFile;
import static uk.gov.moj.cpp.sjp.query.view.util.builders.CaseDetailEntityBuilder.withDefaults;
import static java.util.Arrays.asList;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class ProsecutionCaseServiceTest {

    @InjectMocks
    private ProsecutionCaseService prosecutionCaseService;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    @Mock
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Mock
    private EmployerService employerService;


    @Test
    public void shouldFindProsecutionCaseWhenDefendantIsPerson() {

        final CaseDetail caseDetail = withDefaults()
                .withDefendantDetail(createPersonDefendantDetail())
                .build();
        final JsonObject caseFileDetails = readJsonFromFile("data/prosecutioncasefile.query.case.json");
        final JsonObject nationality = readJsonFromFile("data/referencedata.query.nationality.json");
        final JsonObject prosecutor = readJsonFromFile("data/referencedata.query.prosecutor.json");

        when(caseRepository.findBy(caseDetail.getId())).thenReturn(caseDetail);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantId(caseDetail.getId(), caseDetail.getDefendant().getId())).thenReturn(singletonList(mock(OnlinePleaDetail.class)));
        when(prosecutionCaseFileService.getCaseFileDetails(caseDetail.getId())).thenReturn(of(caseFileDetails));
        when(referenceDataService.getProsecutor(any())).thenReturn(prosecutor);
        when(referenceDataService.getNationality(any())).thenReturn(of(nationality));
        when(employerService.getEmployer(caseDetail.getDefendant().getId())).thenReturn(of(mock(Employer.class)));

        final ProsecutionCase prosecutionCase = prosecutionCaseService.findProsecutionCase(caseDetail.getId());

        assertThat(prosecutionCase.getDefendants().get(0).getPersonDefendant().getPersonDetails().getFirstName(),is("John"));
        assertThat(prosecutionCase.getDefendants().get(0).getPersonDefendant().getPersonDetails().getLastName(),is("Smith"));
        assertThat(prosecutionCase.getDefendants().get(0).getPersonDefendant().getPersonDetails().getTitle(),is("MR"));
        assertThat(prosecutionCase.getDefendants().get(0).getPersonDefendant().getPersonDetails().getAddress().getPostcode(),is("WC1E 1EE"));

    }

    @Test
    public void shouldFindProsecutionCaseWhenDefendantIsCompany() {

        final CaseDetail caseDetail = withDefaults()
                .withDefendantDetail(createLegalEntityDefendantDetails())
                .build();
        final JsonObject caseFileDetails = readJsonFromFile("data/prosecutioncasefile.query.case.json");
        final JsonObject nationality = readJsonFromFile("data/referencedata.query.nationality.json");
        final JsonObject prosecutor = readJsonFromFile("data/referencedata.query.prosecutor.json");

        when(caseRepository.findBy(caseDetail.getId())).thenReturn(caseDetail);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantId(caseDetail.getId(), caseDetail.getDefendant().getId())).thenReturn(singletonList(mock(OnlinePleaDetail.class)));
        when(prosecutionCaseFileService.getCaseFileDetails(caseDetail.getId())).thenReturn(of(caseFileDetails));
        when(referenceDataService.getProsecutor(any())).thenReturn(prosecutor);
        when(referenceDataService.getNationality(any())).thenReturn(of(nationality));
        when(employerService.getEmployer(caseDetail.getDefendant().getId())).thenReturn(of(mock(Employer.class)));

        final ProsecutionCase prosecutionCase = prosecutionCaseService.findProsecutionCase(caseDetail.getId());

        assertThat(prosecutionCase.getDefendants().get(0).getLegalEntityDefendant().getOrganisation().getName(),is("Legal Entity Name"));
        assertThat(prosecutionCase.getDefendants().get(0).getLegalEntityDefendant().getOrganisation().getAddress().getPostcode(),is("BH8 8RW"));

    }

    @Test
    public void shouldPopulateDvlaOffenceCode() {
        final DefendantDetail defendantDetail = createPersonDefendantDetail();
        defendantDetail.setOffences(asList(OffenceDetail.builder()
                .setId(UUID.randomUUID())
                .setSequenceNumber(1)
                .setCode("CA03010")
                .build()));

        final CaseDetail caseDetail = withDefaults()
                .withDefendantDetail(defendantDetail)
                .build();

        final JsonObject caseFileDetails = readJsonFromFile("data/prosecutioncasefile.query.case.json");
        final JsonObject nationality = readJsonFromFile("data/referencedata.query.nationality.json");
        final JsonObject prosecutor = readJsonFromFile("data/referencedata.query.prosecutor.json");
        final JsonObject offenceData = readJsonFromFile("data/referencedataoffences.query.offences-list.json");
        final Map<String, JsonObject> offenceDefinitionByOffenceCode = new HashMap<>();
        offenceDefinitionByOffenceCode.put("CA03010", offenceData.getJsonArray("offences").getJsonObject(0));

        when(caseRepository.findBy(caseDetail.getId())).thenReturn(caseDetail);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantId(caseDetail.getId(), caseDetail.getDefendant().getId())).thenReturn(singletonList(mock(OnlinePleaDetail.class)));
        when(prosecutionCaseFileService.getCaseFileDetails(caseDetail.getId())).thenReturn(of(caseFileDetails));
        when(referenceDataService.getProsecutor(any())).thenReturn(prosecutor);
        when(referenceDataService.getNationality(any())).thenReturn(of(nationality));
        when(employerService.getEmployer(caseDetail.getDefendant().getId())).thenReturn(of(mock(Employer.class)));
        when(referenceDataOffencesService.getOffenceDefinitionsByOffenceCode(new HashSet<String>(asList("CA03010")), now())).thenReturn(offenceDefinitionByOffenceCode);
        when(referenceDataService.getOffenceData("CA03010")).thenReturn(of(offenceData.getJsonArray("offences").getJsonObject(0)));

        final ProsecutionCase prosecutionCase = prosecutionCaseService.findProsecutionCase(caseDetail.getId());

        assertNotNull(prosecutionCase.getDefendants().get(0).getOffences());
        assertThat(prosecutionCase.getDefendants().get(0).getOffences().size(), is(1));
        assertThat(prosecutionCase.getDefendants().get(0).getOffences().get(0).getDvlaOffenceCode(), is("DC10"));
    }

    @Test
    public void shouldNotPopulateDvlaOffenceCodeIfNotAvailable() {
        final DefendantDetail defendantDetail = createPersonDefendantDetail();
        defendantDetail.setOffences(asList(OffenceDetail.builder()
                .setId(UUID.randomUUID())
                .setSequenceNumber(1)
                .setCode("CA03011")
                .build()));

        final CaseDetail caseDetail = withDefaults()
                .withDefendantDetail(defendantDetail)
                .build();

        final JsonObject caseFileDetails = readJsonFromFile("data/prosecutioncasefile.query.case.json");
        final JsonObject nationality = readJsonFromFile("data/referencedata.query.nationality.json");
        final JsonObject prosecutor = readJsonFromFile("data/referencedata.query.prosecutor.json");
        final JsonObject offenceData = readJsonFromFile("data/referencedataoffences.query.offences-list.json");
        final Map<String, JsonObject> offenceDefinitionByOffenceCode = new HashMap<>();
        offenceDefinitionByOffenceCode.put("CA03011", offenceData.getJsonArray("offences").getJsonObject(1));

        when(caseRepository.findBy(caseDetail.getId())).thenReturn(caseDetail);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantId(caseDetail.getId(), caseDetail.getDefendant().getId())).thenReturn(singletonList(mock(OnlinePleaDetail.class)));
        when(prosecutionCaseFileService.getCaseFileDetails(caseDetail.getId())).thenReturn(of(caseFileDetails));
        when(referenceDataService.getProsecutor(any())).thenReturn(prosecutor);
        when(referenceDataService.getNationality(any())).thenReturn(of(nationality));
        when(employerService.getEmployer(caseDetail.getDefendant().getId())).thenReturn(of(mock(Employer.class)));
        when(referenceDataOffencesService.getOffenceDefinitionsByOffenceCode(new HashSet<String>(asList("CA03011")), now())).thenReturn(offenceDefinitionByOffenceCode);
        when(referenceDataService.getOffenceData("CA03011")).thenReturn(of(offenceData.getJsonArray("offences").getJsonObject(1)));

        final ProsecutionCase prosecutionCase = prosecutionCaseService.findProsecutionCase(caseDetail.getId());

        assertNotNull(prosecutionCase.getDefendants().get(0).getOffences());
        assertThat(prosecutionCase.getDefendants().get(0).getOffences().size(), is(1));
        assertNull(prosecutionCase.getDefendants().get(0).getOffences().get(0).getDvlaOffenceCode());
    }

    private DefendantDetail createPersonDefendantDetail() {
        final DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID());

        final PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setTitle("Mr");
        personalDetails.setFirstName("John");
        personalDetails.setLastName("Smith");
        personalDetails.setDateOfBirth(LocalDate.parse("1980-10-12"));
        defendantDetail.setAddress(new Address("2 Kings Avenue", "", "", "", "", "WC1E 1EE"));
        defendantDetail.setPersonalDetails(personalDetails);

        return defendantDetail;
    }

    private DefendantDetail createLegalEntityDefendantDetails() {
        final DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID());

        final LegalEntityDetails legalEntityDetails = new LegalEntityDetails();
        legalEntityDetails.setLegalEntityName("Legal Entity Name");
        defendantDetail.setAddress(new Address("2 Kings Avenue", "", "", "", "", "BH8 8RW"));
        defendantDetail.setLegalEntityDetails(legalEntityDetails);
        return defendantDetail;
    }

}
