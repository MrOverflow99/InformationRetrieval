package com.booleanretrieval.benchmark;

import com.booleanretrieval.index.InvertedIndex;
import com.booleanretrieval.index.SkipList;
import com.booleanretrieval.io.DocumentLoader;
import com.booleanretrieval.model.Document;
import com.booleanretrieval.model.Posting;
import com.booleanretrieval.search.QueryExecutor;
import com.booleanretrieval.compression.PostingListCompressor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class Benchmark {

    private final Path datasetPath;

    public Benchmark(Path datasetPath) {
        this.datasetPath = datasetPath;
    }

    /**
     * Benchmark 1: tempo di indicizzazione al variare del dataset.
     * Produce i dati per il grafico Indexing Time vs Dataset Size.
     */
    public void benchmarkIndexingTime() throws IOException {
        System.out.println("\n=== BENCHMARK 1: Indexing Time vs Dataset Size ===");
        System.out.printf("%-12s %-15s %-15s %-15s %-15s%n",
                "Documents", "Index Time(ms)", "Terms", "Tokens", "Tokens/Doc");
        System.out.println("-".repeat(75));

        int[] sizes = {5000, 10000, 20000, 30000, 40000, 50000};
        DocumentLoader loader = new DocumentLoader(datasetPath);
        List<Document> allDocs = loader.loadAll();

        for (int size : sizes) {
            List<Document> docs = allDocs.subList(0, Math.min(size, allDocs.size()));

            long start = System.currentTimeMillis();
            InvertedIndex index = new InvertedIndex();
            index.indexAll(docs);
            long elapsed = System.currentTimeMillis() - start;

            System.out.printf("%-12d %-15d %-15d %-15d %-15.1f%n",
                    size, elapsed,
                    index.getVocabularySize(),
                    index.getTotalTokensIndexed(),
                    (double) index.getTotalTokensIndexed() / size);
        }
    }

    /**
     * Benchmark 2: tempo delle query al variare della dimensione del dataset.
     * Produce i dati per Query Time vs Dataset Size.
     */
    public void benchmarkQueryTime() throws IOException {
        System.out.println("\n=== BENCHMARK 2: Query Time vs Dataset Size ===");

        String[] queries = {"film", "horror film", "best film", "special effects"};
        int[] sizes = {5000, 10000, 20000, 30000, 40000, 50000};

        DocumentLoader loader = new DocumentLoader(datasetPath);
        List<Document> allDocs = loader.loadAll();

        System.out.printf("%-12s", "Size");
        for (String q : queries) System.out.printf(" %-20s", "'" + q + "'(µs)");
        System.out.println();
        System.out.println("-".repeat(12 + queries.length * 21));

        for (int size : sizes) {
            List<Document> docs = allDocs.subList(0, Math.min(size, allDocs.size()));
            InvertedIndex index = new InvertedIndex();
            index.indexAll(docs);
            QueryExecutor executor = new QueryExecutor(index);

            System.out.printf("%-12d", size);
            for (String query : queries) {
                // Warmup
                executor.conjunctiveQuery(query);

                // Misura media su 10 esecuzioni
                long total = 0;
                int runs = 10;
                for (int i = 0; i < runs; i++) {
                    long t = System.nanoTime();
                    executor.conjunctiveQuery(query);
                    total += System.nanoTime() - t;
                }
                System.out.printf(" %-20d", (total / runs) / 1000);
            }
            System.out.println();
        }
    }

    /**
     * Benchmark 3: AND standard vs AND ottimizzato vs Skip List.
     * Al variare del numero di termini nella query.
     */
    public void benchmarkIntersectionMethods() throws IOException {
        System.out.println("\n=== BENCHMARK 3: Intersection Methods Comparison ===");
        System.out.printf("%-25s %-15s %-15s %-15s %-12s%n",
                "Query", "AND(µs)", "AND_OPT(µs)", "SkipList(µs)", "Results");
        System.out.println("-".repeat(85));

        DocumentLoader loader = new DocumentLoader(datasetPath);
        List<Document> allDocs = loader.loadAll();
        InvertedIndex index = new InvertedIndex();
        index.indexAll(allDocs);
        QueryExecutor executor = new QueryExecutor(index);

        String[][] queries = {
                {"film"},
                {"horror", "film"},
                {"good", "horror", "film"},
                {"great", "horror", "film", "act"},
                {"brilliant", "act", "horror", "film", "stori"}
        };

        for (String[] terms : queries) {
            String query = String.join(" ", terms);
            int runs = 20;

            // AND standard
            long t1 = 0;
            List<Integer> r1 = null;
            for (int i = 0; i < runs; i++) {
                long t = System.nanoTime();
                r1 = executor.conjunctiveQuery(query);
                t1 += System.nanoTime() - t;
            }

            // AND ottimizzato
            long t2 = 0;
            for (int i = 0; i < runs; i++) {
                long t = System.nanoTime();
                executor.optimizedConjunctiveQuery(query);
                t2 += System.nanoTime() - t;
            }

            // Skip List — costruiamo le skip list per i primi due termini
            List<Posting> pl1 = index.getPostingList(terms[0]);
            List<Posting> pl2 = terms.length > 1 ?
                    index.getPostingList(terms[1]) : pl1;

            long t3 = 0;
            for (int i = 0; i < runs; i++) {
                SkipList sl1 = new SkipList(pl1);
                SkipList sl2 = new SkipList(pl2);
                long t = System.nanoTime();
                SkipList.intersect(sl1, sl2);
                t3 += System.nanoTime() - t;
            }

            System.out.printf("%-25s %-15d %-15d %-15d %-12d%n",
                    query,
                    (t1 / runs) / 1000,
                    (t2 / runs) / 1000,
                    (t3 / runs) / 1000,
                    r1 != null ? r1.size() : 0);
        }
    }

    /**
     * Benchmark 4: compressione al variare della document frequency.
     * Mostra come il risparmio varia con termini rari vs frequenti.
     */
    public void benchmarkCompression() throws IOException {
        System.out.println("\n=== BENCHMARK 4: Compression vs Document Frequency ===");
        System.out.printf("%-15s %-10s %-15s %-15s %-12s%n",
                "Term", "DF", "Original(B)", "Compressed(B)", "Saving(%)");
        System.out.println("-".repeat(70));

        DocumentLoader loader = new DocumentLoader(datasetPath);
        List<Document> allDocs = loader.loadAll();
        InvertedIndex index = new InvertedIndex();
        index.indexAll(allDocs);
        PostingListCompressor compressor = new PostingListCompressor();

        // Seleziona termini rappresentativi a diverse frequenze
        String[] terms = {
                "film",        // altissima freq
                "horror",      // alta freq
                "titanic",     // media freq
                "brilliant",   // medio-bassa
                "zorro",       // bassa freq
        };

        for (String term : terms) {
            List<Posting> pl = index.getPostingList(term);
            if (pl.isEmpty()) continue;
            PostingListCompressor.CompressionStats stats = compressor.computeStats(pl);
            System.out.printf("%-15s %-10d %-15d %-15d %-12.1f%n",
                    term, pl.size(),
                    stats.originalBytes(),
                    stats.compressedBytes(),
                    stats.savedPercent());
        }

        // Statistiche globali
        System.out.println("\nGlobal compression over full vocabulary sample (1000 terms):");
        long totalOrig = 0, totalComp = 0;
        for (String t : index.getSampleTerms(1000)) {
            List<Posting> pl = index.getPostingListDirect(t);
            if (pl.isEmpty()) continue;
            PostingListCompressor.CompressionStats s = compressor.computeStats(pl);
            totalOrig += s.originalBytes();
            totalComp += s.compressedBytes();
        }
        System.out.printf("Total original: %d bytes | Compressed: %d bytes | Saving: %.1f%%%n",
                totalOrig, totalComp, (1.0 - (double)totalComp/totalOrig)*100);
    }

    /**
     * Benchmark 5: vocabolario e statistiche linguistiche.
     */
    public void benchmarkHeapsLaw() throws IOException {
        System.out.println("\n=== BENCHMARK 5: Heaps' Law — Vocabulary Growth ===");
        System.out.printf("%-12s %-15s %-20s%n",
                "Tokens", "Unique Terms", "Heaps ratio (M/T^beta)");
        System.out.println("-".repeat(50));

        DocumentLoader loader = new DocumentLoader(datasetPath);
        List<Document> allDocs = loader.loadAll();

        int[] sizes = {5000, 10000, 20000, 30000, 40000, 50000};
        for (int size : sizes) {
            List<Document> docs = allDocs.subList(0, Math.min(size, allDocs.size()));
            InvertedIndex index = new InvertedIndex();
            index.indexAll(docs);
            int T = index.getTotalTokensIndexed();
            int M = index.getVocabularySize();
            // Heaps' Law: M ≈ k * T^beta, tipicamente beta=0.5
            double heapsRatio = M / Math.sqrt(T);
            System.out.printf("%-12d %-15d %-20.3f%n", T, M, heapsRatio);
        }
    }
}