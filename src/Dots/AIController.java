package Dots;

import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.json.Json;
import javax.json.JsonObject;

public class AIController extends Player implements Runnable {
	public static final AIController INSTANCE = new AIController();

	private ConcurrentLinkedQueue<GameController> m_games;
	private Thread m_thread;
	private boolean AITakeover = false;

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
							
							System.out.println("Best");
						}
					}
				}

				// Pick a random open spot (ONLY IF I DIDN'T ALREADY MARK A BOX)
				if (Util.isEmpty(side)) {
					int myCounter = 0; //no infinite loops when I get caught
					while (Util.isEmpty(side) || board.isMarked(row, col, side) || board.makesCapturable(row, col, side) != null) {
						myCounter++;
						if (myCounter > 40000) {
							System.out.println("Counter exceeded");
							side = null;
							AITakeover = true;
							break;
						}
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
				}

				boolean dumb = false;
				
				if (dumb) {
					//for now, pick a sequential number (AI to come)
					for (int i = 0; i < 10 && Util.isEmpty(side); ++i) {
						for (int j = 0; j < 10 && Util.isEmpty(side); ++j) {
							int topMark = board.isMarked(i, j, "t") ? 1 : 0;
							int rightMark = board.isMarked(i, j, "r") ? 1 : 0;
							int bottomMark = board.isMarked(i, j, "b") ? 1 : 0;
							int leftMark = board.isMarked(i, j, "l") ? 1 : 0;
							row = i;
							col = j;
							
							System.out.println("shouldn't happen much");

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
				} else {
					//AI Compare alternatives, pick the one that leaves the lowest opponent score
					
					if (Util.isEmpty(side)) {
						System.out.println("AI!!");
						AIMove bestMove = getBestMove(board, 1);
						row = bestMove.row;
						col = bestMove.col;
						side = bestMove.side;
						System.out.println("AI Done, move: " + row + ", " + col + ", " + side);
					}
				}

					
//					m_games.offer(game); // Try again
//					continue;


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
	
	//finds the move that yields the smallest number of boxes to the opposing player (i.e. minimax)
	private AIMove getBestMove(GameBoard board, int opponent) {
		
		//min move
		int row = -1;
		int col = -1;
		String side = null;
		
		int minOpponentScore = 1000; //TODO
		
		for (int i = 0; i < 10; ++i) {
			for (int j = 0; j < 10; ++j) {
				int topMark = board.isMarked(i, j, "t") ? 1 : 0;
				int rightMark = board.isMarked(i, j, "r") ? 1 : 0;
				int bottomMark = board.isMarked(i, j, "b") ? 1 : 0;
				int leftMark = board.isMarked(i, j, "l") ? 1 : 0;

				//for top, right, bottom, left; if they're not marked

				if (topMark == 0) {
					
					//evaluate how many boxes it captures
					int opponentScore = evaluate(board.duplicate(), new AIMove(i, j, "t"), opponent);
					
					if (opponentScore < minOpponentScore) {
						minOpponentScore = opponentScore;
						row = i;
						col = j;
						side = "t";
					}
				}
					
				if (rightMark == 0) {
					//evaluate how many boxes it captures
					int opponentScore = evaluate(board.duplicate(), new AIMove(i, j, "r"), opponent);
					
					if (opponentScore < minOpponentScore) {
						minOpponentScore = opponentScore;
						row = i;
						col = j;
						side = "r";
					}
				}
				
				if (bottomMark == 0) {
					//evaluate how many boxes it captures
					int opponentScore = evaluate(board.duplicate(), new AIMove(i, j, "b"), opponent);
					
					if (opponentScore < minOpponentScore) {
						minOpponentScore = opponentScore;
						row = i;
						col = j;
						side = "b";
					}
				}
				
				if (leftMark == 0) {
					//evaluate how many boxes it captures
					int opponentScore = evaluate(board.duplicate(), new AIMove(i, j, "l"), opponent);
					
					if (opponentScore < minOpponentScore) {
						minOpponentScore = opponentScore;
						row = i;
						col = j;
						side = "l";
					}
				}
			}
		}
		
		return new AIMove(row, col, side);
	}
	
	//evaluates how many boxes the opponent can capture if the opponent makes a given move
	//NOTE: call on duplicates!
	private int evaluate(GameBoard board, AIMove move, int player) {
		
		Vector<AIMove> moves = board.makesCapturable(move.row, move.col, move.side);
		
		board.mark(move.row, move.col, move.side, player, (result, captured) -> {
			result.add("current", player);
		});
		
		if (moves == null) {			
			return board.getScore(player);
		}
		
		int opponentScore = board.getScore(player);
		
		for (AIMove m : moves) {
			opponentScore = evaluate(board, m, player);
		}
		
		return opponentScore;	
	}
	
}