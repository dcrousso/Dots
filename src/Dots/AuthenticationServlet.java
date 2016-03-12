package Dots;

import java.io.IOException;
import java.sql.SQLException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthenticationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getSession().isNew() || request.getSession().getAttribute("authenticated") == null)
			return;

		request.getSession().removeAttribute("authenticated");
		request.getSession().invalidate();
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String encrypted = Util.encryptMD5(request.getParameter("password"));

		boolean valid = Util.query("SELECT * FROM users WHERE username = ?", new String[] {
			request.getParameter("username")
		}, rs -> {
			try {
				while (rs.next()) {
					if (rs.getString("password").equals(encrypted))
						return true;
				}
			} catch (SQLException e) {
			}
			return false;
		});

		if (valid)
			request.getSession().setAttribute("authenticated", true);

		JsonObject result = Json.createObjectBuilder()
			.add("authenticated", valid)
		.build();
		response.getWriter().write(result.toString());
	}
}
