package uk.gov.moj.cpp.sjp.query.view.response;


import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;

import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DefendantView {

    private final UUID id;
    private final List<OffenceView> offences;
    private final UUID caseId;
    private final Interpreter interpreter;
    private final Boolean speakWelsh;
    private final Integer numPreviousConvictions;
    private final PersonalDetailsView personalDetails;
    private final LegalEntityDetailsView legalEntityDetails;
    private final DisabilityNeeds disabilityNeeds;
    private final String asn;
    private final String pncIdentifier;
    private final String gobAccountNumber;
    private final UUID pcqId;


    public DefendantView(DefendantDetail defendant) {
        this.id = defendant.getId();
        this.personalDetails = nonNull(defendant.getPersonalDetails()) ? new PersonalDetailsView(defendant) : null;
        this.offences = constructDefendantChargeView(defendant);
        this.caseId = defendant.getCaseDetail().getId();
        this.interpreter = Interpreter.of(
                Optional.ofNullable(defendant.getInterpreter())
                        .map(InterpreterDetail::getLanguage)
                        .orElse(null));
        this.speakWelsh = defendant.getSpeakWelsh() != null ? defendant.getSpeakWelsh() : false;
        this.numPreviousConvictions = defendant.getNumPreviousConvictions();
        this.disabilityNeeds = disabilityNeedsOf(defendant.getDisabilityNeeds());
        this.asn = defendant.getAsn();
        this.pncIdentifier = defendant.getPncIdentifier();
        this.gobAccountNumber = defendant.getAccountNumber();
        this.legalEntityDetails = nonNull(defendant.getLegalEntityDetails()) ? new LegalEntityDetailsView(defendant) : null;
        this.pcqId = defendant.getPcqId();
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

    public PersonalDetailsView getPersonalDetails() {
        return personalDetails;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public Boolean getSpeakWelsh() {
        return speakWelsh;
    }

    public Integer getNumPreviousConvictions() {
        return numPreviousConvictions;
    }

    public DisabilityNeeds getDisabilityNeeds() {
        return disabilityNeeds;
    }

    public String getAsn() {
        return asn;
    }

    public String getPncIdentifier() {
        return pncIdentifier;
    }

    public String getGobAccountNumber() {
        return gobAccountNumber;
    }

    public LegalEntityDetailsView getLegalEntityDetails() {
        return legalEntityDetails;
    }

    public UUID getPcqId() { return pcqId; }

    private static List<OffenceView> constructDefendantChargeView(final DefendantDetail defendant) {
        return defendant.getOffences().stream()
                .map(OffenceView::new)
                .sorted(comparing(OffenceView::getOffenceSequenceNumber))
                .collect(toList());
    }
}
