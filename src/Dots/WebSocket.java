package Dots;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/websocket")

public class WebSocket {
	@OnOpen
	public void handleOpen(Session session) {
		System.out.println(session.getId() + " Open");
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
		System.out.println(session.getId() + " Message:" + message);

		StringReader stringReader = new StringReader(message);
		JsonReader jsonReader = Json.createReader(stringReader);

		JsonObject changes = jsonReader.readObject();
		session.getAsyncRemote().sendText(changes.toString());

		stringReader.close();
		jsonReader.close();
	}

	@OnClose
	public void handleClose(Session session) {
		System.out.println(session.getId() + " Close");
	}

	@OnError
	public void handleError(Throwable t) {
		System.out.println(t.getMessage());
	}
}
