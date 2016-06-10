<%@ page import="dotsandboxes.User" %>
<% User user = ((User) session.getAttribute("user")); %>
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8">
		<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
		<meta name="HandheldFriendly" content="True">
		<meta name="MobileOptimized" content="320">
		<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=2, user-scalable=yes"/>

		<link rel="apple-touch-icon" sizes="57x57" href="/apple-touch-icon-57x57.png">
		<link rel="apple-touch-icon" sizes="60x60" href="/apple-touch-icon-60x60.png">
		<link rel="apple-touch-icon" sizes="72x72" href="/apple-touch-icon-72x72.png">
		<link rel="apple-touch-icon" sizes="76x76" href="/apple-touch-icon-76x76.png">
		<link rel="apple-touch-icon" sizes="114x114" href="/apple-touch-icon-114x114.png">
		<link rel="apple-touch-icon" sizes="120x120" href="/apple-touch-icon-120x120.png">
		<link rel="apple-touch-icon" sizes="144x144" href="/apple-touch-icon-144x144.png">
		<link rel="apple-touch-icon" sizes="152x152" href="/apple-touch-icon-152x152.png">
		<link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon-180x180.png">
		<link rel="icon" type="image/png" href="/favicon-32x32.png" sizes="32x32">
		<link rel="icon" type="image/png" href="/favicon-194x194.png" sizes="194x194">
		<link rel="icon" type="image/png" href="/favicon-96x96.png" sizes="96x96">
		<link rel="icon" type="image/png" href="/android-chrome-192x192.png" sizes="192x192">
		<link rel="icon" type="image/png" href="/favicon-16x16.png" sizes="16x16">
		<link rel="manifest" href="/manifest.json">
		<link rel="mask-icon" href="/safari-pinned-tab.svg" color="#000000">
		<meta name="apple-mobile-web-app-title" content="Dots">
		<meta name="application-name" content="Dots">
		<meta name="msapplication-TileColor" content="#b91d47">
		<meta name="msapplication-TileImage" content="/mstile-144x144.png">
		<meta name="theme-color" content="#e9f4fc">

		<title>Dots</title>
		<meta name="keywords" content="dots and boxes, dots, boxes, game">
		<meta name="description" content="Play the classic game Dots and Boxes, but with a twist!">
		<meta name="author" content="Benjamin Stein, Sean Yuan, Devin Rousso, Shane Rosse">
		<link rel="canonical" href="http://dotsandboxes.online">

		<meta name="google-site-verification" content="JBBDq2xkKJGYYw9qWeNDz3kQYmjjo7M-0VhrptQcsoI"/>

		<link rel="stylesheet" href="/ui/css/inline.css">
		<link rel="stylesheet" href="/ui/css/fonticons.css">
	</head>
	<body>
		<header>
			<nav>
				<div class="players">
					<div class="p1" title="Player 1"></div>
					<div class="p2" title="Player 2"></div>
					<div class="p3" title="Player 3"></div>
					<div class="p4" title="Player 4"></div>
				</div>
				<a class="link authentication <%= (user != null ? "logout" : "login") %>" href="/<%= (user != null ? "logout" : "login") %>" title="<%= (user != null ? "Logout" : "Login") %>"><%= (user != null ? "Logout" : "Login") %></a>
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
		<div class="preload" hidden>
			<img src="/ui/images/icon-box-p1.svg">
			<img src="/ui/images/icon-box-p2.svg">
			<img src="/ui/images/icon-box-p3.svg">
			<img src="/ui/images/icon-box-p4.svg">
		</div>
		<script src="/ui/js/main.js"></script>
	</body>
</html>
