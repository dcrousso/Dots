package Dots;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class GameBoard {
	private ConcurrentHashMap<String, Integer> m_board;
	private int m_uncaptured;
	private ConcurrentHashMap<Integer, Integer> m_scores;

	public GameBoard(JsonArray board, Vector<Player> players) {
		m_board = new ConcurrentHashMap<String, Integer>();
		m_uncaptured = Util.count(board.toString(), "\"b\"\\s*:\\s*0");
		m_scores = new ConcurrentHashMap<Integer, Integer>();

		players.parallelStream().forEach(player -> m_scores.put(player.getId(), 0));

		for (int r = 0; r < board.size(); ++r) {
			JsonArray row = (JsonArray) board.get(r);
			for (int c = 0; c < row.size(); ++c) {
				JsonObject cell = (JsonObject) row.get(c);
				String key = generateCellKey(r, c);
				m_board.put(key + "t", cell.getInt("t"));
				m_board.put(key + "r", cell.getInt("r"));
				m_board.put(key + "b", cell.getInt("b"));
				m_board.put(key + "l", cell.getInt("l"));

				int captured = cell.getInt("x");
				m_board.put(key + "x", captured);
				if (captured != 0)
					m_scores.put(captured, getScore(captured) + 1);
			}
		}
	}

	public int getMark(int row, int col, String side) {
		if (Util.isEmpty(side))
			return 0;

		String key = generateCellKey(row, col) + side;
		if (!m_board.containsKey(key))
			return 0;

		return m_board.get(key);
	}

	public boolean isMarked(int row, int col, String side) {
		return getMark(row, col, side) != 0;
	}

	public JsonObject mark(int row, int col, String side, int player, BiConsumer<JsonObjectBuilder, Boolean> callback) {
		if (isMarked(row, col, side))
			return null;

		if (!side.matches("^[trbl]$"))
			return null;

		m_board.put(generateCellKey(row, col) + side, player);

		JsonArrayBuilder boxes = Json.createArrayBuilder();

		if (capture(row, col, player)) {
			boxes.add(Json.createObjectBuilder()
				.add("r", row)
				.add("c", col)
			.build());
		}

		switch (side) {
		case "t":
			m_board.put(generateCellKey(row - 1, col) + "b", player);
			if (capture(row - 1, col, player)) {
				boxes.add(Json.createObjectBuilder()
					.add("r", row - 1)
					.add("c", col)
				.build());
			}
			break;
		case "r":
			m_board.put(generateCellKey(row, col + 1) + "l", player);
			if (capture(row, col + 1, player)) {
				boxes.add(Json.createObjectBuilder()
					.add("r", row)
					.add("c", col + 1)
				.build());
			}
			break;
		case "b":
			m_board.put(generateCellKey(row + 1, col) + "t", player);
			if (capture(row + 1, col, player)) {
				boxes.add(Json.createObjectBuilder()
					.add("r", row + 1)
					.add("c", col)
				.build());
			}
			break;
		case "l":
			m_board.put(generateCellKey(row, col - 1) + "r", player);
			if (capture(row, col - 1, player)) {
				boxes.add(Json.createObjectBuilder()
					.add("r", row)
					.add("c", col - 1)
				.build());
			}
			break;
		}

		JsonObjectBuilder result = Json.createObjectBuilder()
			.add("type", "move")
			.add("player", player)
			.add("line", Json.createObjectBuilder()
				.add("r", row)
				.add("c", col)
				.add("side", side)
			.build());

		JsonArray captured = boxes.build();
		if (!captured.isEmpty())
			result.add("boxes", captured);

		callback.accept(result, !captured.isEmpty());
		return result.build();
	}

	public boolean capture(int row, int col, int player) {
		if (isMarked(row, col, "x"))
			return false;

		if (!isMarked(row, col, "t") || !isMarked(row, col, "r") || !isMarked(row, col, "b") || !isMarked(row, col, "l"))
			return false;

		m_board.put(generateCellKey(row, col) + "x", player);
		m_scores.put(player, getScore(player) + 1);
		--m_uncaptured;
		return true;
	}

	public int getScore(int player) {
		return m_scores.containsKey(player) ? m_scores.get(player) : 0;
	}

	public int getWinner() {
		int winner = -1;
		for (ConcurrentHashMap.Entry<Integer, Integer> entry : m_scores.entrySet()) {
			if (!m_scores.containsKey(winner) || entry.getValue() > m_scores.get(winner))
				winner = entry.getKey();
		}
		return winner;
	}

	public boolean hasUncaptured() {
		return m_uncaptured > 0;
	}

	private String generateCellKey(int row, int col) {
		return row + "." + col + ".";
	}
	
	public boolean makesCapturable(int row, int col, String side) {
		
		//check the given cell
//		int topMark = isMarked(row, col, "t") ? 1 : 0;
//		int rightMark = isMarked(row, col, "r") ? 1 : 0;
//		int bottomMark = isMarked(row, col, "b") ? 1 : 0;
//		int leftMark = isMarked(row, col, "l") ? 1 : 0;
//		if (topMark + rightMark + bottomMark + leftMark == 2) {
//			return true;
//		}
		
		//if it's horizontal, check above and below, unless you're at a border
		
		//top
		if (side == "t") {
			int rightMark = isMarked(row, col, "r") ? 1 : 0;
			int bottomMark = isMarked(row, col, "b") ? 1 : 0;
			int leftMark = isMarked(row, col, "l") ? 1 : 0;
			if (rightMark + bottomMark + leftMark == 2) {
				return true;
			}
			
			if (row != 0) {
				int topMark = isMarked(row-1, col, "t") ? 1 : 0;
				rightMark = isMarked(row-1, col, "r") ? 1 : 0;
				leftMark = isMarked(row-1, col, "l") ? 1 : 0;
				if (rightMark + topMark + leftMark == 2) {
					return true;
				}
			}
		}
		
		//bottom
		if (side == "b") {
			int topMark = isMarked(row, col, "t") ? 1 : 0;
			int rightMark = isMarked(row, col, "r") ? 1 : 0;
			int leftMark = isMarked(row, col, "l") ? 1 : 0;
			if (rightMark + topMark + leftMark == 2) {
				return true;
			}
			
			if (row != 9) {
				rightMark = isMarked(row+1, col, "r") ? 1 : 0;
				int bottomMark = isMarked(row+1, col, "b") ? 1 : 0;
				leftMark = isMarked(row+1, col, "l") ? 1 : 0;
				if (rightMark + bottomMark + leftMark == 2) {
					return true;
				}
			}
		}
		
		//if it's vertical, check left and right, unless you're at a border
		
		//left
		if (side == "l") {
			int topMark = isMarked(row, col, "t") ? 1 : 0;
			int rightMark = isMarked(row, col, "r") ? 1 : 0;
			int bottomMark = isMarked(row, col, "b") ? 1 : 0;
			if (rightMark + topMark + bottomMark == 2) {
				return true;
			}
			
			if (col != 0) {
				int leftMark = isMarked(row, col-1, "l") ? 1 : 0;
				bottomMark = isMarked(row, col-1, "b") ? 1 : 0;
				topMark = isMarked(row, col-1, "t") ? 1 : 0;
				if (leftMark + bottomMark + topMark == 2) {
					return true;
				}
			}
		}
		
		//right
		if (side == "r") {
			int topMark = isMarked(row, col, "t") ? 1 : 0;
			int leftMark = isMarked(row, col, "l") ? 1 : 0;
			int bottomMark = isMarked(row, col, "b") ? 1 : 0;
			if (leftMark + topMark + bottomMark == 2) {
				return true;
			}
			
			if (col != 9) {
				int rightMark = isMarked(row, col+1, "r") ? 1 : 0;
				bottomMark = isMarked(row, col+1, "b") ? 1 : 0;
				topMark = isMarked(row, col+1, "t") ? 1 : 0;
				if (rightMark + bottomMark + topMark == 2) {
					return true;
				}
			}
		}
		
		return false;
	}
}
