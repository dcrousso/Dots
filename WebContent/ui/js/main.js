(function() {
	"use strict";

	var playerIcons = {
		1: document.querySelector("header > nav > .players > div.p1"),
		2: document.querySelector("header > nav > .players > div.p2"),
		3: document.querySelector("header > nav > .players > div.p3"),
		4: document.querySelector("header > nav > .players > div.p4")
	};
	var main = document.querySelector("main");
	var authenticationLink = document.querySelector(".link.authentication");
	var authenticationForm = null;
	var authenticationEnded = null;

	function isEmpty(s) {
		return !s && !s.length && !s.trim().length;
	}

	function ajax(method, url, callback) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (window.logging)
				console.log(xhr);

			if (xhr.readyState !== 4 || xhr.status !== 200)
				return;

			if (typeof callback === "function")
				callback(xhr);
		};
		xhr.open(method, url, true);
		xhr.send();
	}


	// ============================== //
	// ==========   Game   ========== //
	// ============================== //

	var game = {
		rows: 10,
		cols: 10,
		playerId: null,
		currentLine: null,
		cells: [],
		modes: {},
		boxes: {},
		lines: {},
		dots: {},
		scoreElement: document.getElementById("score"),
		playedElement: document.getElementById("played"),
		wonElement: document.getElementById("won"),
		pointsElement: document.getElementById("points")
	};
	var socket = null;

	game.modes.container = main.appendChild(document.createElement("div"));
	game.modes.container.classList.add("modes");
	game.modes.handleClick = function(mode) {
		return function(event) {
			if (!socket || socket.readyState !== 1)
				return;

			playerIcons[3].classList.toggle("disabled", mode < 3);
			playerIcons[4].classList.toggle("disabled", mode < 4);

			socket.send(JSON.stringify({mode: mode}));
			main.className = "waiting";
		};
	};

	game.modes.ai = game.modes.container.appendChild(document.createElement("button"));
	game.modes.ai.textContent = "Play AI";
	game.modes.ai.addEventListener("click", game.modes.handleClick(1));

	game.modes.two = game.modes.container.appendChild(document.createElement("button"));
	game.modes.two.textContent = "Two Players";
	game.modes.two.addEventListener("click", game.modes.handleClick(2));

	game.modes.three = game.modes.container.appendChild(document.createElement("button"));
	game.modes.three.textContent = "Three Players";

	game.modes.four = game.modes.container.appendChild(document.createElement("button"));
	game.modes.four.textContent = "Four Players";

	game.modes.login = function() {
		game.modes.boundThreeClick = game.modes.handleClick(3);
		game.modes.three.removeAttribute("title");
		game.modes.three.disabled = false;
		game.modes.three.addEventListener("click", game.modes.boundThreeClick);

		game.modes.boundFourClick = game.modes.handleClick(4);
		game.modes.four.removeAttribute("title");
		game.modes.four.disabled = false;
		game.modes.four.addEventListener("click", game.modes.boundFourClick);
	};

	game.modes.logout = function() {
		game.modes.three.title = "Register to gain access";
		game.modes.three.disabled = true;
		if (game.modes.boundThreeClick)
			game.modes.three.removeEventListener("click", game.modes.boundThreeClick);

		game.modes.four.title = "Register to gain access";
		game.modes.four.disabled = true;
		if (game.modes.boundFourClick)
			game.modes.four.removeEventListener("click", game.modes.boundFourClick);
	};

	if (authenticationLink.classList.contains("logout"))
		game.modes.login();
	else
		game.modes.logout();

	// Boxes
	game.boxes.container = document.createElement("div");
	game.boxes.container.classList.add("container", "boxes");
	game.boxes.elements = [];
	for (var i = 0; i < game.rows; ++i) {
		game.cells[i] = [];
		for (var j = 0; j < game.cols; ++j) {
			game.cells[i][j] = {};
			game.cells[i][j].box = game.boxes.container.appendChild(document.createElement("div"));
			game.cells[i][j].box.classList.add("box");
			game.cells[i][j].box.dataset.row = i;
			game.cells[i][j].box.dataset.col = j;
			game.boxes.elements.push(game.cells[i][j].box);
		}
	}

	game.boxes.container.addEventListener("mousemove", function(event) {
		function isFilled(element) {
			return /\bp[1234]\b/.test(element.className);
		}

		function markLine(line) {
			if (game.currentLine === line || (line && isFilled(line)))
				return;

			if (game.currentLine)
				game.currentLine.classList.remove("hover");

			if (line) {
				line.classList.add("hover");
				game.currentLine = line;
			} else
				game.currentLine = null;
		}

		function distance(event, element) {
			if (!event)
				return Infinity;

			var rect = element.getBoundingClientRect();
			var x = (rect.left + rect.right) / 2;
			var y = (rect.top + rect.bottom) / 2;
			return Math.sqrt(Math.pow(event.clientX - x, 2) + Math.pow(event.clientY - y, 2));
		}

		var element = document.elementFromPoint(event.clientX, event.clientY);
		if (element === main) {
			markLine();
			return;
		}

		var row = parseInt(element.dataset.row);
		var col = parseInt(element.dataset.col);
		if (isNaN(row) || isNaN(col))
			return;

		var cell = game.cells[row][col];
		var top = isFilled(cell.top) ? Infinity : distance(event, cell.top);
		var right = isFilled(cell.right) ? Infinity : distance(event, cell.right);
		var bottom = isFilled(cell.bottom) ? Infinity : distance(event, cell.bottom);
		var left = isFilled(cell.left) ? Infinity : distance(event, cell.left);

		switch (Math.min(top, right, bottom, left)) {
		case Infinity: // Current cell is filled
		default:
			markLine();
			break;
		case top:
			markLine(cell.top);
			break;
		case right:
			markLine(cell.right);
			break;
		case bottom:
			markLine(cell.bottom);
			break;
		case left:
			markLine(cell.left);
			break;
		}
	});

	game.boxes.container.addEventListener("click", function(event) {
		if (!game.currentLine)
			return;

		var element = document.elementFromPoint(event.clientX, event.clientY);
		var row = parseInt(element.dataset.row);
		var col = parseInt(element.dataset.col);
		if (isNaN(row) || isNaN(col))
			return;

		var move = {
			type: "move",
			line: {
				r: row,
				c: col,
				side: null
			}
		};

		var cell = game.cells[row][col];
		switch (game.currentLine) {
		case cell.top:
			move.line.side = "t";
			break;
		case cell.right:
			move.line.side = "r";
			break;
		case cell.bottom:
			move.line.side = "b";
			break;
		case cell.left:
			move.line.side = "l";
			break;
		default: // Current line is not within clicked cell
			return;
		}

		if (window.logging)
			console.log(move);

		if (socket && socket.readyState === 1)
			socket.send(JSON.stringify(move));
	});

	// Lines
	game.lines.container = document.createElement("div");
	game.lines.container.classList.add("container", "lines");
	game.lines.elements = [];
	for (var i = 0; i < game.rows + 1; ++i) {
		for (var j = 0; j < game.cols + 1; ++j) {
			if (i < game.rows) {
				var left = game.lines.container.appendChild(document.createElement("div"));
				left.classList.add("line", "vertical");
				game.lines.elements.push(left);

				if (j < game.cols) {
					game.cells[i][j].left = left;
					if (j)
						game.cells[i][j - 1].right = game.cells[i][j].left;
				} else
					game.cells[i][j - 1].right = left;
			}

			if (j < game.cols) {
				var top = game.lines.container.appendChild(document.createElement("div"));
				top.classList.add("line", "horizontal");
				game.lines.elements.push(top);

				if (i < game.rows) {
					game.cells[i][j].top = top;
					if (i)
						game.cells[i - 1][j].bottom = game.cells[i][j].top;
				} else
					game.cells[i - 1][j].bottom = top;
			}
		}
	}

	// Dots
	game.dots.container = document.createElement("div");
	game.dots.container.classList.add("container", "dots");
	game.dots.elements = [];
	for (var i = 0; i < game.rows + 1; ++i) {
		for (var j = 0; j < game.cols + 1; ++j) {
			var dot = game.dots.container.appendChild(document.createElement("div"));
			dot.classList.add("dot");
			game.dots.elements.push(dot);
		}
	}

	function resetMain() {
		for (var key in playerIcons)
			playerIcons[key].classList.remove("disabled");

		main.textContent = ""; // Remove all children
		main.className = "mode-select";
		main.removeAttribute("data-winner");

		game.scoreElement.textContent = 0;

		game.playerId = null;

		game.boxes.elements.forEach(function(element) {
			element.classList.remove("p1", "p2", "p3", "p4");
		});

		game.lines.elements.forEach(function(element) {
			element.classList.remove("p1", "p2", "p3", "p4");
		});

		main.appendChild(game.boxes.container);
		main.appendChild(game.lines.container);
		main.appendChild(game.dots.container);
		main.appendChild(game.modes.container);
	}
	resetMain();

	socket = new WebSocket("ws://dotsandboxes.online/websocket");
	socket.onopen = function() {
		if (window.logging)
			console.log(socket);
	};
	socket.onmessage = function(event) {
		if (window.logging)
			console.log(event);

		var content = JSON.parse(event.data);
		if (!content || !content.type)
			return;

		switch (content.type.toLowerCase()) {
		case "init":
			game.playerId = content.player;

			document.body.className = "p" + game.playerId;
			for (var key in playerIcons)
				playerIcons[key].classList.toggle("current", parseInt(key) === content.current);

			main.removeAttribute("class");
			game.modes.container.remove();

			break;
		case "move":
			if (!content.line)
				break;

			var line = null
			switch (content.line.side) {
			case "t":
				line = game.cells[content.line.r][content.line.c].top;
				break;
			case "r":
				line = game.cells[content.line.r][content.line.c].right;
				break;
			case "b":
				line = game.cells[content.line.r][content.line.c].bottom;
				break;
			case "l":
				line = game.cells[content.line.r][content.line.c].left;
				break;
			}
			if (!line)
				break;

			line.classList.add("p" + content.player);

			if (Array.isArray(content.boxes)) {
				for (var i = 0; i < content.boxes.length; ++i)
					game.cells[content.boxes[i].r][content.boxes[i].c].box.classList.add("p" + content.player);

				if (game.playerId === content.player)
					game.scoreElement.textContent = parseInt(game.scoreElement.textContent) + content.boxes.length;
			}

			for (var key in playerIcons)
				playerIcons[key].classList.toggle("current", parseInt(key) === content.current);

			break;
		case "leave":
			for (var key in playerIcons)
				playerIcons[key].classList.remove("current");

			main.className = "leave";

			if (!isNaN(content.played))
				game.playedElement.textContent = content.played || 0;

			if (!isNaN(content.points))
				game.pointsElement.textContent = content.points || 0;

			var restart = main.appendChild(document.createElement("button"));
			restart.textContent = "New Game";
			restart.focus();
			restart.addEventListener("click", function (event) {
				socket.send(JSON.stringify({
					type: "restart"
				}));
				resetMain();
				this.remove();
			});
			break;
		case "end":
			for (var key in playerIcons)
				playerIcons[key].classList.remove("current");

			main.className = "restart";
			main.dataset.winner = content.winner;

			var restart = main.appendChild(document.createElement("button"));
			restart.textContent = "New Game";
			restart.focus();
			restart.addEventListener("click", function (event) {
				socket.send(JSON.stringify({
					type: "restart"
				}));
				resetMain();
				this.remove();
			});

			if (!isNaN(content.played))
				game.playedElement.textContent = content.played || 0;

			if (!isNaN(content.points))
				game.pointsElement.textContent = content.points || 0;

			if (game.playerId === content.winner)
				game.wonElement.textContent = parseInt(game.wonElement.textContent) + 1;

			break;
		}
	};
	socket.onerror = function(error) {
		console.error(error);
		socket.close();
	};
	socket.onclose = function() {
		if (window.logging)
			console.log(socket);

		if (typeof authenticationEnded === "function")
			authenticationEnded();
	};


	// ============================== //
	// ==========  Header  ========== //
	// ============================== //

	authenticationLink.addEventListener("click", function(event) {
		event.preventDefault();

		function authenticate(username, password, isRegister) {
			if (!authenticationForm || isEmpty(username) || isEmpty(password))
				return;

			var query = "username=" + username + "&password=" + password;
			if (isRegister)
				query += "&register=true";

			ajax("POST", "/authentication?" + query, function(xhr) {
				var content = JSON.parse(xhr.responseText);
				if (!content || content.error) {
					authenticationForm.classList.add("error");
					authenticationForm.dataset.error = (content && content.error) || "Invalid Username/Password";
					return;
				}

				authenticationLink.classList.remove("login");
				authenticationLink.classList.add("logout");
				authenticationLink.textContent = "Logout";
				authenticationLink.title = "Logout";

				authenticationForm.remove();
				authenticationForm = null;

				game.playedElement.textContent = parseInt(content.played) || 0;
				game.wonElement.textContent = parseInt(content.won) || 0;
				game.pointsElement.textContent = parseInt(content.points) || 0;
				game.modes.login();
			});
		}

		if (this.classList.contains("login")) {
			if (authenticationForm)
				return;

			authenticationForm = document.body.appendChild(document.createElement("form"));
			authenticationForm.id = "authentication";
			authenticationForm.action = "authentication";

			var usernameInput = authenticationForm.appendChild(document.createElement("input"));
			usernameInput.type = "text";
			usernameInput.placeholder = "Username";
			usernameInput.required = true;
			usernameInput.focus();

			var passwordInput = authenticationForm.appendChild(document.createElement("input"));
			passwordInput.type = "password";
			passwordInput.placeholder = "Password";
			passwordInput.required = true;

			var actionRow = authenticationForm.appendChild(document.createElement("div"));
			actionRow.classList.add("actions");

			var login = actionRow.appendChild(document.createElement("button"));
			login.textContent = "Login";
			login.addEventListener("click", function(event) {
				event.preventDefault();

				authenticate(usernameInput.value, passwordInput.value);
			});

			var register = actionRow.appendChild(document.createElement("button"));
			register.textContent = "Register";
			register.addEventListener("click", function(event) {
				event.preventDefault();

				authenticate(usernameInput.value, passwordInput.value, true);
			});

			var close = authenticationForm.appendChild(document.createElement("div"));
			close.classList.add("close");
			close.addEventListener("click", function(event) {
				authenticationForm.remove();
				authenticationForm = null;
			});
			return;
		}

		if (this.classList.contains("logout")) {
			if (!authenticationEnded) {
				authenticationEnded = function() { 
					ajax("POST", "/authentication?logout=true", function(xhr) {
						window.location.reload();
					});
				};
			}

			if (socket && socket.readyState === 1)
				socket.close();
			else
				authenticationEnded();

			return;
		}
	});
})();
