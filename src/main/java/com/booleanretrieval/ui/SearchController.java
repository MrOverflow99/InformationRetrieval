package com.booleanretrieval.ui;

import com.booleanretrieval.index.InvertedIndex;
import com.booleanretrieval.index.KGramIndex;
import com.booleanretrieval.io.DocumentLoader;
import com.booleanretrieval.model.Document;
import com.booleanretrieval.ranking.TFIDFRanker;
import com.booleanretrieval.search.QueryExecutor;
import com.booleanretrieval.search.SpellingCorrector;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import com.booleanretrieval.compression.PostingListCompressor;
import com.booleanretrieval.model.Posting;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import com.booleanretrieval.io.IndexPersistence;
import java.nio.file.Path;

public class SearchController implements Initializable {


    @FXML private TextField queryField;
    @FXML private Button searchButton;
    @FXML private Label statusLabel;
    @FXML private Label resultsCountLabel;
    @FXML private Label queryTimeLabel;
    @FXML private ListView<String> resultsList;
    @FXML private Label detailArea;
    @FXML private TitledPane detailPane;
    @FXML private RadioButton rbAnd;
    @FXML private RadioButton rbOptAnd;
    @FXML private RadioButton rbOr;
    @FXML private RadioButton rbPhrase;
    @FXML private RadioButton rbWildcard;
    @FXML private CheckBox cbRanking;
    @FXML private HBox spellBox;
    @FXML private HBox suggestionsBox;

    // Componenti del backend
    private InvertedIndex index;
    private QueryExecutor executor;
    private TFIDFRanker ranker;
    private SpellingCorrector corrector;

    // Lista osservabile per la ListView — JavaFX aggiorna la UI automaticamente
    private final ObservableList<String> resultsData = FXCollections.observableArrayList();

    // Mappa per recuperare i Document dai risultati visualizzati
    private List<Integer> currentDocIds = Collections.emptyList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        resultsList.setItems(resultsData);
        searchButton.setDisable(true);

