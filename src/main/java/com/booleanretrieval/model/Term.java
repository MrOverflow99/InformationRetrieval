package com.booleanretrieval.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Term {

    private final String term;
    private int documentFrequency;
    private final List<Posting> postingList;

    public Term(String term) {
        if (term == null || term.isBlank()) {
            throw new IllegalArgumentException("Il termine non puo' essere null o vuoto");
        }
        this.term = term;
        this.documentFrequency = 0;
        this.postingList = new ArrayList<>();
    }

    public void addPosting(Posting posting) {
        if (posting == null) {
            throw new IllegalArgumentException("Posting non puo' essere null");
        }
        postingList.add(posting);
        documentFrequency++;
    }

    public Posting getPosting(int docId) {
        for (Posting p : postingList) {
            if (p.getDocId() == docId) return p;
            if (p.getDocId () > docId) break;
        }
        return null;
    }

    public String getTerm() {
        return term;
    }

    public int getDocumentFrequency() {
        return documentFrequency;
    }

    public List<Posting> getPostingList() {
        return Collections.unmodifiableList(postingList);
    }

    public double computeIDF(int totalDocuments) {
        if (documentFrequency == 0) return 0.0;
        // IDF = log( N / df ) — formula classica TF-IDF
        return Math.log10((double) totalDocuments / documentFrequency);
    }

    @Override
    public String toString() {
        return "Term{'" + term + "', df=" + documentFrequency +
                ", postings=" + postingList.size() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Term other)) return false;
        return term.equals(other.term);
    }

    @Override
    public int hashCode() {
        return term.hashCode();
    }
}
