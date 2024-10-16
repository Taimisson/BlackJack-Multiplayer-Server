package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        for (String suit : new String[]{"Copas", "Ouros", "Espadas", "Paus"}) // Cria as cartas
            for (int i = 1; i <= 13; i++) cards.add(new Card(i, suit));

    }

    public void shuffle() {
        Collections.shuffle(cards); // Isso vai embaralhar o baralho
    }

    public Card drawCard() {
        if (cards.isEmpty()) {
            // Se o baralho estiver vazio, reembaralha
            System.out.println("Reembaralhando o baralho.");
            for (String suit : new String[]{"Copas", "Ouros", "Espadas", "Paus"})
                for (int i = 1; i <= 13; i++)
                    cards.add(new Card(i, suit));
            shuffle();
        }
        return cards.removeFirst();
    }
}
