package uk.gov.moj.cpp.sjp.domain.decision;

public interface OffenceDecisionVisitor {

    void visit(Dismiss dismiss);

    void visit(Withdraw withdraw);

    void visit(Adjourn adjourn);

    void visit(ReferForCourtHearing referForCourtHearing);

    void visit(Discharge discharge);

    void visit(FinancialPenalty financialPenalty);

    void visit(ReferredToOpenCourt referredToOpenCourt);

    void visit(ReferredForFutureSJPSession referredForFutureSJPSession);

    void visit(NoSeparatePenalty noSeparatePenalty);

    void visit(SetAside setAside);

    void visit(Oats oats);

}
