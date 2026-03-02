package com.booleanretrieval.preprocessing;

import java.util.List;
import java.util.Set;


public class StopWordFilter implements TextProcessor {


    //  Lista delle stop words inglesi più comuni.


    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "in", "on", "at", "to",
            "for", "of", "with", "by", "from", "is", "it", "its", "was",
            "are", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "could", "should", "may",
            "might", "shall", "can", "not", "no", "nor", "so", "yet",
            "both", "either", "neither", "as", "if", "then", "than",
            "that", "this", "these", "those", "i", "you", "he", "she",
            "we", "they", "me", "him", "her", "us", "them", "my", "your",
            "his", "our", "their", "what", "which", "who", "whom", "how",
            "when", "where", "why", "all", "each", "every", "any", "some",
            "such", "more", "most", "also", "just", "about", "up", "out",
            "into", "after", "before", "between", "through", "during",
            "there", "here", "very", "only", "own", "same", "other",
            "br", "http", "www", "com", "href", "gt", "lt", "amp", "quot",
            "one", "two", "three", "even", "ever", "always", "because",
            "though", "although", "however", "another", "anyone", "anything",
            "someone", "something", "everyone", "everything", "going", "getting"
    );

    @Override
    public List<String> process(List<String> tokens) {
        return tokens.stream()
                .filter(token -> !STOP_WORDS.contains(token))
                .toList();
    }

    // Utile per il report
    public int getStopWordCount() {
        return STOP_WORDS.size();
    }
}