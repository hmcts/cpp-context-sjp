package uk.gov.moj.cpp.sjp.persistence.entity.view;


public class CaseCountByAgeView {

    private int age;
    private int count;

    public CaseCountByAgeView(int age, int count) {
        this.age = age;
        this.count = count;
    }

    public int getAge() {
        return age;
    }

    public int getCount() {
        return count;
    }
}
