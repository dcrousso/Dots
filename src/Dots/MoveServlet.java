package Dots;

import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MoveServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String json = request.getParameter("changes");
		if (Util.isEmpty(json)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		StringReader stringReader = new StringReader(json);
		JsonReader jsonReader = Json.createReader(stringReader);

		JsonObject changes = jsonReader.readObject();

		stringReader.close();
		jsonReader.close();
	}
}
