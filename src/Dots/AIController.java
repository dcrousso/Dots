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
		try {
			while (!m_games.isEmpty()) {
				Thread.sleep(200);

				GameController game = m_games.poll();
				if (game == null)
					continue;

				int row = -1;
				int col = -1;
				String side = null;

				GameBoard board = game.getBoard();

				for (int i = 0; i < 10 && side == null; ++i) {
					for (int j = 0; j < 10 && side == null; ++j) {
						int topMark = 0;
						if (board.getMark(i, j, "t") != 0)
							topMark = 1;

						int bottomMark = 0;
						if (board.getMark(i, j, "b") != 0)
							bottomMark = 1;

						int leftMark = 0;
						if (board.getMark(i, j, "l") != 0)
							leftMark = 1;

						int rightMark = 0;
						if (board.getMark(i, j, "r") != 0)
							rightMark = 1;

						if (topMark + bottomMark + leftMark + rightMark == 3) {
							row = i;
							col = j;

							if (topMark == 0)
								side = "t";
							else if (bottomMark == 0)
								side = "b";
							else if (leftMark == 0)
								side = "l";
							else if (rightMark == 0)
								side = "r";
						}
					}
				}

				// Pick the first open spot
				for (int i = 0; i < 10 && side == null; ++i) {
					for (int j = 0; j < 10 && side == null; ++j) {
						row = i;
						col = j;
						if (board.getMark(i, j, "t") == 0)
							side = "t";
						else if (board.getMark(i, j, "b") == 0)
							side = "b";
						else if (board.getMark(i, j, "l") == 0)
							side = "l";
						else if (board.getMark(i, j, "r") == 0)
							side = "r";
					}
				}

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
		} catch (InterruptedException e) {
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