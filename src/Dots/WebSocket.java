package Dots;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.http.HttpSession;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/websocket", configurator = SessionConfigurator.class)

public class WebSocket {
	private static final ConcurrentLinkedQueue<WebSocket> s_waiting = new ConcurrentLinkedQueue<WebSocket>();

	private Session m_socketSession;
	private HttpSession m_httpSession;
	private GameController m_game;
	private boolean m_error;
	private int m_id;

	@OnOpen
	public void handleOpen(Session session, EndpointConfig config) {
		m_socketSession = session;
		m_httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
		m_game = null;
		m_error = false;
		m_id = -1;

		WebSocket existing = s_waiting.poll();
		if (existing == null)
			s_waiting.offer(this);
		else
			m_game = existing.m_game = new GameController(existing, this);
	}

	@OnMessage
	public void handleMessage(String message) {
		if (!isAlive())
			return;

		m_game.send(this, Util.parseJSON(message).build());
	}

	@OnClose
	public void handleClose() {
		if (!isAlive()) {
			if (s_waiting.contains(this))
				s_waiting.remove(this);

			return;
		}

		m_error = true;

		m_game.send(this, Json.createObjectBuilder()
			.add("type", "leave")
		.build());
	}

	@OnError
	public void handleError(Throwable t) {
		t.printStackTrace();
		m_error = true;
	}

	public void send(JsonObject content) {
		if (!isAlive())
			return;

		m_socketSession.getAsyncRemote().sendText(content.toString());
	}

	public boolean isAlive() {
		return m_id != -1 && !m_error;
	}

	public Object getSessionAttribute(String key) {
		return m_httpSession.getAttribute(key);
	}

	public void setId(int i) {
		if (m_id != -1)
			return;

		m_id = i;
	}

	public int getId() {
		return m_id;
	}
}
