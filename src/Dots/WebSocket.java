package Dots;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/websocket")

public class WebSocket {
	private static final ConcurrentLinkedQueue<WebSocket> s_waiting = new ConcurrentLinkedQueue<WebSocket>();
	private static final HashSet<GameThread> s_games = new HashSet<GameThread>();

	private Session m_session;
	private GameThread m_game;
	private boolean m_error;
	private int m_id;

	@OnOpen
	public void handleOpen(Session session) {
		m_session = session;
		m_game = null;
		m_error = false;
		m_id = 0;

		WebSocket other = s_waiting.poll();
		if (other == null)
			s_waiting.offer(this);
		else
			s_games.add(m_game = other.m_game = new GameThread(other, this));
	}

	@OnMessage
	public void handleMessage(String message) {
		if (!isAlive())
			return;

		JsonObject content = Util.parseJSON(message).build();
		m_game.send(this, content);

		if (content.getString("type").equals("leave"))
			m_error = true;
	}

	@OnClose
	public void handleClose() {
		if (!isAlive())
			return;

		m_game.send(this, Json.createObjectBuilder()
			.add("type", "leave")
		.build());
	}

	@OnError
	public void handleError(Throwable t) {
		m_error = true;
	}

	public void send(JsonObject content) {
		m_session.getAsyncRemote().sendText(content.toString());
	}

	public boolean isAlive() {
		return m_game != null && !m_error;
	}

	public void setId(int i) {
		m_id = i;
	}

	public int getId() {
		return m_id;
	}
}
