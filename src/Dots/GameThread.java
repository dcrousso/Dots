package Dots;

import java.util.Arrays;
import java.util.Vector;

import javax.json.JsonObject;

public class GameThread extends Thread {
	private Vector<WebSocket> m_players;
	private WebSocket m_current;
	private int m_uncaptured;

	public GameThread(WebSocket ...players) {
		m_players = new Vector<WebSocket>(Arrays.asList(players));
		m_current = m_players.firstElement();

		String game = Util.getFileContents("/DotsData/board.json");
		m_uncaptured = Util.count(game, "\"b\"");

		for (int i = 0; i < m_players.size(); ++i) {
			WebSocket player = m_players.get(i);
			player.setId(i + 1);
			player.send(Util.parseJSON(game)
				.add("player", player.getId())
			.build());
		}
	}

	public void send(WebSocket caller, JsonObject content) {
		if (m_uncaptured <= 0 || caller != m_current)
			return;

		m_players.parallelStream().forEach(player -> player.send(content));

		if (content.containsKey("boxes")) {
			int captured = content.getJsonArray("boxes").size();
			if (captured == 0)
				m_current = m_players.get((m_players.indexOf(m_current) + 1) % m_players.size());
			else
				m_uncaptured -= captured;
	
			if (m_uncaptured <= 0) {
				// TODO: save points for each person to database
			}
		}
	}

	public void run() {
		while (m_players.parallelStream().allMatch(ws -> ws.isAlive())) {
		}
	}
}
