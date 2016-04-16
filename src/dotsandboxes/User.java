package dotsandboxes;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
	private String m_username;
	private int m_played;
	private int m_won;
	private int m_points;
	private boolean m_authenticated;

	public User(String username) {
		m_username = username;
		m_played = 0;
		m_won = 0;
		m_points = 0;
		m_authenticated = true;
	}

	public User(ResultSet rs, String password) {
		try {
			m_username = rs.getString("username");
			m_played = rs.getInt("played");
			m_won = rs.getInt("won");
			m_points = rs.getInt("points");
			m_authenticated = rs.getString("password").equals(password);
		} catch (SQLException e) {
		}
	}

	public String getUsername() {
		return m_username;
	}

	public int getGamesPlayed() {
		if (!isAuthenticated())
			return 0;

		return m_played;
	}

	public int getGamesWon() {
		if (!isAuthenticated())
			return 0;

		return m_won;
	}

	public int getPoints() {
		if (!isAuthenticated())
			return 0;

		return m_points;
	}

	public void addGame(int points, boolean won) {
		if (!isAuthenticated())
			return;

		m_points += points;
		++m_played;
		if (won)
			++m_won;
	}

	public boolean isAuthenticated() {
		return m_authenticated;
	}
}
