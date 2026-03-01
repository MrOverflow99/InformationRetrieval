package com.booleanretrieval.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
*
*    STRUTTURA IN MEMORIA:
*    "java" -> [Posting(1, [3,15,42]), Posting(5, [7,8]), Posting(12, [1])]
*             docId=1               docId=5             docId=12
*             appare in pos 3,15,42 appare in pos 7,8   appare in pos 1
*
*/

public final class Posting implements Comparable<Posting> {

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

    /*
     * PERCHÉ Comparable<Posting>?
     * Le posting list DEVONO essere ordinate per docId — è il requisito
     * fondamentale per fare l'intersezione efficiente (merge-based intersection).
     * Implementando Comparable diciamo: "l'ordinamento naturale di Posting
     * è per docId crescente".
     */

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
