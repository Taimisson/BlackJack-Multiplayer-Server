import java.io.*;
import java.net.*;
import java.util.Scanner;

public class BlackJackClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 12345;
    private boolean inGame = true;
    private int playerId;
    private volatile boolean myTurn = false; // Flag para indicar se é a vez do jogador

    public static void main(String[] args) {
        new BlackJackClient().startClient();
    }

    public void startClient() {
        try (
                Socket socket = new Socket(SERVER_ADDRESS, PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            Scanner scanner = new Scanner(System.in);

            // Thread para ouvir mensagens do servidor
            Thread listenerThread = new Thread(() -> {
                String serverMessage;
                try {
                    while ((serverMessage = in.readLine()) != null && inGame) {
                        if (serverMessage.startsWith("PLAYER_ID")) {
                            playerId = Integer.parseInt(serverMessage.substring(10));
                            System.out.println("Você é o Jogador " + playerId);
                        } else if (serverMessage.startsWith("VISIBLE_CARD")) {
                            System.out.println("Sua carta visível: " + serverMessage.substring(13));
                        } else if (serverMessage.startsWith("HIDDEN_CARD")) {
                            System.out.println("Sua carta oculta: " + serverMessage.substring(12));
                        } else if (serverMessage.startsWith("OPPONENT_VISIBLE_CARD")) {
                            System.out.println("Carta visível do oponente: " + serverMessage.substring(21));
                        } else if (serverMessage.startsWith("LIVES")) {
                            System.out.println("Suas vidas: " + serverMessage.substring(6));
                        } else if (serverMessage.startsWith("OPPONENT_LIVES")) {
                            System.out.println("Vidas do oponente: " + serverMessage.substring(15));
                        } else if (serverMessage.startsWith("ROUND")) {
                            System.out.println(serverMessage);
                        } else if (serverMessage.equals("YOUR_TURN")) {
                            System.out.println("É a sua vez!");
                            myTurn = true; // Define a flag como true
                        } else if (serverMessage.startsWith("CARD")) {
                            System.out.println("Você recebeu: " + serverMessage.substring(5));
                        } else if (serverMessage.startsWith("TOTAL")) {
                            System.out.println("Total atual: " + serverMessage.substring(6));
                        } else if (serverMessage.equals("BUST")) {
                            System.out.println("Você estourou!");
                        } else if (serverMessage.equals("STAND")) {
                            System.out.println("Você passou a vez.");
                        } else if (serverMessage.equals("NOT_YOUR_TURN")) {
                            System.out.println("Não é a sua vez.");
                        } else if (serverMessage.startsWith("OPPONENT_TOTAL")) {
                            System.out.println("Total do oponente: " + serverMessage.substring(15));
                        } else if (serverMessage.startsWith("RESULT")) {
                            System.out.println(serverMessage.substring(7));
                        } else if (serverMessage.startsWith("GAME_OVER")) { // Verifica "GAME_OVER" no início da mensagem
                            inGame = false;
                            System.out.println("Fim do jogo.");
                        } else if (serverMessage.equals("GAME_ENDED")) {
                            System.out.println("O jogo terminou.");
                            inGame = false;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Desconectado do servidor.");
                }
            });
            listenerThread.start();

            // Thread principal para lidar com a entrada do usuário
            while (inGame) {
                if (myTurn) {
                    System.out.println("Digite 'HIT' para pedir outra carta ou 'STAND' para passar a vez.");
                    String input = scanner.nextLine();
                    if (input.equalsIgnoreCase("HIT") || input.equalsIgnoreCase("STAND")) {
                        out.println(input.toUpperCase()); // Envia a ação para o servidor
                        myTurn = false; // Após enviar a ação, define a flag como false
                    } else {
                        System.out.println("Entrada inválida.");
                    }
                }

                // Pequena pausa para evitar loop ocupado
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Aguarda a thread de escuta terminar
            try {
                listenerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Obrigado por jogar!");

        } catch (IOException e) {
            System.out.println("Erro ao conectar-se ao servidor.");
        }
    }
}
