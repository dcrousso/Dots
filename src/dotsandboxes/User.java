package dotsandboxes;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
	private String m_username;
	private int m_played;
	private int m_won;
	private int m_points;

	public User(String username) {
		m_username = username;
		m_played = 0;
		m_won = 0;
		m_points = 0;
	}

	public User(ResultSet rs) {
		try {
			m_username = rs.getString("username");
			m_played = rs.getInt("played");
			m_won = rs.getInt("won");
			m_points = rs.getInt("points");
		} catch (SQLException e) {
		}
	}

	public String getUsername() {
		return m_username;
	}

	public int getGamesPlayed() {
		return m_played;
	}

	public int getGamesWon() {
		return m_won;
	}

	public int getPoints() {
		return m_points;
	}

	public void addGame(int points, boolean won) {
		m_points += points;
		++m_played;
		if (won)
			++m_won;
	}
}
