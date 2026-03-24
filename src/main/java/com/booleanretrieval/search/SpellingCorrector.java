package com.booleanretrieval.search;

import com.booleanretrieval.index.KGramIndex;

import java.util.*;

public class SpellingCorrector {


    private record ScoredCandidate(String term, int distance) {}
    private final KGramIndex kgramIndex;
    private final Set<String> vocabulary; // tutti i termini validi

    // Soglia massima edit distance per considerare un candidato
    private static final int MAX_EDIT_DISTANCE = 2;
    // Soglia minima overlap k-gram (jaccard similarity)
    private static final double MIN_KGRAM_OVERLAP = 0.2;

    public SpellingCorrector(KGramIndex kgramIndex, Set<String> vocabulary) {
        this.kgramIndex = kgramIndex;
        this.vocabulary = vocabulary;
    }

    /**
     * Suggerisce correzioni per una parola non trovata nel vocabolario.
     *
     * @param misspelled     la parola da correggere (già stemmatizzata/lowercased)
     * @param maxSuggestions quante correzioni restituire
     */
    public List<String> suggest(String misspelled, int maxSuggestions) {
        if (vocabulary.contains(misspelled)) {
            return List.of(misspelled); // già corretta
        }

        // Step 1: candidati via K-Gram overlap (filtro rapido)
        Set<String> candidates = getCandidatesViaKGram(misspelled);

        if (candidates.isEmpty()) {
            // Fallback: usa tutto il vocabolario (lento ma completo)
            candidates = new HashSet<>(vocabulary);
        }

        // Step 2: calcola edit distance per ogni candidato e ordina
        List<ScoredCandidate> scored = new ArrayList<>();
        for (String candidate : candidates) {
            int dist = editDistance(misspelled, candidate);
            if (dist <= MAX_EDIT_DISTANCE) {
                scored.add(new ScoredCandidate(candidate, dist));
            }
        }

        // Ordina: prima per edit distance (minore = migliore),
        // poi alfabeticamente per risultati deterministici
        scored.sort(Comparator.comparingInt(ScoredCandidate::distance)
                .thenComparing(ScoredCandidate::term));

        return scored.stream()
                .limit(maxSuggestions)
                .map(ScoredCandidate::term)
                .toList();
    }

    private Set<String> getCandidatesViaKGram(String word) {
        List<String> wordKGrams = kgramIndex.extractKGrams(word);
        Set<String> wordKGramSet = new HashSet<>(wordKGrams);

        // Per ogni k-gram della parola, recupera i termini candidati
        Map<String, Integer> overlapCount = new HashMap<>();
        for (String kgram : wordKGrams) {
            Set<String> terms = kgramIndex.wildcardSearch("*"); // non usiamo wildcard qui
            // accesso diretto ai termini per questo kgram tramite ricerca
        }

        Map<String, Integer> termOverlap = new HashMap<>();
        for (String kgram : wordKGrams) {
            // Recupera termini che contengono questo k-gram
            // Usiamo il fatto che ogni termine nel kgramIndex
            // è stato aggiunto con i suoi k-grammi
            Set<String> termsWithKgram = getTermsForKgram(kgram);
            for (String term : termsWithKgram) {
                termOverlap.merge(term, 1, Integer::sum);
            }
        }

        // Calcola Jaccard e filtra per soglia
        Set<String> candidates = new HashSet<>();
        for (Map.Entry<String, Integer> entry : termOverlap.entrySet()) {
            String term = entry.getKey();
            int intersectionSize = entry.getValue();

            List<String> termKGrams = kgramIndex.extractKGrams(term);
            int unionSize = wordKGramSet.size() + termKGrams.size() - intersectionSize;

            double jaccard = (double) intersectionSize / unionSize;
            if (jaccard >= MIN_KGRAM_OVERLAP) {
                candidates.add(term);
            }
        }
        return candidates;
    }

    private Set<String> getTermsForKgram(String kgram) {
        return kgramIndex.getTermsForKgram(kgram);
    }

    /*
     * Distanza di Levenshtein
     */
    public int editDistance(String s1, String s2) {
        int m = s1.length(), n = s2.length();

        if (m == 0) return n;
        if (n == 0) return m;

        // Ottimizzazione: s1 sia sempre la stringa più corta
        if (m > n) return editDistance(s2, s1);

        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];

        for (int i = 0; i <= m; i++) prev[i] = i;

        for (int j = 1; j <= n; j++) {
            curr[0] = j;
            for (int i = 1; i <= m; i++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    curr[i] = prev[i - 1];
                } else {
                    curr[i] = 1 + Math.min(prev[i - 1],
                            Math.min(prev[i],
                                    curr[i - 1]));
                }
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }

        return prev[m];
    }
}