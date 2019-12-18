package uk.gov.moj.cpp.sjp.event.serialization;

import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.*;

import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.serialization.AbstractSerializationTest;
import uk.gov.moj.cpp.sjp.event.PleasSet;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matcher;

public class PleasSetSerializationTest extends AbstractSerializationTest<PleasSet> {

    private PleasSet buildPleaSetEvent(){
        UUID caseId = UUID.fromString("b939f47d-8801-41a8-91a8-5b937d3a914f");
        UUID defendantId = UUID.fromString("04a46cd8-793a-4b04-9e19-17f53891a44a");
        UUID offence1Id = UUID.fromString("80c32bd3-8842-4f83-a463-3d490c9c4467");
        UUID offence2Id = UUID.fromString("5a954a0f-e6f8-4640-adca-d212b75af640");
        DefendantCourtOptions courtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("ES", true), false);
        List<Plea> pleas = new LinkedList<>();
        pleas.add(new Plea(defendantId, offence1Id, GUILTY));
        pleas.add(new Plea(defendantId, offence2Id, NOT_GUILTY));
        return new PleasSet(caseId, courtOptions, pleas);
    }

    private String getSetPleasJson(){
        return "{\"caseId\":\"b939f47d-8801-41a8-91a8-5b937d3a914f\",\"defendantCourtOptions\":{\"interpreter\":{\"language\":\"ES\",\"needed\":true},\"welshHearing\":false},\"pleas\":[{\"defendantId\":\"04a46cd8-793a-4b04-9e19-17f53891a44a\",\"offenceId\":\"80c32bd3-8842-4f83-a463-3d490c9c4467\",\"pleaType\":\"GUILTY\"},{\"defendantId\":\"04a46cd8-793a-4b04-9e19-17f53891a44a\",\"offenceId\":\"5a954a0f-e6f8-4640-adca-d212b75af640\",\"pleaType\":\"NOT_GUILTY\"}]}";
    }

    @Override
    protected Map<PleasSet, Matcher<String>> getParams() {
        return ImmutableMap.of(buildPleaSetEvent(), equalTo(getSetPleasJson()));
    }
}
