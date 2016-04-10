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

				for (int i = 0; i < 10 && Util.isEmpty(side); ++i) {
					for (int j = 0; j < 10 && Util.isEmpty(side); ++j) {
						int topMark = board.isMarked(i, j, "t") ? 1 : 0;
						int rightMark = board.isMarked(i, j, "r") ? 1 : 0;
						int bottomMark = board.isMarked(i, j, "b") ? 1 : 0;
						int leftMark = board.isMarked(i, j, "l") ? 1 : 0;
						if (topMark + rightMark + bottomMark + leftMark == 3) {
							row = i;
							col = j;

							if (topMark == 0)
								side = "t";
							else if (rightMark == 0)
								side = "r";
							else if (bottomMark == 0)
								side = "b";
							else if (leftMark == 0)
								side = "l";
						}
					}
				}

				// Pick a random open spot
				while (board.isMarked(row, col, side) || Util.isEmpty(side)) {
					row = (int) Math.floor(Math.random() * 10);
					col = (int) Math.floor(Math.random() * 10);
					switch ((int) Math.floor(Math.random() * 4)) {
					case 0:
						side = "t";
						break;
					case 1:
						side = "r";
						break;
					case 2:
						side = "b";
						break;
					case 3:
						side = "l";
						break;
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