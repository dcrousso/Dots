<%@ page import="Dots.User" %>
<%@ page import="Dots.Util" %>
<% User user = ((User) session.getAttribute("user")); %>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
		<meta name="HandheldFriendly" content="True">
		<meta name="MobileOptimized" content="320">
		<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=yes" />

		<title>Dots</title>
		<meta name="keywords" content="dots and boxes, dots, boxes, game">
		<meta name="description" content="Play the classic game Dots and Boxes, but with a twist!">
		<meta name="author" content="Benjamin Stein, Sean Yuan, Devin Rousso, Shane Rosse">
		<link rel="icon" href="ui/images/favicon.png" type="image/png" />

		<link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/main.css">
		<link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/fonticons.css">
	</head>
	<body>
		<header>
			<nav>
				<a href="${pageContext.request.contextPath}/" title="Homepage">Home</a>
				<a class="link authentication <%= (user != null ? "logout" : "login") %>" href="${pageContext.request.contextPath}/<%= (user != null ? "logout" : "login") %>" title="<%= (user != null ? "Logout" : "Login") %>"><%= (user != null ? "Logout" : "Login") %></a>
			</nav>
		</header>
		<main>
			<noscript><h2>You must have JavaScript enabled to play</h2></noscript>
		</main>
		<footer>
			<div class="container">
				<div id="score" class="icon-box" title="Captured Boxes">0</div>
				<div id="played" class="icon-flag-checkered" title="Games Played"><%= (user != null ? user.getGamesPlayed() : 0) %></div>
				<div id="won" class="icon-trophy" title="Games Won"><%= (user != null ? user.getGamesWon() : 0) %></div>
				<div id="points" class="icon-star" title="Total Points"><%= (user != null ? user.getPoints() : 0) %></div>
			</div>
		</footer>
		<div class="scripts">
			<script src="${pageContext.request.contextPath}/ui/js/main.js"></script>
		</div>
	</body>
</html>
