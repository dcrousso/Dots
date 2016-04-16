package dotsandboxes;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import javatechniques.DeepCopy;

public class GameBoard implements Serializable {
	private static final long serialVersionUID = 1L;

	public static class Move {
		public int row;
		public int col;
		public String side;

		Move(int row, int col, String side) {
			this.row = row;
			this.col = col;
			this.side = side;
		}
	}

	private ConcurrentHashMap<String, Integer> m_board;
	private int m_uncaptured;
	private ConcurrentHashMap<Integer, Integer> m_scores;
	private int m_rows;
	private int m_cols;

	public GameBoard(JsonArray board, Vector<Player> players) {
		m_board = new ConcurrentHashMap<String, Integer>();
		m_uncaptured = Util.count(board.toString(), "\"b\"\\s*:\\s*0");
		m_scores = new ConcurrentHashMap<Integer, Integer>();
		m_rows = 0;
		m_cols = 0;

		players.parallelStream().forEach(player -> m_scores.put(player.getId(), 0));

		m_rows = board.size();
		m_cols = board.parallelStream().mapToInt(row -> ((JsonArray) row).size()).min().getAsInt();

		for (int r = 0; r < m_rows; ++r) {
			JsonArray row = (JsonArray) board.get(r);
			for (int c = 0; c < m_cols; ++c) {
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

	public GameBoard duplicate() {
		return (GameBoard) DeepCopy.copy(this);
	}

	public int getRows() {
		return m_rows;
	}

	public int getCols() {
		return m_cols;
	}

	public boolean isMarked(int row, int col, String side) {
		if (!lineExists(row, col, side))
			return false;

		return m_board.get(generateCellKey(row, col) + side) != 0;
	}

	public HashSet<Move> isCapturable(int row, int col) {
		BiFunction<Integer, Integer, Move> determineSide = (r, c) -> {
			if (r < 0 || r >= m_rows || c < 0 || c >= m_cols)
				return null;

			boolean top = isMarked(r, c, "t");
			boolean right = isMarked(r, c, "r");
			boolean bottom = isMarked(r, c, "b");
			boolean left = isMarked(r, c, "l");

			if (!top && right && bottom && left)
				return new Move(r, c, "t");

			if (top && !right && bottom && left)
				return new Move(r, c, "r");

			if (top && right && !bottom && left)
				return new Move(r, c, "b");

			if (top && right && bottom && !left)
				return new Move(r, c, "l");

			return null;
		};

		HashSet<Move> capturable = new HashSet<Move>();

		Move cell = determineSide.apply(row, col);
		if (cell != null)
			capturable.add(cell);

		Move top = determineSide.apply(row - 1, col);
		if (top != null)
			capturable.add(top);

		Move right = determineSide.apply(row, col + 1);
		if (right != null)
			capturable.add(right);

		Move bottom = determineSide.apply(row + 1, col);
		if (bottom != null)
			capturable.add(bottom);

		Move left = determineSide.apply(row, col - 1);
		if (left != null)
			capturable.add(left);

		return capturable;
	}

	public HashSet<Move> makesCapturable(int row, int col, String side) {
		BiFunction<Integer, Integer, Move> checkCell = (r, c) -> {
			final int top = isMarked(r, c, "t") ? 1 : 0;
			final int right = isMarked(r, c, "r") ? 1 : 0;
			final int bottom = isMarked(r, c, "b") ? 1 : 0;
			final int left = isMarked(r, c, "l") ? 1 : 0;

			Function<String, Move> findMissing = s -> {
				if (!s.equals("t") && top == 0)
					return new Move(r, c, "t");

				if (!s.equals("r") && right == 0)
					return new Move(r, c, "r");

				if (!s.equals("b") && bottom == 0)
					return new Move(r, c, "b");

				if (!s.equals("l") && left == 0)
					return new Move(r, c, "l");

				return null;
			};

			boolean isAdjacent = r != row || c != col;
			switch (side) {
			case "t":
				if (isAdjacent && bottom == 0 && top + right + left == 2)
					return findMissing.apply("b");

				if (!isAdjacent && top == 0 && right + bottom + left == 2)
					return findMissing.apply("t");
			case "r":
				if (isAdjacent && left == 0 && top + right + bottom == 2)
					return findMissing.apply("l");

				if (!isAdjacent && right == 0 && top + bottom + left == 2)
					return findMissing.apply("r");
			case "b":
				if (isAdjacent && top == 0 && right + bottom + left == 2)
					return findMissing.apply("t");

				if (!isAdjacent && bottom == 0 && top + right + left == 2)
					return findMissing.apply("b");
			case "l":
				if (isAdjacent && right == 0 && top + bottom + left == 2)
					return findMissing.apply("r");

				if (!isAdjacent && left == 0 && top + right + bottom == 2)
					return findMissing.apply("l");
			}

			return null;
		};

		HashSet<Move> missing = new HashSet<Move>();

		Move cell = checkCell.apply(row, col);
		if (cell != null)
			missing.add(cell);

		switch (side) {
		case "t":
			Move top = checkCell.apply(row - 1, col);
			if (top != null)
				missing.add(top);

			break;
		case "r":
			Move right = checkCell.apply(row, col + 1);
			if (right != null)
				missing.add(right);

			break;
		case "b":
			Move bottom = checkCell.apply(row + 1, col);
			if (bottom != null)
				missing.add(bottom);

			break;
		case "l":
			Move left = checkCell.apply(row, col - 1);
			if (left != null)
				missing.add(left);

			break;
		}

		return missing;
	}

	public JsonObject mark(int row, int col, String side, int player, BiConsumer<JsonObjectBuilder, Boolean> callback) {
		if (!lineExists(row, col, side) || isMarked(row, col, side))
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
			if (!lineExists(row - 1, col, "b"))
				break;

			m_board.put(generateCellKey(row - 1, col) + "b", player);
			if (capture(row - 1, col, player)) {
				boxes.add(Json.createObjectBuilder()
					.add("r", row - 1)
					.add("c", col)
				.build());
			}
			break;
		case "r":
			if (!lineExists(row, col + 1, "l"))
				break;

			m_board.put(generateCellKey(row, col + 1) + "l", player);
			if (capture(row, col + 1, player)) {
				boxes.add(Json.createObjectBuilder()
					.add("r", row)
					.add("c", col + 1)
				.build());
			}
			break;
		case "b":
			if (!lineExists(row + 1, col, "t"))
				break;

			m_board.put(generateCellKey(row + 1, col) + "t", player);
			if (capture(row + 1, col, player)) {
				boxes.add(Json.createObjectBuilder()
					.add("r", row + 1)
					.add("c", col)
				.build());
			}
			break;
		case "l":
			if (!lineExists(row, col - 1, "r"))
				break;

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

		if (callback != null)
			callback.accept(result, !captured.isEmpty());

		return result.build();
	}

	public int getScore(int player) {
		return m_scores.containsKey(player) ? m_scores.get(player) : 0;
	}

	public int getWinner() {
		if ((new HashSet<Integer>(m_scores.values())).size() == 1)
			return -1;

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

	private boolean capture(int row, int col, int player) {
		if (isMarked(row, col, "x"))
			return false;

		if (!isMarked(row, col, "t") || !isMarked(row, col, "r") || !isMarked(row, col, "b") || !isMarked(row, col, "l"))
			return false;

		m_board.put(generateCellKey(row, col) + "x", player);
		m_scores.put(player, getScore(player) + 1);
		--m_uncaptured;
		return true;
	}

	private boolean lineExists(int row, int col, String side) {
		if (Util.isEmpty(side))
			return false;

		return m_board.containsKey(generateCellKey(row, col) + side);
	}

	private String generateCellKey(int row, int col) {
		return row + "." + col + ".";
	}
}
