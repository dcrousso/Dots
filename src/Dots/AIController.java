package Dots;

import javax.json.Json;
import javax.json.JsonObject;

public class AIController extends Player implements Runnable {
	private Thread m_thread;
	private GameBoard m_board;

	AIController() {
		initialize(Type.AI);
		m_game = null;
		m_board = null;

		restart();
	}

	@Override
	public void run() {
		while (!m_error) {
			if (m_board == null)
				continue;

			int row = -1;
			int col = -1;
			String side = null;

			// Evaluate board to determine best move
			// If move takes a box, add {r: #, c: #} to boxes

			if (row == -1 || col == -1 || Util.isEmpty(side))
				continue; // Try again

			m_game.send(this, Json.createObjectBuilder()
				.add("type", "move")
				.add("line", Json.createObjectBuilder()
					.add("r", row)
					.add("c", col)
					.add("side", side)
				.build())
			.build());

			m_board = null;
		}
	}

	@Override
	public void send(JsonObject content) {
		if (content.getString("type").matches("\\bleave\\b|\\bend\\b"))
			m_error = true;
		else if (m_game != null)
			m_board = m_game.getBoard();
	}

	@Override
	public User getUser() {
		return null;
	}

	@Override
	public void restart() {
		if (m_thread == null)
			m_thread = new Thread(this);

		if (!m_thread.isAlive())
			m_thread.start();
	}
}
