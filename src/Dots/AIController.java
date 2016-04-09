package Dots;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.json.Json;
import javax.json.JsonObject;

public class AIController extends Player implements Runnable {
	public static final AIController INSTANCE = new AIController();

	private ConcurrentLinkedQueue<GameController> m_games;
	private Thread m_thread;

	AIController() {
		initialize(Type.AI);
		m_id = 2; // AI is always Player 2

		m_games = new ConcurrentLinkedQueue<GameController>();
		m_thread = null;
	}

	@Override
	public void run() {
		while (!m_games.isEmpty()) {
			GameController game = m_games.poll();
			if (game == null)
				continue;

			int row = -1;
			int col = -1;
			String side = null;

			GameBoard board = game.getBoard();
			// Evaluate board to determine best move
			// If move takes a box, add {r: #, c: #} to boxes

			if (row == -1 || col == -1 || Util.isEmpty(side)) {
				m_games.offer(game); // Try again
				continue;
			}

			game.send(this, Json.createObjectBuilder()
				.add("type", "move")
				.add("line", Json.createObjectBuilder()
					.add("r", row)
					.add("c", col)
					.add("side", side)
				.build())
			.build());
		}

		m_thread = null;
	}

	@Override
	public void send(GameController game, JsonObject content) {
		if (content.getString("type").equals("move")) {
			m_games.offer(game);
			restart();
		} else if (!content.getString("type").equals("init"))
			m_games.remove(game);
	}

	@Override
	public User getUser() {
		return null;
	}

	@Override
	public void restart() {
		if (m_thread != null)
			return;

		(m_thread = new Thread(this)).start();
	}
}
