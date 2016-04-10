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
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			GameController game = m_games.poll();
			if (game == null)
				continue;
			
			int row = -1;
			int col = -1;
			String side = null;

			GameBoard board = game.getBoard();
			// Evaluate board to determine best move
			// If move takes a box, add {r: #, c: #} to boxes
			
			boolean chosen = false;
			
			for (int i = 0; i < 10; ++i) {
				if (chosen) {
					break;
				}
				for (int j = 0; j < 10; ++j) {
					int topMark = 0;
					int bottomMark = 0;
					int leftMark = 0;
					int rightMark = 0;
					if (board.getMark(i, j, "t") != 0) {
						topMark = 1;
					}
					if (board.getMark(i, j, "b") != 0) {
						bottomMark = 1;
					}
					if (board.getMark(i, j, "l") != 0) {
						leftMark = 1;
					}
					if (board.getMark(i, j, "r") != 0) {
						rightMark = 1;
					}
					
					int sum = topMark + bottomMark + leftMark + rightMark;
					
					if (sum == 4) {
						continue;
					} else if (sum == 3) {
						if (topMark == 0) {
							row = i;
							col = j;
							side = "t";
							chosen = true;
							break;
						} else if (bottomMark == 0) {
							row = i;
							col = j;
							side = "b";
							chosen = true;
							break;
						} else if (leftMark == 0) {
							row = i;
							col = j;
							side = "l";
							chosen = true;
							break;
						} else {
							row = i;
							col = j;
							side = "r";
							chosen = true;
							break;
						}
					} else {
						continue;
					}
				}
			}
			if (!chosen) { //pick the first open spot
				for (int i = 0; i < 10; ++i) {
					if (chosen) {
						break;
					}
					for (int j = 0; j < 10; ++j) {
						if (board.getMark(i, j, "t") == 0) {
							row = i;
							col = j;
							side = "t";
							chosen = true;
							break;
						} else if (board.getMark(i, j, "b") == 0) {
							row = i;
							col = j;
							side = "b";
							chosen = true;
							break;
						} else if (board.getMark(i, j, "l") == 0) {
							row = i;
							col = j;
							side = "l";
							chosen = true;
							break;
						} else if (board.getMark(i, j, "r") == 0) {
							row = i;
							col = j;
							side = "r";
							chosen = true;
							break;
						} else {
							continue;
						}
					}
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