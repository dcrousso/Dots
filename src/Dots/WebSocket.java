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

public class WebSocket extends Player {
	private static final ConcurrentLinkedQueue<WebSocket> s_waiting = new ConcurrentLinkedQueue<WebSocket>();

	private Session m_socketSession;
	private HttpSession m_httpSession;

	@OnOpen
	public void handleOpen(Session session, EndpointConfig config) {
		initialize(Type.Client);

		m_socketSession = session;
		m_httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());

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

	public Object getAttribute(String key) {
		return m_httpSession.getAttribute(key);
	}

	public void restart() {
		if (!isAlive())
			return;

		initialize(getType());

		WebSocket existing = s_waiting.poll();
		if (existing == null)
			s_waiting.offer(this);
		else
			m_game = existing.m_game = new GameController(existing, this);
	}
}
