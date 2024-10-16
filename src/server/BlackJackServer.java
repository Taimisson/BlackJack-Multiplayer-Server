package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlackJackServer {
    private static final int PORT = 12345;
    protected final List<ClientHandler> players = Collections.synchronizedList(new ArrayList<>()); // List of players
    protected Deck deck;
    protected boolean gameInProgress = false;
    protected int currentPlayerIndex = 0;
    protected int currentRound = 1; // Starts at round 1

    public static void main(String[] args) {
        new BlackJackServer().startServer();
    }

    public void startServer() {
        System.out.println("Servidor BlackJack iniciado...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                if (players.size() < 2) {
                    Socket socket = serverSocket.accept();
                    ClientHandler player = new ClientHandler(socket, players.size() + 1, this);
                    players.add(player);
                    player.start();
                    System.out.println("Novo jogador conectado. Total de jogadores: " + players.size());
                    if (players.size() == 2) {
                        startGame();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to start the game
    public void startGame() {
        currentRound = 1; // Reset the round to 1 when starting a new game
        deck = new Deck();
        deck.shuffle();
        gameInProgress = true;

        // Distribute initial cards
        synchronized (players) {
            for (ClientHandler player : players) {
                // Visible card
                Card visibleCard = deck.drawCard();
                player.setVisibleCard(visibleCard);
                player.sendMessage("VISIBLE_CARD " + visibleCard);

                // Hidden card
                Card hiddenCard = deck.drawCard();
                player.setHiddenCard(hiddenCard);
                player.sendMessage("HIDDEN_CARD " + hiddenCard);

                // Update total
                player.updateTotal();

                // Reset turn flags
                player.setTurnDone(false);
            }

            // Send visible cards information to players
            for (ClientHandler player : players) {
                // Send opponent's visible card
                ClientHandler opponent = getOpponent(player);
                player.sendMessage("OPPONENT_VISIBLE_CARD " + opponent.getVisibleCard());
            }

            // Notify the first player that it's their turn
            if (!players.isEmpty()) {
                players.get(currentPlayerIndex).sendMessage("YOUR_TURN");
                System.out.println("Rodada " + currentRound + " iniciada. É a vez do Jogador " + players.get(currentPlayerIndex).getPlayerId());
            }
        }
    }

    // Get the opponent of a player
    public ClientHandler getOpponent(ClientHandler player) {
        for (ClientHandler p : players) {
            if (p != player) {
                return p;
            }
        }
        return null;
    }

    // Method to calculate the hand value
    public int calculateHandValue(List<Card> hand) {
        int total = 0;
        int aces = 0;
        for (Card card : hand) {
            int value = card.getValue();
            if (value == 1) {
                aces++;
                value = 11;
            }
            total += value;
        }
        while (total > 21 && aces > 0) { // If bust and has Aces, convert Aces from 11 to 1
            total -= 10;
            aces--;
        }
        return total;
    }
}
