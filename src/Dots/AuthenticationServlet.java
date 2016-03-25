package Dots;

import java.io.IOException;
import java.sql.SQLException;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthenticationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getSession().isNew() || request.getSession().getAttribute("user") == null)
			return;

		request.getSession().removeAttribute("user");
		request.getSession().invalidate();
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String username = request.getParameter("username");
		if (Util.isEmpty(username))
			return;

		String password = request.getParameter("password");
		if (Util.isEmpty(password))
			return;

		String encrypted = Util.encryptMD5(password);

		User user = Util.query("SELECT * FROM users WHERE username = ?", new String[] {
			username
		}, rs -> {
			try {
				while (rs.next()) {
					if (rs.getString("password").equals(encrypted))
						return new User(rs);
				}
			} catch (SQLException e) {
			}
			return null;
		});

		JsonObjectBuilder result = Json.createObjectBuilder();

		if (user != null) {
			result.add("played", user.getGamesPlayed());
			result.add("won", user.getGamesWon());
			result.add("points", user.getPoints());
		} else
			result.add("error", "Invalid Username/Password");

		request.getSession().setAttribute("user", user);
		response.getWriter().write(result.build().toString());
	}
}
