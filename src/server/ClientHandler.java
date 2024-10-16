package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler extends Thread { // Thread para lidar com um cliente
    private final Socket socket; // Socket para se comunicar com o cliente
    private PrintWriter out; // Para enviar mensagens ao cliente
    private BufferedReader in; // Para receber mensagens do cliente
    private final int playerId; // ID do jogador
    private Card visibleCard; // Carta visível
    private Card hiddenCard; // Carta oculta
    private final ArrayList<Card> hand; // Mão do jogador
    private boolean inGame = true; // Flag para indicar se o jogador está no jogo
    private boolean isTurnDone = false; // Flag para indicar se o jogador terminou a vez
    private int total = 0; // Total da mão do jogador
    private int lives = 5; // Each player starts with 5 lives
    private BlackJackServer server; // Referência ao servidor

    public ClientHandler(Socket socket, int playerId, BlackJackServer server) { // Construtor
        this.socket = socket;
        this.playerId = playerId;
        this.server = server;
        hand = new ArrayList<>();
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) { // Método para enviar mensagem ao cliente
        out.println(message);
        out.flush();
    }

    public void updateTotal() { // Método para atualizar o total da mão
        hand.clear();
        hand.add(visibleCard);
        hand.add(hiddenCard);
        total = server.calculateHandValue(hand);
    }

    @Override
    public void run() { // Método run da thread
        try {
            sendMessage("PLAYER_ID " + playerId);

            String message;
            while ((message = in.readLine()) != null && inGame) { // Enquanto o jogador estiver no jogo e houver mensagens
                System.out.println("Recebido do Jogador " + playerId + ": " + message);
                if (message.equalsIgnoreCase("HIT") && server.gameInProgress) {
                    if (server.players.get(server.currentPlayerIndex) == this) {
                        Card card = server.deck.drawCard();
                        hand.add(card);
                        total = server.calculateHandValue(hand);
                        sendMessage("CARD " + card);
                        sendMessage("TOTAL " + total);
                        if (total > 21) {
                            sendMessage("BUST");
                            isTurnDone = true;
                            nextTurn();
                        } else {
                            sendMessage("YOUR_TURN");
                            System.out.println("Jogador " + playerId + " pediu HIT. Total agora: " + total);
                        }
                    } else {
                        sendMessage("NOT_YOUR_TURN");
                    }
                } else if (message.equalsIgnoreCase("STAND") && server.gameInProgress) {
                    if (server.players.get(server.currentPlayerIndex) == this) {
                        isTurnDone = true;
                        sendMessage("STAND");
                        System.out.println("Jogador " + playerId + " pediu STAND.");
                        nextTurn();
                    } else {
                        sendMessage("NOT_YOUR_TURN");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Jogador " + playerId + " desconectado.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (server.players) { // Sincronizar a lista de jogadores
                server.players.remove(this); // Remover o jogador da lista
                System.out.println("Jogador removido. Total de jogadores: " + server.players.size());
                server.gameInProgress = false;
            }
        }
    }

    // Next turn
    private void nextTurn() {
        synchronized (server.players) {
            // checar se todos os jogadores terminaram
            if (allPlayersDone())
                determineWinner();
            else {
                server.currentPlayerIndex = (server.currentPlayerIndex + 1) % server.players.size();
                ClientHandler nextPlayer = server.players.get(server.currentPlayerIndex);
                if (!nextPlayer.isTurnDone) {
                    nextPlayer.sendMessage("YOUR_TURN");
                    System.out.println("É a vez do Jogador " + nextPlayer.playerId);
                } else {
                    nextTurn();
                }
            }
        }
    }

    // verifica se todos os jogadores terminaram
    private boolean allPlayersDone() {
        for (ClientHandler player : server.players)
            if (!player.isTurnDone)
                return false;

        return true;
    }


    private void determineWinner() {
        System.out.println("Determinando o vencedor da rodada " + server.currentRound);
        if (server.players.size() < 2) {
            // If a player has disconnected
            System.out.println("Um jogador desconectou. Encerrando o jogo.");
            return;
        }

        ClientHandler player1 = server.players.get(0);
        ClientHandler player2 = server.players.get(1);

        int total1 = player1.total;
        int total2 = player2.total;

        player1.sendMessage("OPPONENT_TOTAL " + total2);
        player2.sendMessage("OPPONENT_TOTAL " + total1);

        String result1;
        String result2;

        int livesAtStake = server.currentRound; // vidas em jogo

        if (total1 > 21 && total2 > 21) {
            result1 = "RESULT Ambos estouraram. Empate.";
            result2 = "RESULT Ambos estouraram. Empate.";
        } else if (total1 > 21) {
            result1 = "RESULT Você estourou. Você perdeu.";
            result2 = "RESULT Seu oponente estourou. Você ganhou!";
            updateLives(player1, player2, livesAtStake);
        } else if (total2 > 21) {
            result1 = "RESULT Seu oponente estourou. Você ganhou!";
            result2 = "RESULT Você estourou. Você perdeu.";
            updateLives(player2, player1, livesAtStake);
        } else {
            if (total1 > total2) {
                result1 = "RESULT Você ganhou!";
                result2 = "RESULT Você perdeu.";
                updateLives(player2, player1, livesAtStake);
            } else if (total1 < total2) {
                result1 = "RESULT Você perdeu.";
                result2 = "RESULT Você ganhou!";
                updateLives(player1, player2, livesAtStake);
            } else {
                result1 = "RESULT Empate.";
                result2 = "RESULT Empate.";
            }
        }

        player1.sendMessage(result1);
        player2.sendMessage(result2);

        player1.sendMessage("LIVES " + player1.lives);
        player1.sendMessage("OPPONENT_LIVES " + player2.lives);

        player2.sendMessage("LIVES " + player2.lives);
        player2.sendMessage("OPPONENT_LIVES " + player1.lives);


        if (player1.lives <= 0 || player2.lives <= 0) {
            String finalResult1, finalResult2;
            if (player1.lives <= 0 && player2.lives <= 0) {
                finalResult1 = "GAME_OVER Ambos jogadores perderam todas as vidas. Empate final.";
                finalResult2 = "GAME_OVER Ambos jogadores perderam todas as vidas. Empate final.";
            } else if (player1.lives <= 0) {
                finalResult1 = "GAME_OVER Você perdeu todas as suas vidas. Você perdeu o jogo.";
                finalResult2 = "GAME_OVER Seu oponente perdeu todas as vidas. Você venceu o jogo!";
            } else {
                finalResult1 = "GAME_OVER Seu oponente perdeu todas as vidas. Você venceu o jogo!";
                finalResult2 = "GAME_OVER Você perdeu todas as suas vidas. Você perdeu o jogo.";
            }
            player1.sendMessage(finalResult1);
            player2.sendMessage(finalResult2);

            player1.sendMessage("GAME_ENDED");
            player2.sendMessage("GAME_ENDED");

            server.gameInProgress = false;
            System.out.println("Jogo finalizado.");
        } else {
            server.currentRound++;
            resetGame();
        }
    }

    private void updateLives(ClientHandler loser, ClientHandler winner, int livesAtStake) {
        loser.lives -= livesAtStake;
        winner.lives += livesAtStake;
        if (loser.lives < 0) {
            loser.lives = 0;
        }
        System.out.println("Jogador " + loser.playerId + " perdeu " + livesAtStake + " vida(s). Vidas restantes: " + loser.lives);
        System.out.println("Jogador " + winner.playerId + " ganhou " + livesAtStake + " vida(s). Vidas totais: " + winner.lives);
    }

    private void resetGame() {
        System.out.println("Resetando o jogo para a rodada " + server.currentRound);
        server.deck = new Deck();
        server.deck.shuffle();
        server.gameInProgress = true;

        synchronized (server.players) {
            for (ClientHandler player : server.players) {
                player.hand.clear();
                player.isTurnDone = false;
                player.updateTotal();
                player.sendMessage("ROUND " + server.currentRound + ". Vale " + server.currentRound + " vida(s).");

                // Distribute new cards
                // Visible card
                Card visibleCard = server.deck.drawCard();
                player.setVisibleCard(visibleCard);
                player.sendMessage("VISIBLE_CARD " + visibleCard);

                // Hidden card
                Card hiddenCard = server.deck.drawCard();
                player.setHiddenCard(hiddenCard);
                player.sendMessage("HIDDEN_CARD " + hiddenCard);

                // Update total
                player.updateTotal();
            }

            // mandar informações das cartas visíveis para os jogadores
            for (ClientHandler player : server.players) {
                ClientHandler opponent = server.getOpponent(player);
                player.sendMessage("OPPONENT_VISIBLE_CARD " + opponent.getVisibleCard());
                player.sendMessage("LIVES " + player.lives);
                player.sendMessage("OPPONENT_LIVES " + opponent.lives);
            }

            // notifica o primeiro jogador que é a vez dele
            server.currentPlayerIndex = 0;
            if (!server.players.isEmpty()) {
                server.players.get(server.currentPlayerIndex).sendMessage("YOUR_TURN");
                System.out.println("Rodada " + server.currentRound + " iniciada. É a vez do Jogador " + server.players.get(server.currentPlayerIndex).getPlayerId());
            }
        }
    }

    // Getters and Setters
    public int getPlayerId() {
        return playerId;
    }

    public Card getVisibleCard() {
        return visibleCard;
    }

    public void setVisibleCard(Card visibleCard) {
        this.visibleCard = visibleCard;
    }

    public void setHiddenCard(Card hiddenCard) {
        this.hiddenCard = hiddenCard;
    }

    public void setTurnDone(boolean isTurnDone) {
        this.isTurnDone = isTurnDone;
    }
}
