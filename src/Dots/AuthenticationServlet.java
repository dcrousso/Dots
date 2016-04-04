package Dots;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AuthenticationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().write(((User) request.getSession().getAttribute("user")) == null ? "false" : "true");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String logout = request.getParameter("logout");
		if (!Util.isEmpty(logout) && logout.equals("true")) {
			request.getSession().removeAttribute("user");
			request.getSession().invalidate();
			return;
		}

		String username = request.getParameter("username");
		if (Util.isEmpty(username))
			return;

		String password = request.getParameter("password");
		if (Util.isEmpty(password))
			return;

		boolean register = Optional.ofNullable(request.getParameter("register")).orElse("").equals("true");

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

		if ((user != null && !register) || (user == null && register)) {
			if (user == null && register) {
				user = new User(username);
				Util.update("INSERT INTO users (username, password, played, won, points) VALUES (?, ?, ?, ?, ?)", new String[] {
					user.getUsername(),
					encrypted,
					Integer.toString(user.getGamesPlayed()),
					Integer.toString(user.getGamesWon()),
					Integer.toString(user.getPoints())
				});
			}

			result.add("played", user.getGamesPlayed());
			result.add("won", user.getGamesWon());
			result.add("points", user.getPoints());
		} else
			result.add("error", "Invalid Username/Password");

		request.getSession().setAttribute("user", user);
		response.getWriter().write(result.build().toString());
	}
}
