package com.booleanretrieval.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/*
*
* Spezza il testo grezzo in token (parole).
*
* Il Tokenizer è l'unico componente che lavora su String invece di List<String>
* perché è il primo step: trasforma il testo in token.
* Tutti gli altri processor lavorano su token già separati.
*
 */

public class Tokenizer {

    // Questa è una micro-ottimizzazione, se no ciao a ricompilare 475293752837 volte
    private static final Pattern NON_WORD = Pattern.compile("[\\W_]+");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern HTML_ENTITY = Pattern.compile("&[a-zA-Z]{2,6};");

    // Tokenizza una stringa di testo
    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return new ArrayList<>();

        // Pulizia HTML — PRIMA di tutto il resto
        text = HTML_TAG.matcher(text).replaceAll(" ");
        text = HTML_ENTITY.matcher(text).replaceAll(" ");

        String[] parts = NON_WORD.split(text.trim());
        List<String> tokens = new ArrayList<>(parts.length);
        for (String part : parts) {
            // length > 1 elimina "s" da apostrofi e singole lettere
            if (part.length() > 1 && !part.matches("\\d+")) {
                tokens.add(part);
            }
        }
        return tokens;
    }
}
