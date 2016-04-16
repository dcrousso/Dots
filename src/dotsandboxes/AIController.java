package dotsandboxes;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.json.Json;
import javax.json.JsonObject;

import dotsandboxes.GameBoard.Move;

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
				Thread.sleep(250);

				GameController game = m_games.poll();
				if (game == null)
					continue;

				int row = -1;
				int col = -1;
				String side = null;

				GameBoard board = game.getBoard();

				// Find any capturable boxes and take them
				for (int r = 0; r < board.getRows() && Util.isEmpty(side); ++r) {
					for (int c = 0; c < board.getCols() && Util.isEmpty(side); ++c) {
						HashSet<Move> capturable = board.isCapturable(r, c);
						if (!capturable.isEmpty()) {
							Move line = capturable.iterator().next();
							row = line.row;
							col = line.col;
							side = line.side;
						}
					}
				}

				// No boxes capturable, so pick a random spot
				if (Util.isEmpty(side)) {
					row = -1;
					col = -1;
					side = null;

					int attempts = 0;
					do {
						if (++attempts > board.getRows() * board.getCols() * 100) { // Limit the number of tries
							side = null;
							break;
						}

						row = (int) Math.floor(Math.random() * board.getRows());
						col = (int) Math.floor(Math.random() * board.getCols());
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
					} while (board.isMarked(row, col, side) || !board.makesCapturable(row, col, side).isEmpty());
				}

				// No non-capturing moves left, so evaluate the best option
				if (Util.isEmpty(side)) {
					GameBoard.Move move = getBestMove(board, 1); // Opponent is always player 1
					row = move.row;
					col = move.col;
					side = move.side;
				}

				// Unable to find a move
				if (Util.isEmpty(side)) {
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

	// Finds the move that yields the smallest number of boxes to the opposing player (i.e. minimax)
	private GameBoard.Move getBestMove(GameBoard board, int opponent) {
		int row = -1;
		int col = -1;
		String side = null;

		int minOpponentScore = Integer.MAX_VALUE;

		for (int r = 0; r < board.getRows(); ++r) {
			for (int c = 0; c < board.getCols(); ++c) {
				if (!board.isMarked(r, c, "t")) {
					int opponentScore = evaluate(board.duplicate(), new GameBoard.Move(r, c, "t"), opponent);
					if (opponentScore < minOpponentScore) {
						minOpponentScore = opponentScore;
						row = r;
						col = c;
						side = "t";
					}
				}

				if (!board.isMarked(r, c, "r")) {
					int opponentScore = evaluate(board.duplicate(), new GameBoard.Move(r, c, "r"), opponent);
					if (opponentScore < minOpponentScore) {
						minOpponentScore = opponentScore;
						row = r;
						col = c;
						side = "r";
					}
				}

				if (!board.isMarked(r, c, "b")) {
					int opponentScore = evaluate(board.duplicate(), new GameBoard.Move(r, c, "b"), opponent);
					if (opponentScore < minOpponentScore) {
						minOpponentScore = opponentScore;
						row = r;
						col = c;
						side = "b";
					}
				}

				if (!board.isMarked(r, c, "l")) {
					int opponentScore = evaluate(board.duplicate(), new GameBoard.Move(r, c, "l"), opponent);
					if (opponentScore < minOpponentScore) {
						minOpponentScore = opponentScore;
						row = r;
						col = c;
						side = "l";
					}
				}
			}
		}

		return new GameBoard.Move(row, col, side);
	}

	// Evaluates how many boxes the opponent can capture if the opponent makes a given move
	private int evaluate(GameBoard board, GameBoard.Move move, int player) {
		HashSet<GameBoard.Move> moves = board.makesCapturable(move.row, move.col, move.side);
		board.mark(move.row, move.col, move.side, player, null);
		return moves.parallelStream().mapToInt(m -> evaluate(board, m, player)).max().orElse(board.getScore(player));
	}
}