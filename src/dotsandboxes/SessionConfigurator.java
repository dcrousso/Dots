package dotsandboxes;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class SessionConfigurator extends ServerEndpointConfig.Configurator {
	@Override
	public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
		super.modifyHandshake(config, request, response);

		config.getUserProperties().put(HttpSession.class.getName(), (HttpSession) request.getHttpSession());
	}
}