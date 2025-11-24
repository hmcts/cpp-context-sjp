package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.util.List;

public interface DefendantCaseSearcher {

    List<DefendantCase> searchDefendantCases(Envelope<?> envelope, DefendantDetail defendant);
}
