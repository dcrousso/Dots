<% boolean authenticated = !session.isNew() && session.getAttribute("authenticated") != null; %>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
		<meta name="HandheldFriendly" content="True">
		<meta name="MobileOptimized" content="320">
		<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no" />

		<title>Dots</title>
		<meta name="keywords" content="dots and boxes, dots, boxes, game">
		<meta name="description" content="Play the classic game Dots and Boxes, but with a twist!">
		<meta name="author" content="Benjamin Stein, Sean Yuan, Devin Rousso, Shane Rosse">

		<link rel="stylesheet" href="${pageContext.request.contextPath}/ui/css/main.css">
	</head>
	<body>
		<header>
			<nav>
				<a href="${pageContext.request.contextPath}/" title="Homepage">Home</a>
				<a class="link authentication <%= (authenticated ? "logout" : "login") %>" href="${pageContext.request.contextPath}/<%= (authenticated ? "logout" : "login") %>" title="<%= (authenticated ? "Logout" : "Login") %>"><%= (authenticated ? "Logout" : "Login") %></a>
			</nav>
		</header>
		<main>
			<noscript><h2>You must have JavaScript enabled to play</h2></noscript>
			<div class="container boxes"></div>
			<div class="container lines"></div>
			<div class="container dots"></div>
		</main>
		<footer>
		</footer>
		<div class="scripts">
			<script src="${pageContext.request.contextPath}/ui/js/main.js"></script>
		</div>
	</body>
</html>
