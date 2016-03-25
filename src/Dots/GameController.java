package Dots;

import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class GameController {
	private Vector<WebSocket> m_players;
	private int m_current;

	private ConcurrentSkipListMap<String, Integer> m_board;
	private int m_uncaptured;
	private Vector<Integer> m_scores;

	public GameController(WebSocket ...players) {
		m_players = new Vector<WebSocket>(Arrays.asList(players));
		m_current = 0;

		m_board = new ConcurrentSkipListMap<String, Integer>();
		String game = Util.getFileContents("/DotsData/board.json");
		JsonArray board = Util.parseJSON(game).build().getJsonArray("board");
		for (int r = 0; r < board.size(); ++r) {
			JsonArray row = (JsonArray) board.get(r);
			for (int c = 0; c < row.size(); ++c) {
				JsonObject cell = (JsonObject) row.get(c);
				String key = generateCellKey(r, c);
				m_board.put(key + "x", cell.getInt("x"));
				m_board.put(key + "t", cell.getInt("t"));
				m_board.put(key + "r", cell.getInt("r"));
				m_board.put(key + "b", cell.getInt("b"));
				m_board.put(key + "l", cell.getInt("l"));
			}
		}

		m_uncaptured = Util.count(game, "\"b\"");

		m_scores = new Vector<Integer>();
		m_players.parallelStream().forEach(player -> m_scores.add(0));

		for (int i = 0; i < m_players.size(); ++i) {
			WebSocket player = m_players.get(i);
			player.setId(i + 1);
			player.send(Util.parseJSON(game)
				.add("player", player.getId())
			.build());
		}
	}

	public void send(WebSocket caller, JsonObject content) {
		if (m_uncaptured <= 0 || caller != m_players.get(m_current))
			return;

		System.out.println(content.toString());
		m_players.parallelStream().forEach(player -> player.send(content));

		switch (content.getString("type")) {
		case "move":
			// {"type":"move","player":1,"line":{"r":3,"c":4,"side":"l"},"boxes":[]}
			JsonObject line = (JsonObject) content.getJsonObject("line");
			m_board.put(generateCellKey(line.getInt("r"), line.getInt("c")) + line.getString("side"), m_current);

			JsonArray boxes = (JsonArray) content.getJsonArray("boxes");
			if (boxes.size() == 0)
				m_current = (m_current + 1) % m_players.size();
			else {
				boxes.parallelStream().forEach(item -> {
					JsonObject box = (JsonObject) item;
					m_board.put(generateCellKey(box.getInt("r"), box.getInt("c")) + "x", m_current);
				});

				m_uncaptured -= boxes.size();
				m_scores.set(m_current, m_scores.get(m_current) + boxes.size());
			}

			if (m_uncaptured <= 0) {
				int winner = 0;
				for (int i = 1; i < m_scores.size(); ++i) {
					if (m_scores.get(i) > m_scores.get(winner))
						winner = i;
				}

				JsonObject end = Json.createObjectBuilder()
					.add("type", "end")
					.add("winner", winner + 1)
				.build();

				for (int i = 0; i < m_players.size(); ++i) {
					WebSocket player = m_players.get(i);
					player.send(end);

					User user = (User) player.getSessionAttribute("user");
					if (user == null)
						continue;

					user.addGame(m_scores.get(i), i == winner);
					Util.update("UPDATE users SET played = ?, won = ?, points = ? WHERE username = ?", new String[] {
						Integer.toString(user.getGamesPlayed()),
						Integer.toString(user.getGamesWon()),
						Integer.toString(user.getPoints()),
						user.getUsername()
					});
				}
			}
			break;
		case "leave":
			if (m_players.parallelStream().anyMatch(player -> ((User) player.getSessionAttribute("user")) == null)) // All players must be logged in
				break;

			// TODO: Save current game state (m_board) to a file for playing later
			// Create UID for game and save the value of m_board using JSON to /DotsData/<UID>.json
			m_players.parallelStream().forEach(player -> {
				User user = (User) player.getSessionAttribute("user");
				if (user == null)
					return;

				// Add UID to the games column of the database
			});
			break;
		}
	}

	private String generateCellKey(int row, int col) {
		return row + "." + col + ".";
	}
}
