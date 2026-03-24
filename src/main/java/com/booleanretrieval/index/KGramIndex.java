package com.booleanretrieval.index;

import java.util.*;
import java.io.Serializable;

public class KGramIndex implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int k;
    // kgram --> Set di termini del vocabolario che lo contengono
    private final Map<String, Set<String>> index;

    public KGramIndex(int k) {
        if (k < 2 || k > 5) throw new IllegalArgumentException("k deve essere tra 2 e 5");
        this.k     = k;
        this.index = new HashMap<>();
    }

    public void addTerm(String term) {
        for (String kgram : extractKGrams(term)) {
            index.computeIfAbsent(kgram, x -> new HashSet<>()).add(term);
        }
    }

    public List<String> extractKGrams(String term) {
        String augmented = "$" + term + "$";
        List<String> kgrams = new ArrayList<>();
        for (int i = 0; i <= augmented.length() - k; i++) {
            kgrams.add(augmented.substring(i, i + k));
        }
        return kgrams;
    }

    public Set<String> wildcardSearch(String pattern) {
        if (!pattern.contains("*")) {
            // Nessun wildcard — restituisce il termine se esiste
            return index.values().stream()
                    .flatMap(Set::stream)
                    .filter(t -> t.equals(pattern))
                    .collect(java.util.stream.Collectors.toSet());
        }

        String[] parts = pattern.split("\\*", -1);
        String prefix = parts[0];             // es: "ho" da "ho*or"
        String suffix = parts[parts.length - 1]; // es: "or" da "ho*or"

        // k-grammi dal prefisso e suffisso
        List<String> queryKGrams = new ArrayList<>();

        if (!prefix.isEmpty()) {
            String augPrefix = "$" + prefix;
            for (int i = 0; i <= augPrefix.length() - k; i++) {
                if (i + k <= augPrefix.length()) {
                    queryKGrams.add(augPrefix.substring(i, i + k));
                }
            }
        }

        if (!suffix.isEmpty()) {
            String augSuffix = suffix + "$";
            for (int i = 0; i <= augSuffix.length() - k; i++) {
                if (i + k <= augSuffix.length()) {
                    queryKGrams.add(augSuffix.substring(i, i + k));
                }
            }
        }

        if (queryKGrams.isEmpty()) {
            // Ssolo * — restituisce tutti i termini
            return index.values().stream()
                    .flatMap(Set::stream)
                    .collect(java.util.stream.Collectors.toSet());
        }

        // Intersezione: candidati che contengono TUTTI i k-grammi della query
        Set<String> candidates = null;
        for (String kgram : queryKGrams) {
            Set<String> terms = index.getOrDefault(kgram, Collections.emptySet());
            if (candidates == null) {
                candidates = new HashSet<>(terms);
            } else {
                candidates.retainAll(terms); // intersezione
            }
            if (candidates.isEmpty()) return Collections.emptySet();
        }

        if (candidates == null) return Collections.emptySet();

        String regex = patternToRegex(pattern);
        Set<String> results = new HashSet<>();
        for (String candidate : candidates) {
            if (candidate.matches(regex)) {
                results.add(candidate);
            }
        }
        return results;
    }

    private String patternToRegex(String pattern) {
        return pattern.replace(".", "\\.").replace("*", ".*");
    }

    public int size() { return index.size(); }
    public int getK() { return k; }

    public Set<String> getTermsForKgram(String kgram) {
        return Collections.unmodifiableSet(
                index.getOrDefault(kgram, Collections.emptySet())
        );
    }
}