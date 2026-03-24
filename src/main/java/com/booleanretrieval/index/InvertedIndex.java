package com.booleanretrieval.index;

import com.booleanretrieval.model.Document;
import com.booleanretrieval.model.Posting;
import com.booleanretrieval.model.Term;
import com.booleanretrieval.preprocessing.PreprocessingPipeline;

import java.io.Serializable;
import java.util.*;

public class InvertedIndex implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TreeMap<String, Term> dictionary;

    private final HashMap<Integer, Document> documents;

    private transient PreprocessingPipeline pipeline;
    private final KGramIndex kgramIndex = new KGramIndex(3);

    // Statistiche per TF-IDF e il report
    private int totalTokensIndexed = 0;

    public InvertedIndex() {
        this.dictionary = new TreeMap<>();
        this.documents  = new HashMap<>();
        this.pipeline   = new PreprocessingPipeline();
    }

    public void indexDocument(Document doc) {
        // Registra il documento
        documents.put(doc.getDocId(), doc);

        // Preprocessing: ottieni la lista di token normalizzati
        List<String> tokens = pipeline.process(doc.getContent());
        totalTokensIndexed += tokens.size();

        Map<String, Posting> docPostings = new HashMap<>();

        for (int position = 0; position < tokens.size(); position++) {
            String token = tokens.get(position);

            // Recupera o crea il Term nel dizionario globale
            Term term = dictionary.computeIfAbsent(token, t -> {
                kgramIndex.addTerm(t); // nuovo termine → aggiungilo al K-Gram Index
                return new Term(t);
            });

            // Recupera o crea il Posting per questo documento
            Posting posting = docPostings.computeIfAbsent(token, k -> {
                Posting newPosting = new Posting(doc.getDocId());
                term.addPosting(newPosting); // aggiunge al Term globale
                return newPosting;
            });

            posting.addPosition(position);
        }
    }

    private Object readResolve() {
        this.pipeline = new PreprocessingPipeline();
        return this;
    }

    public void indexAll(List<Document> docs) {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < docs.size(); i++) {
            indexDocument(docs.get(i));

            if ((i + 1) % 1000 == 0) {
                System.out.printf("Indicizzati %d/%d documenti (termini unici: %d)%n",
                        i + 1, docs.size(), dictionary.size());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("Indicizzazione completata: %d documenti, %d termini unici, %d token totali in %dms%n",
                docs.size(), dictionary.size(), totalTokensIndexed, elapsed);
    }

    // =========== METODI DI RICERCA ===========

    public List<Posting> getPostingList(String rawTerm) {
        // Il termine della query deve subire lo stesso preprocessing
        // dei termini indicizzati, altrimenti non matcheranno mai!
        List<String> processed = pipeline.processQuery(rawTerm);
        if (processed.isEmpty()) return Collections.emptyList();

        String stemmedTerm = processed.get(0);
        Term term = dictionary.get(stemmedTerm);
        return term == null ? Collections.emptyList() : term.getPostingList();
    }

    /**
     * Restituisce il Term object (con DF per TF-IDF).
     */
    public Term getTerm(String rawTerm) {
        List<String> processed = pipeline.processQuery(rawTerm);
        if (processed.isEmpty()) return null;
        return dictionary.get(processed.get(0));
    }

    public Document getDocument(int docId) {
        return documents.get(docId);
    }

    public int getTotalDocuments() {
        return documents.size();
    }

    public int getVocabularySize() {
        return dictionary.size();
    }

    public int getTotalTokensIndexed() {
        return totalTokensIndexed;
    }

    public List<String> getSampleTerms(int n) {
        return dictionary.keySet().stream().limit(n).toList();
    }

    /*
     * Statistiche per il report
     */

    public void printStats() {
        System.out.println("\n=== Statistiche Indice ===");
        System.out.println("Documenti indicizzati : " + getTotalDocuments());
        System.out.println("Termini unici (vocab) : " + getVocabularySize());
        System.out.println("Token totali          : " + getTotalTokensIndexed());
        System.out.printf("Media token/documento : %.1f%n",
                (double) totalTokensIndexed / getTotalDocuments());

        // Trova i 10 termini più frequenti (highest DF)
        System.out.println("\nTop 10 termini per document frequency:");
        dictionary.values().stream()
                .sorted(Comparator.comparingInt(Term::getDocumentFrequency).reversed())
                .limit(10)
                .forEach(t -> System.out.printf("  %-15s df=%-6d%n",
                        t.getTerm(), t.getDocumentFrequency()));
    }

    // debug: accesso diretto al dizionario senza preprocessing
    public Term getTermDirect(String stemmedTerm) {
        return dictionary.get(stemmedTerm);
    }

    public KGramIndex getKgramIndex() { return kgramIndex; }
    public Set<String> getVocabulary() { return Collections.unmodifiableSet(dictionary.keySet()); }
    public List<Posting> getPostingListDirect(String stemmedTerm) {
        Term term = dictionary.get(stemmedTerm);
        return term == null ? Collections.emptyList() : term.getPostingList();
    }
}