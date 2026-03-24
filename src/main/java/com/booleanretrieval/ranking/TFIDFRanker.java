package com.booleanretrieval.ranking;

import com.booleanretrieval.index.InvertedIndex;
import com.booleanretrieval.model.Posting;
import com.booleanretrieval.model.Term;
import com.booleanretrieval.preprocessing.PreprocessingPipeline;

import java.util.*;

public class TFIDFRanker {

    private final InvertedIndex index;
    private final PreprocessingPipeline pipeline;

    public TFIDFRanker(InvertedIndex index) {
        this.index    = index;
        this.pipeline = new PreprocessingPipeline();
    }

    public List<ScoredDocument> rank(List<Integer> candidateDocIds, String queryText) {
        List<String> queryTerms = pipeline.processQuery(queryText);
        if (queryTerms.isEmpty() || candidateDocIds.isEmpty()) {
            return Collections.emptyList();
        }

        int N = index.getTotalDocuments();
        List<ScoredDocument> scored = new ArrayList<>();

        for (int docId : candidateDocIds) {
            double score = 0.0;

            for (String term : queryTerms) {
                Term termObj = index.getTerm(term);
                if (termObj == null) continue;

                // Trova il Posting per questo documento
                Posting posting = termObj.getPosting(docId);
                if (posting == null) continue;

                // TF logaritmico: smorza l'effetto di termini molto ripetuti
                double tf  = Math.log10(1 + posting.getFrequency());

                // IDF: log(N/df) — più raro il termine, più alto il valore
                double idf = termObj.computeIDF(N);

                score += tf * idf;
            }

            if (score > 0) {
                scored.add(new ScoredDocument(docId, score));
            }
        }

        // Ordina per score decrescente, il documento più rilevante per primo
        scored.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
        return scored;
    }

    public record ScoredDocument(int docId, double score) {}
}