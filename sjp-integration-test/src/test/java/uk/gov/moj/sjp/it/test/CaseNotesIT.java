package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.NoteAuthor.noteAuthor;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.ADJOURNMENT;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.CASE;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.moj.sjp.it.helper.CaseNoteHelper.addCaseNote;
import static uk.gov.moj.sjp.it.helper.CaseNoteHelper.getCaseNotes;
import static uk.gov.moj.sjp.it.helper.CaseNoteHelper.pollForCaseNotes;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;

import uk.gov.justice.json.schemas.domains.sjp.NoteAuthor;
import uk.gov.moj.sjp.it.command.CreateCase;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class CaseNotesIT extends BaseIntegrationTest {

    public static final String TIMESTAMP_WITHOUT_ZONE_REGEX = "\\d{4}-[01]\\d-[0-3]\\dT[0-2]\\d:[0-5]\\d:[0-5]\\d(?:\\.\\d+)?Z?";
    private UUID caseId, decisionId;
    private NoteAuthor legalAdviser, courtAdmin, prosecutor;

    @Before
    public void setUp() {
        caseId = randomUUID();
        decisionId = randomUUID();

        legalAdviser = noteAuthor()
                .withUserId(randomUUID())
                .withFirstName("John")
                .withLastName("Smith")
                .build();

        courtAdmin = noteAuthor()
                .withUserId(randomUUID())
                .withFirstName("Trevor")
                .withLastName("Wall")
                .build();

        prosecutor = noteAuthor()
                .withUserId(randomUUID())
                .withFirstName("David")
                .withLastName("Brick")
                .build();

        stubForUserDetails(legalAdviser);
        stubForUserDetails(courtAdmin);
        stubForUserDetails(prosecutor);

        stubGroupForUser(legalAdviser.getUserId(), "Legal Advisers");
        stubGroupForUser(courtAdmin.getUserId(), "Court Administrators");
        stubGroupForUser(prosecutor.getUserId(), "SJP Prosecutors");
    }

    @Test
    public void shouldRestrictNotesInteractions() {
        final String note = "note";

        addCaseNote(caseId, prosecutor.getUserId(), note, CASE.toString(), FORBIDDEN);
        getCaseNotes(caseId, prosecutor.getUserId(), FORBIDDEN);

        addCaseNote(caseId, courtAdmin.getUserId(), note, CASE.toString(), ACCEPTED);
        getCaseNotes(caseId, courtAdmin.getUserId(), OK);

        addCaseNote(caseId, legalAdviser.getUserId(), note, CASE.toString(), ACCEPTED);
        getCaseNotes(caseId, legalAdviser.getUserId(), OK);
    }

    @Test
    public void shouldRejectInvalidNotes() {
        final String note = "note";

        addCaseNote(caseId, legalAdviser.getUserId(), note, "UNSUPPORTED", BAD_REQUEST);
        addCaseNote(caseId, legalAdviser.getUserId(), "", ADJOURNMENT.toString(), BAD_REQUEST);
        addCaseNote(caseId, legalAdviser.getUserId(), note, DECISION.toString(), BAD_REQUEST);
    }

    @Test
    public void shouldAddAndRetrieveCaseNotes() {

        createCase(caseId);

        final JsonObject initialNotes = getCaseNotes(caseId, legalAdviser.getUserId(), OK);
        assertThat(initialNotes.toString(), isJson(allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.notes.length()", is(0)))
        ));

        addCaseNote(caseId, legalAdviser.getUserId(), "note 1", ADJOURNMENT.toString(), decisionId, ACCEPTED);
        addCaseNote(caseId, legalAdviser.getUserId(), "note 2", DECISION.toString(), decisionId, ACCEPTED);
        addCaseNote(caseId, courtAdmin.getUserId(), "note 3", CASE.toString(), ACCEPTED);

        final JsonObject notes = pollForCaseNotes(caseId, withJsonPath("$.notes.length()", is(3)));

        assertThat(notes.toString(), isJson(allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.notes", allOf(
                        hasItem(
                                isJson(allOf(
                                        withJsonPath("$.noteId", notNullValue()),
                                        withJsonPath("$.addedAt", matchesPattern(TIMESTAMP_WITHOUT_ZONE_REGEX)),
                                        withJsonPath("$.noteText", is("note 1")),
                                        withJsonPath("$.noteType", is(ADJOURNMENT.toString())),
                                        withJsonPath("$.authorFirstName", is(legalAdviser.getFirstName())),
                                        withJsonPath("$.authorLastName", is(legalAdviser.getLastName())),
                                        withJsonPath("$.decisionId", is(decisionId.toString()))
                                ))
                        ),
                        hasItem(
                                isJson(allOf(
                                        withJsonPath("$.noteId", notNullValue()),
                                        withJsonPath("$.addedAt", notNullValue()),
                                        withJsonPath("$.noteText", is("note 2")),
                                        withJsonPath("$.noteType", is(DECISION.toString())),
                                        withJsonPath("$.authorFirstName", is(legalAdviser.getFirstName())),
                                        withJsonPath("$.authorLastName", is(legalAdviser.getLastName())),
                                        withJsonPath("$.decisionId", is(decisionId.toString()))
                                ))
                        ),
                        hasItem(
                                isJson(allOf(
                                        withJsonPath("$.noteId", notNullValue()),
                                        withJsonPath("$.addedAt", notNullValue()),
                                        withJsonPath("$.noteText", is("note 3")),
                                        withJsonPath("$.noteType", is(CASE.toString())),
                                        withJsonPath("$.authorFirstName", is(courtAdmin.getFirstName())),
                                        withJsonPath("$.authorLastName", is(courtAdmin.getLastName())),
                                        withoutJsonPath("$.decisionId")
                                ))
                        )
                ))
        )));
    }

    private static void createCase(final UUID caseId) {
        CreateCase.createCaseForPayloadBuilder(CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId));

        pollUntilCaseByIdIsOk(caseId);
    }

}
