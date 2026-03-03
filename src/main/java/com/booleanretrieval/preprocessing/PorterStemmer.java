package com.booleanretrieval.preprocessing;

import java.util.List;

/*
*
*  Beh, che dire, buona fortuna.........
*
*/

public class PorterStemmer implements TextProcessor {

    @Override
    public List<String> process(List<String> tokens) {
        return tokens.stream()
                .map(this::stem)
                .toList();
    }

    public String stem(String word) {
        if (word == null || word.length() <= 2) {
            return word; // parole cortissime non vengono stemmate
        }

        char[] b = word.toCharArray();
        int k = b.length - 1; // indice ultimo carattere

        b = step1a(b, k);
        k = b.length - 1;
        b = step1b(b, k);
        k = b.length - 1;
        b = step1c(b, k);
        k = b.length - 1;
        b = step2(b, k);
        k = b.length - 1;
        b = step3(b, k);
        k = b.length - 1;
        b = step4(b, k);
        k = b.length - 1;
        b = step5a(b, k);
        k = b.length - 1;
        b = step5b(b, k);

        return new String(b);
    }

    // true se il carattere in posizione i è una vocale
    private boolean isVowel(char[] b, int i) {
        switch (b[i]) {
            case 'a': case 'e': case 'i': case 'o': case 'u': return true;
            case 'y': return i > 0 && !isVowel(b, i - 1);
            default: return false;
        }
    }

    /*
     * Calcola la "measure" m della parola (o del suo stem fino a j).
     * m conta le coppie VC nella struttura della parola.
     * m=0 → nessuna VC (es: "tr", "ee")
     * m=1 → una VC (es: "tree", "trouble")
     * m=2 → due VC (es: "oat", "trees")
     */

    private int measure(char[] b, int j) {
        int n = 0;
        int i = 0;
        // Salta consonanti iniziali
        while (i <= j && !isVowel(b, i)) i++;
        while (i <= j) {
            while (i <= j && isVowel(b, i)) i++;
            if (i > j) break;
            n++;
            while (i <= j && !isVowel(b, i)) i++;
        }
        return n;
    }

    // true se la parola (fino a j) contiene almeno una vocale
    private boolean containsVowel(char[] b, int j) {
        for (int i = 0; i <= j; i++) {
            if (isVowel(b, i)) return true;
        }
        return false;
    }

    // true se b[j-1] e b[j] sono consonanti uguali (double consonant)
    private boolean doubleConsonant(char[] b, int j) {
        if (j < 1) return false;
        if (b[j] != b[j - 1]) return false;
        return !isVowel(b, j);
    }

    /*
     *  true se b[i-1],b[i],b[i+1] hanno pattern consonante-vocale-consonante
     *  e b[i+1] non è w, x, y
     */
    private boolean cvc(char[] b, int i) {
        if (i < 2) return false;
        if (isVowel(b, i) || !isVowel(b, i - 1) || isVowel(b, i - 2)) return false;
        char c = b[i];
        return c != 'w' && c != 'x' && c != 'y';
    }

    private boolean endsWith(char[] b, int k, String suffix) {
        int len = suffix.length();
        if (k < len - 1) return false;
        return new String(b, k - len + 1, len).equals(suffix);
    }

    private char[] setSuffix(char[] b, int j, String suffix) {
        char[] result = new char[j + 1 + suffix.length()];
        System.arraycopy(b, 0, result, 0, j + 1);
        for (int i = 0; i < suffix.length(); i++) {
            result[j + 1 + i] = suffix.charAt(i);
        }
        return result;
    }

    private char[] truncate(char[] b, int newLength) {
        char[] result = new char[newLength];
        System.arraycopy(b, 0, result, 0, newLength);
        return result;
    }

    // =========== I 5 PASSI DELL'ALGORITMO =========== //

    /*Step 1a: gestisce plurali e forme verbali semplici */
    private char[] step1a(char[] b, int k) {
        if (endsWith(b, k, "sses")) return truncate(b, k - 1); // sses → ss
        if (endsWith(b, k, "ies"))  return truncate(b, k - 1); // ies  → i
        if (endsWith(b, k, "ss"))   return b;                  // ss   → ss (invariato)
        if (endsWith(b, k, "s"))    return truncate(b, k);     // s    → (rimuovi)
        return b;
    }

