package uk.gov.moj.cpp.sjp.query.view.response;


import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DefendantView {

    private final UUID id;
    private final UUID personId;
    private final List<OffenceView> offences;
    private final UUID caseId;
    private final Interpreter interpreter;
    private Integer numPreviousConvictions;

    public DefendantView(DefendantDetail defendant) {
        this.id = defendant.getId();
        this.personId = defendant.getPersonId();
        this.offences = constructDefendantChargeView(defendant);
        this.caseId = defendant.getCaseDetail().getId();

        if (defendant.getInterpreter() == null) {
            this.interpreter = new Interpreter();
        } else {
            this.interpreter = new Interpreter();
            this.interpreter.setNeeded(defendant.getInterpreter().getNeeded());
            this.interpreter.setLanguage(defendant.getInterpreter().getLanguage());
        }
        this.numPreviousConvictions = defendant.getNumPreviousConvictions();
    }

    public UUID getPersonId() {
        return personId;
    }

    public List<OffenceView> getOffences() {
        return offences;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getId() {
        return id;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    private static List<OffenceView> constructDefendantChargeView(DefendantDetail defendant) {
        final Set<OffenceDetail> offences = defendant.getOffences();
        if (offences == null) {
            return new ArrayList<>();
        } else {
            List<OffenceView> offenceViewList = new ArrayList<>();
            offences.forEach(offence -> offenceViewList.add(new OffenceView(offence)));
            offenceViewList.sort(Comparator.comparing(OffenceView::getOffenceSequenceNumber));
            return offenceViewList;
        }
    }

    public Integer getNumPreviousConvictions() {
        return numPreviousConvictions;
    }
}
