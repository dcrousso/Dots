package dotsandboxes;

import java.util.Arrays;
import java.util.Vector;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class GameController {
	private Vector<Player> m_players;
	private int m_current;
	private GameBoard m_board;
	private boolean m_alive;

	public GameController(Player ...players) {
		m_players = new Vector<Player>(Arrays.asList(players));
		m_current = 0;

		for (int i = 0; i < m_players.size(); ++i) {
			Player player = m_players.get(i);
			player.setId(i + 1);
			player.send(this, Json.createObjectBuilder()
				.add("type", "init")
				.add("player", player.getId())
				.add("current", m_players.firstElement().getId())
			.build());
		}
		m_board = new GameBoard(Util.parseJSON(Defaults.EMPTY_BOARD).build().getJsonArray("board"), m_players);

		m_alive = true;
	}

	public void send(Player caller, JsonObject content) {
		switch (content.getString("type").toLowerCase()) {
		case "move":
			if (!m_board.hasUncaptured() || caller != m_players.get(m_current))
				break;

			JsonObject line = (JsonObject) content.getJsonObject("line");
			JsonObject response = m_board.mark(line.getInt("r"), line.getInt("c"), line.getString("side"), m_players.get(m_current).getId(), (result, captured) -> {
				if (!captured)
					m_current = (m_current + 1) % m_players.size();

				result.add("current", m_players.get(m_current).getId());
			});
			if (response == null)
				break;

			m_players.parallelStream().forEach(player -> {
				// Don't forward the move to an AI if they initiated it unless it captured a box
				if (player != caller || caller.getType() != Player.Type.AI || response.containsKey("boxes"))
					player.send(this, response);
			});

			if (!m_board.hasUncaptured()) {
				final int winner = m_board.getWinner();
				savePlayerInfo(winner);
				m_players.parallelStream().forEach(player -> {
					JsonObjectBuilder end = Json.createObjectBuilder()
						.add("type", "end")
						.add("winner", winner);

					User user = player.getUser();
					if (user != null)
						end.add("played", user.getGamesPlayed()).add("points", user.getPoints());

					player.send(this, end.build());
				});
			}

			break;
		case "leave":
			if (!m_alive)
				break;

			savePlayerInfo(-1); // No Winner
			m_players.parallelStream().forEach(player -> {
				JsonObjectBuilder leave = Json.createObjectBuilder()
					.add("type", "leave");

				User user = player.getUser();
				if (user != null)
					leave.add("played", user.getGamesPlayed()).add("points", user.getPoints());

				player.send(this, leave.build());
			});
			break;
		case "restart":
			if (!caller.isAlive() || (m_board.hasUncaptured() && m_players.parallelStream().noneMatch(player -> !player.isAlive())))
				break;

			caller.restart();
			break;
		}
	}

	public GameBoard getBoard() {
		return m_board;
	}

	private void savePlayerInfo(int winner) {
		m_players.forEach(player -> {
			User user = player.getUser();
			if (user == null)
				return;

			user.addGame(m_board.getScore(player.getId()), player.getId() == winner);
			Util.update("UPDATE users SET played = ?, won = ?, points = ? WHERE username = ?", new String[] {
				Integer.toString(user.getGamesPlayed()),
				Integer.toString(user.getGamesWon()),
				Integer.toString(user.getPoints()),
				user.getUsername()
			});
		});
		m_alive = false;
	}
}
