package server;

public class Card {
    private final int number;
    private final String suit;

    public Card(int number, String suit) {
        this.number = number;
        this.suit = suit;
    }

    public int getValue() {
        return Math.min(number, 10); // Caso seja 1 (Ás), retorna 1, caso seja 11, 12 ou 13, retorna 10
    }

    @Override
    public String toString() {
        String name;
        name = switch (number) {
            case 1 -> "Ás";
            case 11 -> "Valete";
            case 12 -> "Dama";
            case 13 -> "Rei";
            default -> String.valueOf(number);
        };
        return name + " de " + suit;
    }
}
