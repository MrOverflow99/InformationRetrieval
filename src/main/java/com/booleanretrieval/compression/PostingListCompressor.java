package com.booleanretrieval.compression;

import com.booleanretrieval.model.Posting;
import java.util.ArrayList;
import java.util.List;

public class PostingListCompressor {

    public byte[] compress(List<Posting> postingList) {
        if (postingList == null || postingList.isEmpty()) return new byte[0];

        List<Byte> bytes = new ArrayList<>();
        int prevDocId = 0;

        for (Posting posting : postingList) {
            // Gap del docId rispetto al precedente
            int gap = posting.getDocId() - prevDocId;
            prevDocId = posting.getDocId();

            writeVByte(gap, bytes);

            // Frequenza (numero di posizioni)
            List<Integer> positions = posting.getPositions();
            writeVByte(positions.size(), bytes);

            // Gap encoding anche per le posizioni
            int prevPos = 0;
            for (int pos : positions) {
                writeVByte(pos - prevPos, bytes);
                prevPos = pos;
            }
        }

        // Converti List<Byte> in byte[] primitivo
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }

    public List<Posting> decompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return new ArrayList<>();
        }

        List<Posting> result = new ArrayList<>();
        int[] cursor = {0}; // array per passare per riferimento (Java non ha ref params)
        int prevDocId = 0;

        while (cursor[0] < compressed.length) {
            // Leggi gap e ricostruisci docId assoluto
            int gap   = readVByte(compressed, cursor);
            int docId = prevDocId + gap;
            prevDocId = docId;

            Posting posting = new Posting(docId);

            // Leggi le posizioni
            int posCount = readVByte(compressed, cursor);
            int prevPos  = 0;
            for (int i = 0; i < posCount; i++) {
                int posGap = readVByte(compressed, cursor);
                int pos    = prevPos + posGap;
                prevPos    = pos;
                posting.addPosition(pos);
            }

            result.add(posting);
        }
        return result;
    }

    private void writeVByte(int value, List<Byte> bytes) {
        while (value > 127) {
            // MSB = 0: ci sono altri byte
            bytes.add((byte) (value & 0x7F)); // prendi 7 bit, MSB=0
            value >>= 7;
        }
        // Ultimo byte: MSB = 1
        bytes.add((byte) (value | 0x80));
    }

    private int readVByte(byte[] data, int[] cursor) {
        int value = 0;
        int shift = 0;

        while (cursor[0] < data.length) {
            byte b = data[cursor[0]++];
            // I 7 bit bassi sono dati
            value |= (b & 0x7F) << shift;
            shift += 7;
            // Se MSB = 1 → questo è l'ultimo byte del numero
            if ((b & 0x80) != 0) break;
        }
        return value;
    }


    public CompressionStats computeStats(List<Posting> postingList) {
        // Dimensione originale: per ogni posting,
        // 4 byte docId + 4 byte per ogni posizione
        int originalBytes = 0;
        for (Posting p : postingList) {
            originalBytes += 4; // docId
            originalBytes += 4 * p.getPositions().size(); // posizioni
        }

        byte[] compressed = compress(postingList);
        int compressedBytes = compressed.length;

        double ratio = originalBytes == 0 ? 1.0 :
                (double) compressedBytes / originalBytes;

        return new CompressionStats(originalBytes, compressedBytes, ratio);
    }

    public record CompressionStats(
            int originalBytes,
            int compressedBytes,
            double compressionRatio
    ) {
        public double savedPercent() {
            return (1.0 - compressionRatio) * 100;
        }

        @Override
        public String toString() {
            return String.format(
                    "Originale: %d bytes | Compresso: %d bytes | Ratio: %.2f | Risparmio: %.1f%%",
                    originalBytes, compressedBytes, compressionRatio, savedPercent()
            );
        }
    }
}