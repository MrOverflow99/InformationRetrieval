package com.booleanretrieval.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.Serializable;

public final class Posting implements Comparable<Posting>, Serializable {

    private static final long serialVersionUID = 1L;
    private final int docId;
    private final List<Integer> positions;

    public Posting(int docId) {
        this.docId = docId;
        this.positions = new ArrayList<>();
    }

    public void addPosition(int position) {
        positions.add(position);
    }

    public int getDocId() {
        return docId;
    }

    public List<Integer> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public int getFrequency() {
        return positions.size();
    }

    @Override
    public int compareTo(Posting other) {
        return Integer.compare(this.docId, other.docId);
    }

    @Override
    public String toString() {
        return "Posting {docId=" + docId + ", freq="  + getFrequency() + ", positions=" + positions + '}';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Posting other)) return false;
        return docId == other.docId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(docId);
    }

}
