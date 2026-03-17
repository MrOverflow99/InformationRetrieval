package com.booleanretrieval.preprocessing;

import java.util.List;
import java.util.Set;

/*
 *
 * Compone tutti i processor in una pipeline ordinata.
 * Facade
 *
 */

public class PreprocessingPipeline {

    private final Tokenizer tokenizer;
    private final LowercaseFilter lowercaseFilter;
    private final StopWordFilter stopWordFilter;
    private final PorterStemmer stemmer;

    private static final Set<String> POST_STEM_STOPWORDS = Set.of(
            "on", "go", "us", "wa", "ha", "doe", "becaus", "alway",
            "anoth", "ani", "everyon", "someon", "sometim", "everybodi"
    );

    public PreprocessingPipeline() {
        this.tokenizer = new Tokenizer();
        this.lowercaseFilter = new LowercaseFilter();
        this.stopWordFilter = new StopWordFilter();
        this.stemmer = new PorterStemmer();
    }

    /*
     * Ecco la magia, ringrazio il socio Alessandro Daniele R per il suggerimento.
     * Processa il testo grezzo e restituisce i token normalizzati.
     * 1. Tokenize → split in parole
     * 2. Lowercase → tutto minuscolo (prima dello stopword filter!)
     * 3. StopWords → rimuovi parole inutili
     * 4. Stem → riduci alla radice
     */
    public List<String> process(String rawText) {
        List<String> tokens = tokenizer.tokenize(rawText);
        tokens = lowercaseFilter.process(tokens);
        tokens = stopWordFilter.process(tokens);
        tokens = stemmer.process(tokens);
        tokens = postStemFilter(tokens);
        return tokens;
    }

    /* Processa solo per la query — stessa pipeline, stesso trattamento */
    public List<String> processQuery(String query) {
        return process(query);
    }

    private List<String> postStemFilter(List<String> tokens) {
        return tokens.stream()
                .filter(t -> !POST_STEM_STOPWORDS.contains(t))
                .filter(t -> t.length() > 1) // sicurezza extra: niente token singoli
                .toList();
    }
}