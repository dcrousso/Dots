package Dots;

import java.util.Arrays;
import java.util.Vector;

import javax.json.Json;
import javax.json.JsonObject;

public class GameController {
	private Vector<Player> m_players;
	private int m_current;
	private GameBoard m_board;

	public GameController(Player ...players) {
		m_players = new Vector<Player>(Arrays.asList(players));
		m_current = 0;

		String game = Util.getFileContents("/DotsData/board.json");
		for (int i = 0; i < m_players.size(); ++i) {
			Player player = m_players.get(i);
			player.setId(i + 1);
			player.send(Util.parseJSON(game)
				.add("player", player.getId())
			.build());
		}
		m_board = new GameBoard(Util.parseJSON(game).build().getJsonArray("board"), m_players);
	}

	public void send(Player caller, JsonObject content) {
		switch (content.getString("type")) {
		case "move":
			if (!m_board.hasUncaptured() || caller != m_players.get(m_current))
				break;

			JsonObject line = (JsonObject) content.getJsonObject("line");
			JsonObject response = m_board.mark(line.getInt("r"), line.getInt("c"), line.getString("side"), m_current + 1);  // Player IDs start at 1
			if (response == null)
				break;

			if (response.getJsonArray("boxes").size() == 0)
				m_current = (m_current + 1) % m_players.size();

			m_players.parallelStream().forEach(player -> player.send(response));

			if (!m_board.hasUncaptured()) {
				int winner = m_board.getWinner();

				JsonObject end = Json.createObjectBuilder()
					.add("type", "end")
					.add("winner", winner)
				.build();

				for (int i = 0; i < m_players.size(); ++i) {
					Player player = m_players.get(i);
					player.send(end);

					User user = (User) player.getAttribute("user");
					if (user != null) {
						user.addGame(m_board.getScore(i), i == winner);
						Util.update("UPDATE users SET played = ?, won = ?, points = ? WHERE username = ?", new String[] {
							Integer.toString(user.getGamesPlayed()),
							Integer.toString(user.getGamesWon()),
							Integer.toString(user.getPoints()),
							user.getUsername()
						});
					}
				}
			}

			break;
		case "leave":
			if (m_players.parallelStream().noneMatch(player -> ((User) player.getAttribute("user")) == null)) { // All players must be logged in
				// TODO: Save current game state (m_board) to a file for playing later
				// Create UID for game and save the value of m_board using JSON to /DotsData/<UID>.json
				m_players.parallelStream().forEach(player -> {
					User user = (User) player.getAttribute("user");
					if (user == null)
						return;

					// Add UID to the savedGame column of the database
				});
			}

			m_players.parallelStream().forEach(player -> player.send(content));
			break;
		case "restart":
			if (!caller.isAlive() || (m_board.hasUncaptured() && m_players.parallelStream().noneMatch(player -> !player.isAlive())))
				break;

			caller.restart();
			caller.send(content);
			break;
		}
	}

	public GameBoard getBoard() {
		return m_board;
	}
}