        // Configura la ListView per mostrare i dettagli al click
        resultsList.setOnMouseClicked(event -> {
            int selectedIndex = resultsList.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < currentDocIds.size()) {
                showDocumentDetail(currentDocIds.get(selectedIndex));
            }
        });

        // Permetti di cercare premendo Invio
        queryField.setOnAction(event -> onSearch());

        // Carica l'indice in un thread separato — la UI rimane reattiva
        Thread indexThread = new Thread(this::loadIndex);
        indexThread.setDaemon(true); // il thread si chiude quando si chiude l'app
        indexThread.start();
    }

    private void loadIndex() {
        try {
            // Path dove salviamo l'indice — nella home dell'utente
            Path indexPath = Path.of(System.getProperty("user.home"),
                    ".booleanretrieval", "index.ser");
            IndexPersistence persistence = new IndexPersistence(indexPath);

            if (persistence.exists()) {
                // === CASO 1: indice già salvato → caricalo ===
                updateStatus("Caricamento indice da disco...");
                try {
                    index = persistence.load();
                    updateStatus("Indice caricato da disco ✓");
                } catch (Exception e) {
                    // File corrotto o versione incompatibile → reindicizza
                    System.err.println("Indice corrotto, reindicizzazione: " + e.getMessage());
                    index = null;
                }
            }

            if (index == null) {
                // === CASO 2: nessun indice → costruiscilo e salvalo ===
                updateStatus("Caricamento dataset...");
                Path datasetPath = Path.of(System.getProperty("user.home"),
                        "Desktop", "Dataset", "aclImdb");
                DocumentLoader loader = new DocumentLoader(datasetPath);
                List<Document> docs = loader.loadAll();

                updateStatus("Indicizzazione in corso...");
                index = new InvertedIndex();

                for (int i = 0; i < docs.size(); i++) {
                    index.indexDocument(docs.get(i));
                    if ((i + 1) % 5000 == 0) {
                        int progress = i + 1;
                        updateStatus("Indicizzati " + progress + "/" + docs.size() + "...");
                    }
                }

                // Salva per i prossimi avvii
                updateStatus("Salvataggio indice su disco...");
                persistence.save(index);
            }

            executor  = new QueryExecutor(index);
            ranker    = new TFIDFRanker(index);
            corrector = new SpellingCorrector(index.getKgramIndex(), index.getVocabulary());

            // Compression stats nel terminale
            printCompressionStats();

            Platform.runLater(() -> {
                statusLabel.setText("Indice pronto: " + index.getTotalDocuments() +
                        " documenti, " + index.getVocabularySize() + " termini unici");
                searchButton.setDisable(false);
                queryField.requestFocus();
            });

        } catch (Exception e) {
            updateStatus("Errore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printCompressionStats() {
        PostingListCompressor compressor = new PostingListCompressor();
        System.out.println("\n=== Posting List Compression ===");
        String[] testTerms = {"film", "horror", "brilliant", "titanic"};
        for (String term : testTerms) {
            List<Posting> pl = index.getPostingList(term);
            if (pl.isEmpty()) continue;
            PostingListCompressor.CompressionStats stats = compressor.computeStats(pl);
            System.out.printf("'%-12s' df=%-6d %s%n", term, pl.size(), stats);
        }
        long totalOrig = 0, totalComp = 0;
        for (String t : index.getSampleTerms(1000)) {
            List<Posting> pl = index.getPostingListDirect(t);
            if (pl.isEmpty()) continue;
            PostingListCompressor.CompressionStats s = compressor.computeStats(pl);
            totalOrig += s.originalBytes();
            totalComp += s.compressedBytes();
        }
        System.out.printf("Risparmio globale stimato: %.1f%%%n",
                (1.0 - (double) totalComp / totalOrig) * 100);
    }

    @FXML
    public void onSearch() {
        String query = queryField.getText().trim();
        if (query.isEmpty() || index == null) return;

        searchButton.setDisable(true);
        resultsData.clear();
        spellBox.setVisible(false);
        spellBox.setManaged(false);

        Thread searchThread = new Thread(() -> {
            long startTime = System.nanoTime();
            List<Integer> docIds = executeQuery(query);
            long elapsed = (System.nanoTime() - startTime) / 1_000_000; // ms

            // Ranking TF-IDF se abilitato
            final List<Integer> finalDocIds;
            if (cbRanking.isSelected() && !docIds.isEmpty() &&
                    !rbWildcard.isSelected()) {
                List<TFIDFRanker.ScoredDocument> ranked = ranker.rank(docIds, query);
                finalDocIds = ranked.stream()
                        .map(TFIDFRanker.ScoredDocument::docId)
                        .toList();
            } else {
                finalDocIds = docIds;
            }

            // Spelling correction se nessun risultato
            List<String> suggestions = Collections.emptyList();
            if (finalDocIds.isEmpty()) {
                suggestions = corrector.suggest(query.toLowerCase().split("\\s+")[0], 5);
            }

            final List<String> finalSuggestions = suggestions;
            final long finalElapsed = elapsed;

            Platform.runLater(() -> {
                currentDocIds = finalDocIds;
                updateResultsList(finalDocIds);
                resultsCountLabel.setText(finalDocIds.size() + " risultati");
                queryTimeLabel.setText("(" + finalElapsed + " ms)");
                searchButton.setDisable(false);

                if (!finalSuggestions.isEmpty() && finalDocIds.isEmpty()) {
                    showSpellingSuggestions(finalSuggestions);
                }
            });
        });
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private List<Integer> executeQuery(String query) {
        if (rbAnd.isSelected())    return executor.conjunctiveQuery(query);
        if (rbOptAnd.isSelected()) return executor.optimizedConjunctiveQuery(query);
        if (rbOr.isSelected())     return executor.disjunctiveQuery(query);
        if (rbPhrase.isSelected()) return executor.phraseQuery(query);
        if (rbWildcard.isSelected()) return executeWildcardQuery(query);
        return executor.conjunctiveQuery(query);
    }
    private List<Integer> executeWildcardQuery(String pattern) {
        KGramIndex kgramIdx = index.getKgramIndex();
        var matchingTerms = kgramIdx.wildcardSearch(pattern.toLowerCase());

        if (matchingTerms.isEmpty()) return Collections.emptyList();

        return matchingTerms.stream()
                .flatMap(term -> index.getPostingList(term).stream())
                .map(p -> p.getDocId())
                .distinct()
                .sorted()
                .toList();
    }

    private void updateResultsList(List<Integer> docIds) {
        ObservableList<String> items = FXCollections.observableArrayList();
        int rank = 1;
        for (int docId : docIds) {
            Document doc = index.getDocument(docId);
            String preview = doc.getContent()
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            preview = preview.substring(0, Math.min(130, preview.length()));
            String sentiment = doc.getSentiment().equals("pos") ? "★" : "▼";
            items.add(String.format("%3d  %s  doc%-6d  %s...",
                    rank++, sentiment, docId, preview));
        }
        resultsData.setAll(items);

        // Stile celle ListView
        resultsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #2D2D2D;");
                } else {
                    setText(item);
                    boolean isPos = item.contains("★");
                    // Alterna sfondo righe per leggibilità
                    String bg = getIndex() % 2 == 0 ? "#2D2D2D" : "#323232";
                    String accent = isPos ? "#87A556" : "#C0392B";
                    setStyle("-fx-background-color: " + bg + ";" +
                            "-fx-text-fill: #DDDDDD;" +
                            "-fx-font-size: 12px;" +
                            "-fx-padding: 6 12 6 12;" +
                            "-fx-border-color: transparent transparent #3A3A3A transparent;" +
                            "-fx-border-width: 0 0 1 0;");
                }
            }
        });
    }

    private void showDocumentDetail(int docId) {
        Document doc = index.getDocument(docId);
        String detail = String.format(
                "DocID: %d | Sentiment: %s | File: %s%n%n%s",
                doc.getDocId(),
                doc.getSentiment().toUpperCase(),
                doc.getFilePath(),
                doc.getContent().replaceAll("<[^>]+>", " ")
                        .replaceAll("\\s+", " ").trim()
        );
        detailArea.setText(detail);
        detailPane.setExpanded(true);
    }

    private void showSpellingSuggestions(List<String> suggestions) {
        suggestionsBox.getChildren().clear();
        for (String suggestion : suggestions) {
            Button btn = new Button(suggestion);
            btn.setStyle("-fx-background-color: #444444;" +
                    "-fx-text-fill: #87A556;" +
                    "-fx-border-color: #87A556;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 2;" +
                    "-fx-background-radius: 2;" +
                    "-fx-cursor: hand;" +
                    "-fx-font-size: 11px;" +
                    "-fx-padding: 2 8 2 8;");
            btn.setOnAction(e -> {
                queryField.setText(suggestion);
                onSearch();
            });
            suggestionsBox.getChildren().add(btn);
        }
        spellBox.setVisible(true);
        spellBox.setManaged(true);
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }
}