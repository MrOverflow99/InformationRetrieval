package com.booleanretrieval.search;

import com.booleanretrieval.index.InvertedIndex;
import com.booleanretrieval.model.Posting;
import com.booleanretrieval.preprocessing.PreprocessingPipeline;

import java.util.*;

public class QueryExecutor {

    private final InvertedIndex index;
    private final PreprocessingPipeline pipeline;

    public QueryExecutor(InvertedIndex index) {
        this.index = index;
        this.pipeline = new PreprocessingPipeline();
    }

    //CONJCTIVE QUERY - AND

    public List<Integer> conjunctiveQuery(String queryText) {
        List<String> terms = pipeline.processQuery(queryText);
        if (terms.isEmpty()) return Collections.emptyList();

        // Recupera tutte le posting list
        List<List<Posting>> postingLists = new ArrayList<>();
        for (String term : terms) {
            List<Posting> pl = index.getPostingList(term);
            if (pl.isEmpty()) return Collections.emptyList(); // early exit: un termine assente = nessun risultato
            postingLists.add(pl);
        }

        // Inizia dall'intersezione delle prime due, poi continua
        List<Posting> result = postingLists.get(0);
        for (int i = 1; i < postingLists.size(); i++) {
            result = intersect(result, postingLists.get(i));
            if (result.isEmpty()) return Collections.emptyList(); // early exit
        }

        return result.stream().map(Posting::getDocId).toList();
    }

    private List<Posting> intersect(List<Posting> p1, List<Posting> p2) {
        List<Posting> result = new ArrayList<>();
        int i = 0, j = 0;

        while (i < p1.size() && j < p2.size()) {
            int id1 = p1.get(i).getDocId();
            int id2 = p2.get(j).getDocId();

            if (id1 == id2) {
                result.add(p1.get(i));
                i++; j++;
            } else if (id1 < id2) {
                i++;
            } else {
                j++;
            }
        }
        return result;
    }

    // OPTIMIZED CONJUNCTIVE QUERY

    public List<Integer> optimizedConjunctiveQuery(String queryText) {
        List<String> terms = pipeline.processQuery(queryText);
        if (terms.isEmpty()) return Collections.emptyList();

        List<List<Posting>> postingLists = new ArrayList<>();
        for (String term : terms) {
            List<Posting> pl = index.getPostingList(term);
            if (pl.isEmpty()) return Collections.emptyList();
            postingLists.add(new ArrayList<>(pl));
        }

        //ordina per lunghezza crescente
        postingLists.sort(Comparator.comparingInt(List::size));

        List<Posting> result = postingLists.get(0);
        for (int i = 1; i < postingLists.size(); i++) {
            result = intersect(result, postingLists.get(i));
            if (result.isEmpty()) return Collections.emptyList();
        }

        return result.stream().map(Posting::getDocId).toList();
    }


    // DISJUNCTIVE QUERY (OR)

    public List<Integer> disjunctiveQuery(String queryText) {
        List<String> terms = pipeline.processQuery(queryText);
        if (terms.isEmpty()) return Collections.emptyList();

        List<List<Posting>> postingLists = new ArrayList<>();
        for (String term : terms) {
            List<Posting> pl = index.getPostingList(term);
            if (!pl.isEmpty()) postingLists.add(pl);
            // Nota: nell'OR non facciamo early exit se un termine manca
        }

        if (postingLists.isEmpty()) return Collections.emptyList();

        List<Posting> result = postingLists.get(0);
        for (int i = 1; i < postingLists.size(); i++) {
            result = union(result, postingLists.get(i));
        }

        return result.stream().map(Posting::getDocId).toList();
    }

    private List<Posting> union(List<Posting> p1, List<Posting> p2) {
        List<Posting> result = new ArrayList<>();
        int i = 0, j = 0;

        while (i < p1.size() && j < p2.size()) {
            int id1 = p1.get(i).getDocId();
            int id2 = p2.get(j).getDocId();

            if (id1 == id2) {
                result.add(p1.get(i));
                i++; j++;
            } else if (id1 < id2) {
                result.add(p1.get(i++));
            } else {
                result.add(p2.get(j++));
            }
        }
        // Aggiungi gli elementi rimanenti
        while (i < p1.size()) result.add(p1.get(i++));
        while (j < p2.size()) result.add(p2.get(j++));

        return result;
    }

    // ================================================================
    // PHRASE QUERY
    // ================================================================

    public List<Integer> phraseQuery(String queryText) {
        List<String> terms = pipeline.processQuery(queryText);
        if (terms.size() < 2) {
            // Phrase query con un solo termine = ricerca normale
            return terms.isEmpty() ? Collections.emptyList()
                    : conjunctiveQuery(queryText);
        }

        // Step 1: AND per trovare i candidati
        List<List<Posting>> postingLists = new ArrayList<>();
        for (String term : terms) {
            List<Posting> pl = index.getPostingList(term);
            if (pl.isEmpty()) return Collections.emptyList();
            postingLists.add(pl);
        }

        // Mappa docId → Posting per accesso O(1) durante la verifica posizionale
        // La costruiamo per ogni termine
        List<Map<Integer, Posting>> postingMaps = new ArrayList<>();
        for (List<Posting> pl : postingLists) {
            Map<Integer, Posting> map = new HashMap<>();
            for (Posting p : pl) map.put(p.getDocId(), p);
            postingMaps.add(map);
        }

        // Step 2: candidati = docId presenti in TUTTE le posting list
        Set<Integer> candidates = new HashSet<>(postingMaps.get(0).keySet());
        for (int i = 1; i < postingMaps.size(); i++) {
            candidates.retainAll(postingMaps.get(i).keySet());
        }

        // Step 3: verifica posizionale
        List<Integer> results = new ArrayList<>();
        for (int docId : candidates) {
            if (hasConsecutivePositions(docId, terms, postingMaps)) {
                results.add(docId);
            }
        }

        Collections.sort(results);
        return results;
    }

    private boolean hasConsecutivePositions(int docId, List<String> terms,
                                            List<Map<Integer, Posting>> postingMaps) {
        // Posizioni del primo termine
        List<Integer> firstPositions = postingMaps.get(0).get(docId).getPositions();

        for (int startPos : firstPositions) {
            boolean phraseFound = true;

            for (int termIdx = 1; termIdx < terms.size(); termIdx++) {
                List<Integer> positions = postingMaps.get(termIdx).get(docId).getPositions();
                int expectedPos = startPos + termIdx;

                // Ricerca binaria: le posizioni sono ordinate per costruzione
                if (!containsPosition(positions, expectedPos)) {
                    phraseFound = false;
                    break;
                }
            }

            if (phraseFound) return true;
        }
        return false;
    }

    private boolean containsPosition(List<Integer> positions, int target) {
        int lo = 0, hi = positions.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1; // >>> evita overflow per indici molto grandi
            int val = positions.get(mid);
            if (val == target) return true;
            else if (val < target) lo = mid + 1;
            else hi = mid - 1;
        }
        return false;
    }
}
