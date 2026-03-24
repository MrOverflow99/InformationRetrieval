package com.booleanretrieval.index;

import com.booleanretrieval.model.Posting;

import java.util.List;


public class SkipList {

    private final int[] docIds;
    private final int[] skipPointers;   // skipPointers[i] = indice a cui saltare da i
    private final int[] skipTargets;    // skipTargets[i]  = docId a cui si arriva saltando
    private final int size;
    private final int skipInterval;     // ogni quanti elementi piazzare uno skip pointer

    public SkipList(List<Posting> postingList) {
        this.size = postingList.size();

        this.skipInterval = Math.max(1, (int) Math.sqrt(size));

        this.docIds       = new int[size];
        this.skipPointers = new int[size];
        this.skipTargets  = new int[size];

        // Popola gli array
        for (int i = 0; i < size; i++) {
            docIds[i] = postingList.get(i).getDocId();
            skipPointers[i] = -1; // -1 = nessuno skip pointer qui
            skipTargets[i]  = -1;
        }

        // Piazza gli skip pointers ogni skipInterval posizioni
        for (int i = 0; i + skipInterval < size; i += skipInterval) {
            skipPointers[i] = i + skipInterval;
            skipTargets[i]  = docIds[i + skipInterval];
        }
    }

    public static int[] intersect(SkipList sl1, SkipList sl2) {
        int[] result = new int[Math.min(sl1.size, sl2.size)];
        int resultSize = 0;
        int i = 0, j = 0;

        while (i < sl1.size && j < sl2.size) {
            int id1 = sl1.docIds[i];
            int id2 = sl2.docIds[j];

            if (id1 == id2) {
                result[resultSize++] = id1;
                i++; j++;
            } else if (id1 < id2) {
                if (sl1.skipPointers[i] != -1 && sl1.skipTargets[i] <= id2) {
                    i = sl1.skipPointers[i]; // SALTO!
                } else {
                    i++;
                }
            } else {
                // Stesso ragionamento per sl2
                if (sl2.skipPointers[j] != -1 && sl2.skipTargets[j] <= id1) {
                    j = sl2.skipPointers[j]; // SALTO!
                } else {
                    j++;
                }
            }
        }

        // Restituisce solo la parte riempita
        int[] trimmed = new int[resultSize];
        System.arraycopy(result, 0, trimmed, 0, resultSize);
        return trimmed;
    }

    public int getDocId(int index) { return docIds[index]; }
    public int size() { return size; }
    public int getSkipInterval() { return skipInterval; }
}