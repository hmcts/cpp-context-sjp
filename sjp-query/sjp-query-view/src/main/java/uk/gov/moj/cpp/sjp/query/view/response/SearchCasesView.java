package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.List;

public class SearchCasesView {

    private final String searchTerm;

    private final List<SearchCasesHit> hits;

    public SearchCasesView(String searchTerm, List<SearchCasesHit> hits) {
        this.searchTerm = searchTerm;
        this.hits = hits;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public List<SearchCasesHit> getHits() {
        return hits;
    }

    @Override
    public String toString() {
        return "SearchCasesView{" +
                "searchTerm='" + searchTerm + '\'' +
                ", hits=" + hits +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
        	return true;
        }
        
        if (o == null || getClass() != o.getClass()){
        	return false;
        }

        SearchCasesView that = (SearchCasesView) o;

        if (searchTerm != null ? !searchTerm.equals(that.searchTerm) : that.searchTerm != null){
        	return false;
        }
        
        return hits != null ? hits.equals(that.hits) : that.hits == null;

    }

    @Override
    public int hashCode() {
        int result = searchTerm != null ? searchTerm.hashCode() : 0;
        result = 31 * result + (hits != null ? hits.hashCode() : 0);
        return result;
    }

}
