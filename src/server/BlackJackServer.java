import java.io.*;
import java.net.*;
import java.util.*;

public class BlackJackServer {
    private static final int PORT = 12345;
    private List<ClientHandler> players = Collections.synchronizedList(new ArrayList<>());
    private Deck deck;
    private boolean gameInProgress = false;
    private int currentPlayerIndex = 0;
    private int currentRound = 1; // Inicia na rodada 1

    public static void main(String[] args) {
        new BlackJackServer().startServer();
    }

    public void startServer() {
        System.out.println("Servidor BlackJack iniciado...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                if (players.size() < 2) {
                    Socket socket = serverSocket.accept();
                    ClientHandler player = new ClientHandler(socket, players.size() + 1);
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

    // Método para iniciar o jogo
    private void startGame() {
        currentRound = 1; // Resetar a rodada para 1 ao iniciar um novo jogo
        deck = new Deck();
        deck.shuffle();
        gameInProgress = true;

        // Distribui cartas iniciais
        synchronized (players) {
            for (ClientHandler player : players) {
                // Carta visível
                Card visibleCard = deck.drawCard();
                player.visibleCard = visibleCard;
                player.sendMessage("VISIBLE_CARD " + visibleCard);

                // Carta oculta
                Card hiddenCard = deck.drawCard();
                player.hiddenCard = hiddenCard;
                player.sendMessage("HIDDEN_CARD " + hiddenCard);

                // Atualiza total
                player.updateTotal();

                // Reinicia flags de turno
                player.isTurnDone = false;
            }

            // Envia informações das cartas visíveis aos jogadores
            for (ClientHandler player : players) {
                // Envia a carta visível do oponente
                ClientHandler opponent = getOpponent(player);
                player.sendMessage("OPPONENT_VISIBLE_CARD " + opponent.visibleCard);
            }

            // Notifica o primeiro jogador que é sua vez
            if (players.size() > 0) {
                players.get(currentPlayerIndex).sendMessage("YOUR_TURN");
                System.out.println("Rodada " + currentRound + " iniciada. É a vez do Jogador " + players.get(currentPlayerIndex).playerId);
            }
        }
    }

    // Obtém o oponente de um jogador
    private ClientHandler getOpponent(ClientHandler player) {
        for (ClientHandler p : players) {
            if (p != player) {
                return p;
            }
        }
        return null;
    }

    // Classe interna para lidar com os clientes
    class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int playerId;
        private Card visibleCard;
        private Card hiddenCard;
        private List<Card> hand;
        private boolean inGame = true;
        private boolean isTurnDone = false;
        private int total = 0;
        private int lives = 5; // Cada jogador começa com 5 vidas

        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            hand = new ArrayList<>();
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        public void sendMessage(String message) {
            out.println(message);
            out.flush();
        }

        public void updateTotal() {
            hand.clear();
            hand.add(visibleCard);
            hand.add(hiddenCard);
            total = calculateHandValue(hand);
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                sendMessage("PLAYER_ID " + playerId);

                String message;
                while ((message = in.readLine()) != null && inGame) {
                    System.out.println("Recebido do Jogador " + playerId + ": " + message);
                    if (message.equalsIgnoreCase("HIT") && gameInProgress) {
                        if (players.get(currentPlayerIndex) == this) {
                            Card card = deck.drawCard();
                            hand.add(card);
                            total = calculateHandValue(hand);
                            sendMessage("CARD " + card);
                            sendMessage("TOTAL " + total);
                            if (total > 21) {
                                sendMessage("BUST");
                                isTurnDone = true;
                                nextTurn();
                            } else {
                                // Jogador não estourou, enviar "YOUR_TURN" novamente
                                sendMessage("YOUR_TURN");
                                System.out.println("Jogador " + playerId + " pediu HIT. Total agora: " + total);
                            }
                        } else {
                            sendMessage("NOT_YOUR_TURN");
                        }
                    } else if (message.equalsIgnoreCase("STAND") && gameInProgress) {
                        if (players.get(currentPlayerIndex) == this) {
                            isTurnDone = true;
                            sendMessage("STAND");
                            System.out.println("Jogador " + playerId + " pediu STAND.");
                            nextTurn();
                        } else {
                            sendMessage("NOT_YOUR_TURN");
                        }
                    }
                    // Removido: Tratamento de "SIM" e "NAO"
                }
            } catch (IOException e) {
                System.out.println("Jogador " + playerId + " desconectado.");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (players) {
                    players.remove(this);
                    System.out.println("Jogador removido. Total de jogadores: " + players.size());
                    gameInProgress = false;
                }
            }
        }

        // Próximo turno
        private void nextTurn() {
            synchronized (players) {
                // Verifica se todos os jogadores terminaram suas jogadas
                if (allPlayersDone()) {
                    determineWinner();
                } else {
                    // Avança para o próximo jogador que ainda não terminou
                    currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
                    ClientHandler nextPlayer = players.get(currentPlayerIndex);
                    if (!nextPlayer.isTurnDone) {
                        nextPlayer.sendMessage("YOUR_TURN");
                        System.out.println("É a vez do Jogador " + nextPlayer.playerId);
                    } else {
                        // Se o próximo jogador já terminou, verifica novamente
                        nextTurn();
                    }
                }
            }
        }

        // Verifica se todos os jogadores terminaram suas jogadas
        private boolean allPlayersDone() {
            for (ClientHandler player : players) {
                if (!player.isTurnDone) {
                    return false;
                }
            }
            return true;
        }

        // Determinar vencedor e atualizar vidas
        private void determineWinner() {
            System.out.println("Determinando o vencedor da rodada " + currentRound);
            if (players.size() < 2) {
                // Caso um jogador tenha desconectado
                System.out.println("Um jogador desconectou. Encerrando o jogo.");
                return;
            }

            ClientHandler player1 = players.get(0);
            ClientHandler player2 = players.get(1);

            int total1 = player1.total;
            int total2 = player2.total;

            player1.sendMessage("OPPONENT_TOTAL " + total2);
            player2.sendMessage("OPPONENT_TOTAL " + total1);

            String result1 = "";
            String result2 = "";

            int livesAtStake = currentRound; // Vidas em jogo nesta rodada

            if (total1 > 21 && total2 > 21) {
                result1 = "RESULT Ambos estouraram. Empate.";
                result2 = "RESULT Ambos estouraram. Empate.";
                // Sem alteração nas vidas
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
                    // Sem alteração nas vidas
                }
            }

            player1.sendMessage(result1);
            player2.sendMessage(result2);

            // Enviar informações de vidas atualizadas
            player1.sendMessage("LIVES " + player1.lives);
            player1.sendMessage("OPPONENT_LIVES " + player2.lives);

            player2.sendMessage("LIVES " + player2.lives);
            player2.sendMessage("OPPONENT_LIVES " + player1.lives);

            // Verificar se algum jogador atingiu 0 vidas
            if (player1.lives <= 0 || player2.lives <= 0) {
                // Jogo finalizado
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

                gameInProgress = false;
                System.out.println("Jogo finalizado.");
            } else {
                // Iniciar automaticamente a próxima rodada
                currentRound++;
                resetGame();
            }
        }

