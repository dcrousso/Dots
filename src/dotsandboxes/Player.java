package dotsandboxes;

import javax.json.JsonObject;

public abstract class Player {
	public static enum Type {
		Client,
		AI
	};

	private Type m_type;
	protected GameController m_game;
	protected boolean m_error;
	protected int m_id;

	public Type getType() {
		return m_type;
	}

	public abstract void send(GameController game, JsonObject content);

	public abstract void restart();

	public boolean isAlive() {
		return m_id != -1 && !m_error;
	}

	public void setId(int i) {
		if (m_id != -1)
			return;

		m_id = i;
	}

	public int getId() {
		return m_id;
	}

	protected void initialize(Type type) {
		m_type = type;
		m_game = null;
		m_error = false;
		m_id = -1;
	}
}
