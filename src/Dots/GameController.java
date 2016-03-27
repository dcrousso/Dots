package Dots;

import java.util.Arrays;
import java.util.Vector;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import Dots.Player.Type;

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
			m_board.mark(line.getInt("r"), line.getInt("c"), line.getString("side"), m_current);

			JsonArray boxes = (JsonArray) content.getJsonArray("boxes");
			if (boxes.size() == 0)
				m_current = (m_current + 1) % m_players.size();
			else {
				boxes.parallelStream().forEach(item -> {
					JsonObject box = (JsonObject) item;
					m_board.capture(box.getInt("r"), box.getInt("c"), m_current);
				});
			}

			if (!m_board.hasUncaptured()) {
				int winner = m_board.getWinner();

				JsonObject end = Json.createObjectBuilder()
					.add("type", "end")
					.add("winner", winner + 1)
				.build();

				for (int i = 0; i < m_players.size(); ++i) {
					Player player = m_players.get(i);
					player.send(end);

					if (player.getType() == Type.AI)
						continue;

					User user = (User) player.getAttribute("user");
					if (user == null)
						continue;

					user.addGame(m_board.getScore(i), i == winner);
					Util.update("UPDATE users SET played = ?, won = ?, points = ? WHERE username = ?", new String[] {
						Integer.toString(user.getGamesPlayed()),
						Integer.toString(user.getGamesWon()),
						Integer.toString(user.getPoints()),
						user.getUsername()
					});
				}
			}

			m_players.parallelStream().forEach(player -> player.send(content));
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
			if (!m_board.hasUncaptured() || m_players.parallelStream().anyMatch(player -> !player.isAlive()))
				m_players.parallelStream().forEach(player -> player.restart());

			m_players.parallelStream().forEach(player -> player.send(content));
			break;
		}
	}

	public GameBoard getBoard() {
		return m_board;
	}
}
