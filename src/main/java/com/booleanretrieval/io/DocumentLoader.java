package com.booleanretrieval.io;

import com.booleanretrieval.model.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/*
 * Carica i documenti dal filesystem nella memoria.
 *
 * STRUTTURA IMDB:
 *
 * aclImdb/
 *   train/
 *     pos/   <- file .txt con recensioni positive
 *     neg/   <- file .txt con recensioni negative
 *   test/
 *     pos/
 *     neg/
 */

public class DocumentLoader {

    private final Path datasetRoot;
    private final AtomicInteger idCounter = new AtomicInteger(0); // salviamo da bug se in futuro aggiungiamo parallelismo.

    public DocumentLoader(Path datasetRoot) {
        if (datasetRoot == null || !Files.isDirectory(datasetRoot)) {
            throw new IllegalArgumentException("datasetRoot deve essere una directory valida: " + datasetRoot);
        }
        this.datasetRoot = datasetRoot;
    }

    public List<Document> loadAll(int maxDocuments) throws IOException {
        List<Document> documents = new ArrayList<>();

        String[] splits = {"train", "test"};
        String[] sentiments = {"pos", "neg"};

        outer:
        for (String split : splits) {
            for (String sentiment : sentiments) {
                Path dir = datasetRoot.resolve(split).resolve(sentiment);

                if (!Files.isDirectory(dir)) {
                    System.err.println("Error: Directory non trovata, skip: " + dir);
                    continue;
                }

                try (Stream<Path> files = Files.walk(dir)){
                    List<Path> txtFiles = files
                            .filter(p -> p.toString().endsWith(".txt"))
                            .sorted() // ordinamento deterministico — gli ID saranno sempre gli stessi
                            .toList();
                    for (Path file : txtFiles) {
                        if (maxDocuments > 0 && documents.size() >= maxDocuments) {
                            break outer;
                        }

                        String content = Files.readString(file);
                        int docId = idCounter.getAndIncrement();
                        documents.add(new Document(docId, file.toString(), content, sentiment));
                    }
                }
            }
        }

        System.out.printf("Caricati %d documenti da %s%n", documents.size(), datasetRoot);
        return documents;
    }

    public List<Document> loadAll() throws IOException {
        return loadAll(-1);
    }
}