    /* Step 1b: gestisce -eed, -ed, -ing */
    private char[] step1b(char[] b, int k) {
        if (endsWith(b, k, "eed")) {
            int j = k - 3;
            if (measure(b, j) > 0) return truncate(b, k - 1); // eed → ee
            return b;
        }

        boolean flag = false;
        char[] stem = b;

        if (endsWith(b, k, "ed") && containsVowel(b, k - 2)) {
            stem = truncate(b, k - 1);
            flag = true;
        } else if (endsWith(b, k, "ing") && containsVowel(b, k - 3)) {
            stem = truncate(b, k - 2);
            flag = true;
        }

        if (flag) {
            int j = stem.length - 1;
            if (endsWith(stem, j, "at") || endsWith(stem, j, "bl") || endsWith(stem, j, "iz")) {
                return setSuffix(stem, j, "e");
            }
            if (doubleConsonant(stem, j)) {
                char last = stem[j];
                if (last != 'l' && last != 's' && last != 'z') {
                    return truncate(stem, j); // rimuovi consonante doppia
                }
            }
            if (measure(stem, j) == 1 && cvc(stem, j)) {
                return setSuffix(stem, j, "e");
            }
            return stem;
        }
        return b;
    }

    /** Step 1c: y → i se c'è una vocale nello stem */
    private char[] step1c(char[] b, int k) {
        if (endsWith(b, k, "y") && containsVowel(b, k - 1)) {
            b[k] = 'i';
        }
        return b;
    }

    /** Step 2: suffissi più lunghi con m > 0 */
    private char[] step2(char[] b, int k) {
        if (k < 1) return b;
        switch (b[k - 1]) {
            case 'a':
                if (endsWith(b, k, "ational") && measure(b, k-7) > 0) return setSuffix(b, k-7, "ate");
                if (endsWith(b, k, "tional")  && measure(b, k-6) > 0) return setSuffix(b, k-6, "tion");
                break;
            case 'c':
                if (endsWith(b, k, "enci") && measure(b, k-4) > 0) return setSuffix(b, k-4, "ence");
                if (endsWith(b, k, "anci") && measure(b, k-4) > 0) return setSuffix(b, k-4, "ance");
                break;
            case 'e':
                if (endsWith(b, k, "izer") && measure(b, k-4) > 0) return setSuffix(b, k-4, "ize");
                break;
            case 'l':
                if (endsWith(b, k, "bli")  && measure(b, k-3) > 0) return setSuffix(b, k-3, "ble");
                if (endsWith(b, k, "alli") && measure(b, k-4) > 0) return setSuffix(b, k-4, "al");
                if (endsWith(b, k, "entli") && measure(b, k-5) > 0) return setSuffix(b, k-5, "ent");
                if (endsWith(b, k, "eli")  && measure(b, k-3) > 0) return setSuffix(b, k-3, "e");
                if (endsWith(b, k, "ousli") && measure(b, k-5) > 0) return setSuffix(b, k-5, "ous");
                break;
            case 'o':
                if (endsWith(b, k, "ization") && measure(b, k-7) > 0) return setSuffix(b, k-7, "ize");
                if (endsWith(b, k, "ation")   && measure(b, k-5) > 0) return setSuffix(b, k-5, "ate");
                if (endsWith(b, k, "ator")    && measure(b, k-4) > 0) return setSuffix(b, k-4, "ate");
                break;
            case 's':
                if (endsWith(b, k, "alism")   && measure(b, k-5) > 0) return setSuffix(b, k-5, "al");
                if (endsWith(b, k, "iveness") && measure(b, k-7) > 0) return setSuffix(b, k-7, "ive");
                if (endsWith(b, k, "fulness") && measure(b, k-7) > 0) return setSuffix(b, k-7, "ful");
                if (endsWith(b, k, "ousness") && measure(b, k-7) > 0) return setSuffix(b, k-7, "ous");
                break;
            case 't':
                if (endsWith(b, k, "aliti")  && measure(b, k-5) > 0) return setSuffix(b, k-5, "al");
                if (endsWith(b, k, "iviti")  && measure(b, k-5) > 0) return setSuffix(b, k-5, "ive");
                if (endsWith(b, k, "biliti") && measure(b, k-6) > 0) return setSuffix(b, k-6, "ble");
                break;
        }
        return b;
    }

