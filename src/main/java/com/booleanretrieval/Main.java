package com.booleanretrieval;
import com.booleanretrieval.ui.SearchApp;
import com.booleanretrieval.benchmark.Benchmark;
import com.booleanretrieval.ui.SearchApp;
import com.booleanretrieval.index.InvertedIndex;
import com.booleanretrieval.io.DocumentLoader;
import com.booleanretrieval.model.Document;
import com.booleanretrieval.model.Posting;
import com.booleanretrieval.search.QueryExecutor;
import com.booleanretrieval.ranking.TFIDFRanker;
import com.booleanretrieval.index.SkipList;
import com.booleanretrieval.index.KGramIndex;
import com.booleanretrieval.search.SpellingCorrector;
import java.util.Set;
import com.booleanretrieval.compression.PostingListCompressor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;



public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length > 0 && args[0].equals("--benchmark")) {
            Path datasetPath = Path.of(System.getProperty("user.home"),
                    "Desktop", "Dataset", "aclImdb");
            Benchmark bench = new Benchmark(datasetPath);
            bench.benchmarkIndexingTime();
            bench.benchmarkQueryTime();
            bench.benchmarkIntersectionMethods();
            bench.benchmarkCompression();
            bench.benchmarkHeapsLaw();
            return;
        }

        SearchApp.main(args);
    }
}

/*
public class Main {
    public static void main(String[] args) throws IOException {

        Path datasetPath = Path.of(System.getProperty("user.home"),
                "Desktop", "Dataset", "aclImdb");

        DocumentLoader loader = new DocumentLoader(datasetPath);
        System.out.println("Caricamento dataset...");
        List<Document> docs = loader.loadAll(5000);

        System.out.println("Indicizzazione in corso...");
        InvertedIndex index = new InvertedIndex();
        index.indexAll(docs);
        index.printStats();

        QueryExecutor executor = new QueryExecutor(index);

        System.out.println("\n=== Conjunctive Query (AND) ===");
        printResults("horror AND film", executor.conjunctiveQuery("horror film"), index);
        printResults("brilliant AND acting", executor.conjunctiveQuery("brilliant acting"), index);

        System.out.println("\n=== Optimized Conjunctive Query ===");
        // Confronto velocità AND vs AND ottimizzato
        long t1 = System.nanoTime();
        List<Integer> r1 = executor.conjunctiveQuery("good film horror");
        long t2 = System.nanoTime();
        List<Integer> r2 = executor.optimizedConjunctiveQuery("good film horror");
        long t3 = System.nanoTime();
        System.out.printf("AND normale:    %d risultati in %d µs%n", r1.size(), (t2-t1)/1000);
        System.out.printf("AND ottimizzato: %d risultati in %d µs%n", r2.size(), (t3-t2)/1000);

        System.out.println("\n=== Disjunctive Query (OR) ===");
        printResults("terrible OR awful", executor.disjunctiveQuery("terrible awful"), index);

        System.out.println("\n=== Phrase Query ===");
        printResults("\"special effects\"", executor.phraseQuery("special effects"), index);
        printResults("\"best film\"", executor.phraseQuery("best film"), index);
        printResults("\"waste of time\"", executor.phraseQuery("waste of time"), index);

        System.out.println("\n=== TF-IDF Ranking ===");
        TFIDFRanker ranker = new TFIDFRanker(index);

// Prendi i risultati dell'OR e rankali
        List<Integer> candidates = executor.disjunctiveQuery("horror film");
        List<TFIDFRanker.ScoredDocument> ranked = ranker.rank(candidates, "horror film");

        System.out.println("Top 5 documenti per 'horror film' (TF-IDF):");
        ranked.stream().limit(5).forEach(sd -> {
            Document doc = index.getDocument(sd.docId());
            String preview = doc.getContent().replaceAll("\\s+", " ")
                    .substring(0, Math.min(100, doc.getContent().length()));
            System.out.printf("  doc%-4d score=%.4f [%s] \"%s...\"%n",
                    sd.docId(), sd.score(), doc.getSentiment(), preview);
        });

// Test Skip List
        System.out.println("\n=== Skip List vs Standard Intersection ===");
        List<Posting> pl1 = index.getPostingList("film");
        List<Posting> pl2 = index.getPostingList("horror");

        SkipList sl1 = new SkipList(pl1);
        SkipList sl2 = new SkipList(pl2);

        long t5 = System.nanoTime();
        int[] skipResult = SkipList.intersect(sl1, sl2);
        long t4 = System.nanoTime();

        System.out.printf("Skip List intersection 'film'∩'horror': %d risultati in %d µs%n",
                skipResult.length, (t4-t5)/1000);
        System.out.printf("(Skip interval: √%d ≈ %d)%n", pl1.size(), sl1.getSkipInterval());

        System.out.println("\n=== WildCard Query ===");
        KGramIndex kgramIdx = index.getKgramIndex();
        String[] wildcards = {"hor*", "*ing", "bril*", "act*"};
        for (String pattern : wildcards) {
            Set<String> matches = kgramIdx.wildcardSearch(pattern);
            System.out.printf("'%s' → %d termini: %s%n",
                    pattern, matches.size(),
                    matches.stream().sorted().limit(8).toList());
        }

        System.out.println("\n=== Spelling Correction ===");
        SpellingCorrector corrector = new SpellingCorrector(kgramIdx, index.getVocabulary());
        String[] misspelled = {"horoor", "brlliant", "terribl", "amzing"};
        for (String word : misspelled) {
            List<String> suggestions = corrector.suggest(word, 3);
            System.out.printf("'%s' → suggerimenti: %s%n", word, suggestions);
        }
    }

    private static void printResults(String query, List<Integer> docIds, InvertedIndex index) {
        System.out.printf("Query: %-30s → %d risultati", query, docIds.size());
        if (!docIds.isEmpty()) {
            System.out.print(", es: ");
            docIds.stream().limit(2).forEach(id -> {
                Document doc = index.getDocument(id);
                // Mostra i primi 80 caratteri della recensione
                String preview = doc.getContent().replaceAll("\\s+", " ").substring(0, Math.min(80, doc.getContent().length()));
                System.out.printf("[doc%d: \"%s...\"] ", id, preview);
            });
        }
        System.out.println();
    }
}
 */