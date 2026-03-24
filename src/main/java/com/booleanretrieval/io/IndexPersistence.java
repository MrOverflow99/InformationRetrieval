package com.booleanretrieval.io;

import com.booleanretrieval.index.InvertedIndex;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * Gestisce il salvataggio e caricamento dell'indice su disco.
 */
public class IndexPersistence {

    private final Path indexFile;

    public IndexPersistence(Path indexFile) {
        this.indexFile = indexFile;
    }

    /*
     * Salva l'indice su disco.
     */
    public void save(InvertedIndex index) throws IOException {
        // Crea le directory padre se non esistono
        Files.createDirectories(indexFile.getParent());

        long start = System.currentTimeMillis();
        System.out.println("Salvataggio indice in: " + indexFile);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(
                        Files.newOutputStream(indexFile)))) {
            oos.writeObject(index);
        }

        long elapsed = System.currentTimeMillis() - start;
        long fileSize = Files.size(indexFile);
        System.out.printf("Indice salvato: %.1f MB in %d ms%n",
                fileSize / 1_000_000.0, elapsed);
    }

    /**
     * Carica l'indice da disco.
     * Restituisce null se il file non esiste.
     */

    public InvertedIndex load() throws IOException, ClassNotFoundException {
        if (!Files.exists(indexFile)) {
            System.out.println("Nessun indice salvato trovato — verrà creato.");
            return null;
        }

        long start = System.currentTimeMillis();
        long fileSize = Files.size(indexFile);
        System.out.printf("Caricamento indice da disco (%.1f MB)...%n",
                fileSize / 1_000_000.0);

        InvertedIndex index;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(
                        Files.newInputStream(indexFile)))) {
            index = (InvertedIndex) ois.readObject();
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("Indice caricato: %d documenti, %d termini in %d ms%n",
                index.getTotalDocuments(), index.getVocabularySize(), elapsed);
        return index;
    }

    /*
     * True se esiste un indice salvato compatibile.
     */
    public boolean exists() {
        return Files.exists(indexFile);
    }

    public void delete() throws IOException {
        Files.deleteIfExists(indexFile);
        System.out.println("Indice cancellato — verrà reindicizzato al prossimo avvio.");
    }
}