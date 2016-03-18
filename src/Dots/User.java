package Dots;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
	private String m_username;
	private int m_played;
	private int m_won;
	private int m_highscore;

	User() {
		m_username = null;
		m_played = 0;
		m_won = 0;
		m_highscore = 0;
	}

	User(ResultSet rs) {
		try {
			m_username = rs.getString("username");
			m_played = rs.getInt("played");
			m_won = rs.getInt("won");
			m_highscore = rs.getInt("highscore");
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

	public void addGame(boolean won) {
		++m_played;

		if (won)
			++m_won;
	}

	public void addGame() {
		addGame(false);
	}

	public int getHighscore() {
		return m_highscore;
	}

	public void setHighscore(int highscore) {
		m_highscore = highscore;
	}
}
