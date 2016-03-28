package Dots;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class GameBoard {
	private ConcurrentSkipListMap<String, Integer> m_board;
	private int m_uncaptured;
	private ConcurrentHashMap<Integer, Integer> m_scores;

	public GameBoard(JsonArray board, Vector<Player> players) {
		m_board = new ConcurrentSkipListMap<String, Integer>();
		m_uncaptured = Util.count(board.toString(), "\"b\"\\s*:\\s*0");
		m_scores = new ConcurrentHashMap<Integer, Integer>();

		for (int r = 0; r < board.size(); ++r) {
			JsonArray row = (JsonArray) board.get(r);
			for (int c = 0; c < row.size(); ++c) {
				JsonObject cell = (JsonObject) row.get(c);
				String key = generateCellKey(r, c);
				m_board.put(key + "x", cell.getInt("x"));
				m_board.put(key + "t", cell.getInt("t"));
				m_board.put(key + "r", cell.getInt("r"));
				m_board.put(key + "b", cell.getInt("b"));
				m_board.put(key + "l", cell.getInt("l"));
			}
		}

		players.parallelStream().forEach(player -> m_scores.put(player.getId(), 0));
	}

	public int getMark(int row, int col, String side) {
		String key = generateCellKey(row, col) + side;
		if (!m_board.containsKey(key))
			return 0;
		return m_board.get(key);
	}

	public boolean isMarked(int row, int col, String side) {
		return getMark(row, col, side) != 0;
	}

	public JsonObject mark(int row, int col, String side, int player) {
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

		return Json.createObjectBuilder()
			.add("type", "move")
			.add("player", player)
			.add("line", Json.createObjectBuilder()
				.add("r", row)
				.add("c", col)
				.add("side", side)
			.build())
			.add("boxes", boxes.build())
		.build();
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
		return m_scores.get(player);
	}

	public int getWinner() {
		int winner = 0;
		for (int i = 1; i < m_scores.size(); ++i) {
			if (m_scores.get(i) > m_scores.get(winner))
				winner = i;
		}
		return winner;
	}

	public boolean hasUncaptured() {
		return m_uncaptured > 0;
	}

	private String generateCellKey(int row, int col) {
		return row + "." + col + ".";
	}
}