    /* Step 3: suffissi con m > 0 */
    private char[] step3(char[] b, int k) {
        switch (b[k]) {
            case 'e':
                if (endsWith(b, k, "icate") && measure(b, k-5) > 0) return truncate(b, k-4);
                if (endsWith(b, k, "ative") && measure(b, k-5) > 0) return truncate(b, k-4);
                if (endsWith(b, k, "alize") && measure(b, k-5) > 0) return setSuffix(b, k-5, "al");
                break;
            case 'i':
                if (endsWith(b, k, "iciti") && measure(b, k-5) > 0) return setSuffix(b, k-5, "ic");
                break;
            case 'l':
                if (endsWith(b, k, "ical") && measure(b, k-4) > 0) return truncate(b, k-3);
                if (endsWith(b, k, "ful")  && measure(b, k-3) > 0) return truncate(b, k-2);
                break;
            case 's':
                if (endsWith(b, k, "ness") && measure(b, k-4) > 0) return truncate(b, k-3);
                break;
        }
        return b;
    }

    /* Step 4: suffissi con m > 1 */
    private char[] step4(char[] b, int k) {
        if (k < 1) return b;
        switch (b[k - 1]) {
            case 'a':
                if (endsWith(b, k, "al") && measure(b, k-2) > 1) return truncate(b, k-1);
                break;
            case 'c':
                if (endsWith(b, k, "ance") && measure(b, k-4) > 1) return truncate(b, k-3);
                if (endsWith(b, k, "ence") && measure(b, k-4) > 1) return truncate(b, k-3);
                break;
            case 'e':
                if (endsWith(b, k, "er") && measure(b, k-2) > 1) return truncate(b, k-1);
                break;
            case 'i':
                if (endsWith(b, k, "ic") && measure(b, k-2) > 1) return truncate(b, k-1);
                break;
            case 'l':
                if (endsWith(b, k, "able") && measure(b, k-4) > 1) return truncate(b, k-3);
                if (endsWith(b, k, "ible") && measure(b, k-4) > 1) return truncate(b, k-3);
                break;
            case 'n':
                if (endsWith(b, k, "ant")  && measure(b, k-3) > 1) return truncate(b, k-2);
                if (endsWith(b, k, "ement") && measure(b, k-5) > 1) return truncate(b, k-4);
                if (endsWith(b, k, "ment") && measure(b, k-4) > 1) return truncate(b, k-3);
                if (endsWith(b, k, "ent")  && measure(b, k-3) > 1) return truncate(b, k-2);
                break;
            case 'o':
                if (endsWith(b, k, "ion") && measure(b, k-3) > 1) {
                    char prev = b[k-3];
                    if (prev == 's' || prev == 't') return truncate(b, k-2);
                }
                if (endsWith(b, k, "ou") && measure(b, k-2) > 1) return truncate(b, k-1);
                break;
            case 's':
                if (endsWith(b, k, "ism") && measure(b, k-3) > 1) return truncate(b, k-2);
                break;
            case 't':
                if (endsWith(b, k, "ate") && measure(b, k-3) > 1) return truncate(b, k-2);
                if (endsWith(b, k, "iti") && measure(b, k-3) > 1) return truncate(b, k-2);
                break;
            case 'u':
                if (endsWith(b, k, "ous") && measure(b, k-3) > 1) return truncate(b, k-2);
                break;
            case 'v':
                if (endsWith(b, k, "ive") && measure(b, k-3) > 1) return truncate(b, k-2);
                break;
            case 'z':
                if (endsWith(b, k, "ize") && measure(b, k-3) > 1) return truncate(b, k-2);
                break;
        }
        return b;
    }

    /* Step 5a: rimuove -e finale con m > 1, o m=1 e non cvc */
    private char[] step5a(char[] b, int k) {
        if (b[k] == 'e') {
            int m = measure(b, k - 1);
            if (m > 1 || (m == 1 && !cvc(b, k - 1))) {
                return truncate(b, k);
            }
        }
        return b;
    }

    /* Step 5b: -ll → -l con m > 1 */
    private char[] step5b(char[] b, int k) {
        if (doubleConsonant(b, k) && measure(b, k) > 1 && b[k] == 'l') {
            return truncate(b, k);
        }
        return b;
    }
}