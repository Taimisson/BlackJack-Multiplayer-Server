# BlackJack Multiplayer Server

## Project Overview
This project is a simple multiplayer BlackJack game server built using Java. The server supports multiple clients (players), and implements the basic rules of BlackJack, including hit, stand, and hand value calculation. The game progresses through rounds, and each player can gain or lose lives based on the results of each round. The game is played between two players, and additional players can join once a game has ended.

## Features
- **Multiplayer Support**: Up to 2 players can join and play a game simultaneously.
- **Turn-Based Gameplay**: The game alternates turns between players, and each player can choose to `HIT` (draw another card) or `STAND` (hold).
- **Lives System**: Each player starts with 5 lives. Lives are wagered each round, and players gain or lose lives based on the outcome.
- **Deck Management**: The deck is shuffled at the start of each game and re-shuffled when empty.
- **Card Value Calculation**: Cards have face values, and aces can count as either 1 or 11 to avoid busting.
- **Winner Determination**: After each round, the winner is determined based on the card totals. If both players bust, the round is a draw.

## How to Run

### Server
1. Compile and run the `BlackJackServer` class.
   ```bash
   javac server/BlackJackServer.java
   java server.BlackJackServer
   ```
2. The server will start on port `12345`. Players will be able to connect once the server is running.

### Client
1. Compile and run the `BlackJackClient` class.
   ```bash
   javac client/BlackJackClient.java
   java client.BlackJackClient
   ```
2. The client will attempt to connect to the server on `localhost:12345`. Ensure the server is running before starting the client.
3. Once connected, you will receive a `PLAYER_ID` and start playing once two players are connected.

## Game Rules
- **Objective**: Get a hand value closest to 21 without exceeding it.
- **HIT**: Draw an additional card to increase the total hand value.
- **STAND**: End the turn and keep the current hand value.
- **Bust**: If the total hand value exceeds 21, the player loses the round.
- **Lives**: Players start with 5 lives, and lives are gained or lost based on round outcomes. When a player loses all lives, the game ends.
- **Rounds**: Each round, players wager lives equal to the current round number.

## Server Structure

- **BlackJackServer.java**: The main server class that accepts connections and manages game flow.
- **ClientHandler.java**: Handles communication with a connected client (player).
- **Deck.java**: Manages the deck of cards, including shuffling and drawing cards.
- **Card.java**: Represents a playing card, including suit and value.

## Client Structure

- **BlackJackClient.java**: Handles communication with the server, sends player actions, and receives game updates.

## Example Gameplay Flow

1. Both players connect to the server.
2. Each player is dealt two cards, one visible and one hidden.
3. The first player takes their turn, either hitting or standing.
4. The second player takes their turn.
5. The winner is determined based on total hand value (closest to 21 without busting).
6. Lives are adjusted based on the round result.
7. The game continues to the next round until one player loses all their lives.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

## Author
This project was created by Taimisson de Carvalho Schardosim. Contributions and feedback are welcome.
