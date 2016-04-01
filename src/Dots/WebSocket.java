package Dots;

import java.util.HashMap;
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
	private static final HashMap<Integer, ConcurrentLinkedQueue<WebSocket>> s_waiting = new HashMap<Integer, ConcurrentLinkedQueue<WebSocket>>();
	static {
		s_waiting.put(2, new ConcurrentLinkedQueue<WebSocket>());
		s_waiting.put(3, new ConcurrentLinkedQueue<WebSocket>());
		s_waiting.put(4, new ConcurrentLinkedQueue<WebSocket>());
	}

	private Session m_socketSession;
	private HttpSession m_httpSession;

	@OnOpen
	public void handleOpen(Session session, EndpointConfig config) {
		initialize(Type.Client);

		m_socketSession = session;
		m_httpSession = (HttpSession) config.getUserProperties().get(HttpSession.class.getName());
	}

	@OnMessage
	public void handleMessage(String message) {
		JsonObject content = Util.parseJSON(message).build();
		if (!isAlive() && content.containsKey("mode")) {
			int mode = content.getInt("mode");
			ConcurrentLinkedQueue<WebSocket> waiting = s_waiting.get(mode);
			if (waiting == null)
				return;

			if (mode == 2) { // 2 Players
				WebSocket p1 = waiting.poll();
				if (p1 == null)
					waiting.offer(this);
				else
					m_game = p1.m_game = new GameController(p1, this);
			} else if (mode == 3 && getUser() != null) { // 3 Players (Authenticated only)
				WebSocket p1 = waiting.poll();
				WebSocket p2 = waiting.poll();
				if (p2 == null) {
					if (p1 != null)
						waiting.offer(p1);
					waiting.offer(this);
				} else
					m_game = p1.m_game = p2.m_game = new GameController(p1, p2, this);
			} else if (mode == 4 && getUser() != null) { // 4 Players (Authenticated only)
				WebSocket p1 = waiting.poll();
				WebSocket p2 = waiting.poll();
				WebSocket p3 = waiting.poll();
				if (p3 == null) {
					if (p1 != null)
						waiting.offer(p1);
					if (p2 != null)
						waiting.offer(p2);
					waiting.offer(this);
				} else
					m_game = p1.m_game = p2.m_game = p3.m_game = new GameController(p1, p2, p3, this);
			}
		}

		if (!isAlive() || content.containsKey("mode"))
			return;

		m_game.send(this, content);
	}

	@OnClose
	public void handleClose() {
		if (!isAlive()) {
			s_waiting.values().parallelStream().forEach(queue -> {
				if (queue.contains(this))
					queue.remove(this);
			});

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

	public User getUser() {
		return (User) m_httpSession.getAttribute("user");
	}

	public void restart() {
		if (!isAlive())
			return;

		initialize(getType());
	}
}
