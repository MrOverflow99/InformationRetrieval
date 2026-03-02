package com.booleanretrieval.model;

/*
*
*   Primo file del progetto, rispettando il principio
*   Single Responsibility Principle (SRP)
*   Rappresenta un documento nella collezione.
*   Document sa solo com'è fatto un documento, non sa come
*   indicizzarlo, cercarlo, o visualizzarlo.
*
*/

public final class Document {

    private final int docId;
    private final String filePath;
    private final String content;
    private final String sentiment;

    public Document(int docID, String filePath, String content, String sentiment) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath non puo' essere null o vuoto");
        }
        if (content == null){
            throw new IllegalArgumentException("content non puo' essere null");
        }

        this.docId = docID;
        this.filePath = filePath;
        this.content = content;
        this.sentiment = sentiment;
    }

    public int getDocId() {

        return docId;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContent() {
        return content;
    }

    public String getSentiment() {
        return sentiment;
    }

    @Override
    public String toString() {
        return "Document{" +
                "docID=" + docId +
                ", sentiment='" + sentiment + '\'' +
                ", filePath='" + filePath + '\'' +
                ", contentLength=" + content.length() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document other)) return false;
        return docId == other.docId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(docId);
    }

}