        // Método para atualizar vidas
        private void updateLives(ClientHandler loser, ClientHandler winner, int livesAtStake) {
            loser.lives -= livesAtStake;
            winner.lives += livesAtStake;
            // Garantir que as vidas não fiquem negativas
            if (loser.lives < 0) {
                loser.lives = 0;
            }
            System.out.println("Jogador " + loser.playerId + " perdeu " + livesAtStake + " vida(s). Vidas restantes: " + loser.lives);
            System.out.println("Jogador " + winner.playerId + " ganhou " + livesAtStake + " vida(s). Vidas totais: " + winner.lives);
        }

        // Encerra o jogo para todos os jogadores
        private void endGameForAll() {
            for (ClientHandler player : players) {
                player.sendMessage("GAME_ENDED");
                player.inGame = false;
            }
            gameInProgress = false;
        }

        // Método para resetar o estado do jogo para uma nova rodada
        private void resetGame() {
            System.out.println("Resetando o jogo para a rodada " + currentRound);
            deck = new Deck();
            deck.shuffle();
            gameInProgress = true;

            synchronized (players) {
                for (ClientHandler player : players) {
                    player.hand.clear();
                    player.isTurnDone = false;
                    player.updateTotal();
                    player.sendMessage("ROUND " + currentRound + ". Vale " + currentRound + " vida(s).");

                    // Distribuir novas cartas
                    // Carta visível
                    Card visibleCard = deck.drawCard();
                    player.visibleCard = visibleCard;
                    player.sendMessage("VISIBLE_CARD " + visibleCard);

                    // Carta oculta
                    Card hiddenCard = deck.drawCard();
                    player.hiddenCard = hiddenCard;
                    player.sendMessage("HIDDEN_CARD " + hiddenCard);

                    // Atualiza total
                    player.updateTotal();
                }

                // Envia informações das cartas visíveis aos jogadores
                for (ClientHandler player : players) {
                    ClientHandler opponent = getOpponent(player);
                    player.sendMessage("OPPONENT_VISIBLE_CARD " + opponent.visibleCard);
                    player.sendMessage("LIVES " + player.lives);
                    player.sendMessage("OPPONENT_LIVES " + opponent.lives);
                }

                // Notifica o primeiro jogador que é sua vez
                currentPlayerIndex = 0; // Reinicia com o primeiro jogador
                if (players.size() > 0) {
                    players.get(currentPlayerIndex).sendMessage("YOUR_TURN");
                    System.out.println("Rodada " + currentRound + " iniciada. É a vez do Jogador " + players.get(currentPlayerIndex).playerId);
                }
            }
        }
    }

    // Método para calcular o valor da mão
    private int calculateHandValue(List<Card> hand) {
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
        while (total > 21 && aces > 0) { // Se estourar e houver Ás, converter Ás de 11 para 1
            total -= 10;
            aces--;
        }
        return total;
    }

    // Classe para representar o baralho e as cartas
    class Deck {
        private List<Card> cards;

        public Deck() {
            cards = new ArrayList<>();
            for (String suit : new String[]{"Copas", "Ouros", "Espadas", "Paus"}) {
                for (int i = 1; i <= 13; i++) {
                    cards.add(new Card(i, suit));
                }
            }
        }

        public void shuffle() {
            Collections.shuffle(cards);
        }

        public Card drawCard() {
            if (cards.isEmpty()) {
                // Reembaralhar se o baralho acabar
                System.out.println("Reembaralhando o baralho.");
                for (String suit : new String[]{"Copas", "Ouros", "Espadas", "Paus"}) {
                    for (int i = 1; i <= 13; i++) {
                        cards.add(new Card(i, suit));
                    }
                }
                shuffle();
            }
            return cards.remove(0);
        }
    }

    class Card {
        private int number;
        private String suit;

        public Card(int number, String suit) {
            this.number = number;
            this.suit = suit;
        }

        public int getValue() {
            if (number > 10) {
                return 10;
            } else if (number == 1) {
                return 1; // Ás será tratado no cálculo da mão
            } else {
                return number;
            }
        }

        @Override
        public String toString() {
            String name;
            switch (number) {
                case 1:
                    name = "Ás";
                    break;
                case 11:
                    name = "Valete";
                    break;
                case 12:
                    name = "Dama";
                    break;
                case 13:
                    name = "Rei";
                    break;
                default:
                    name = String.valueOf(number);
            }
            return name + " de " + suit;
        }
    }
}
