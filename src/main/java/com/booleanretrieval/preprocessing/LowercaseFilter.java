package com.booleanretrieval.preprocessing;

import java.util.List;

/*
*
*   Niente di pazzesco, converto tutto in minuscolo
*
*/

public class LowercaseFilter implements TextProcessor {

    @Override
    public List<String> process(List<String> tokens) {
        return tokens.stream()
                .map(String::toLowerCase)
                .toList();
    }
}