package com.booleanretrieval.preprocessing;

import java.util.List;

/*
*
* PERCHÉ UN'INTERFACCIA?
* Ogni step della pipeline fa la stessa cosa concettualmente:
* prende una lista di token, la trasforma, restituisce una lista.
* L'interfaccia formalizza questo contratto.
*
* Vantaggio pratico: possiamo costruire la pipeline come
* List<TextProcessor> e applicarli in sequenza senza sapere
* quale specifico processor stiamo usando. Questo è il
* principio Open/Closed: aperto all'estensione (aggiungi un processor),
* chiuso alla modifica (non tocchi il codice esistente).
*
*/

public interface TextProcessor {
    List<String> process(List<String> tokens);
 }
