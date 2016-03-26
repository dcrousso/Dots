(function() {
	"use strict";

	var main = document.querySelector("main");
	var authenticationLink = document.querySelector(".link.authentication");
	var scoreElement = document.getElementById("score");
	var playedElement = document.getElementById("played");
	var wonElement = document.getElementById("won");
	var pointsElement = document.getElementById("points");
	var authenticationForm = null;

	function isEmpty(s) {
		return !s && !s.length && !s.trim().length;
	}

	function ajax(method, url, callback) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (xhr.readyState != 4 || xhr.status != 200)
				return;

			if (typeof callback === "function")
				callback(xhr);
		};
		xhr.open(method, url, true);
		xhr.send();
	}

	function authenticate(username, password, isRegister) {
		if (!authenticationForm || isEmpty(username) || isEmpty(password))
			return;

		var query = "username=" + username + "&password=" + password;
		if (isRegister)
			query += "&register=true";

		ajax("POST", "authentication?" + query, function(xhr) {
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

			playedElement.textContent = content.played || 0;
			wonElement.textContent = content.won || 0;
			pointsElement.textContent = content.points || 0;
		});
	}

	function distance(event, element) {
		if (!event)
			return Infinity;

		var rect = element.getBoundingClientRect();
		var x = (rect.left + rect.right) / 2;
		var y = (rect.top + rect.bottom) / 2;
		return Math.sqrt(Math.pow(event.clientX - x, 2) + Math.pow(event.clientY - y, 2));
	}

	function isFilled(element) {
		return /\bp\d+\b/.test(element.className);
	}

	// ============================== //
	// ==========  Header  ========== //
	// ============================== //

	authenticationLink.addEventListener("click", function(event) {
		event.preventDefault();

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
			ajax("GET", "authentication", function(xhr) {
				authenticationLink.classList.remove("logout");
				authenticationLink.classList.add("login");
				authenticationLink.textContent = "Login";
				authenticationLink.title = "Login";

				playedElement.textContent = 0;
				wonElement.textContent = 0;
				pointsElement.textContent = 0;
			});
			return;
		}
	});


	// ============================== //
	// ==========   Game   ========== //
	// ============================== //

	var playerId = null;
	var cells = [];
	var socket = null;

	function initGame(rows, cols) {
		main.textContent = ""; // Remove all children
		main.classList.add("waiting");

		var boxContainer = main.appendChild(document.createElement("div"));
		boxContainer.classList.add("container", "boxes");

		for (var i = 0; i < rows; ++i) {
			cells[i] = [];
			for (var j = 0; j < cols; ++j) {
				cells[i][j] = {};
				cells[i][j].box = boxContainer.appendChild(document.createElement("div"));
				cells[i][j].box.classList.add("box");
				cells[i][j].box.dataset.row = i;
				cells[i][j].box.dataset.col = j;
			}
		}

		var lineContainer = main.appendChild(document.createElement("div"));
		lineContainer.classList.add("container", "lines");

		for (var i = 0; i < rows + 1; ++i) {
			for (var j = 0; j < cols + 1; ++j) {
				if (i < rows) {
					var left = lineContainer.appendChild(document.createElement("div"));
					left.classList.add("line", "vertical");

					if (j < cols) {
						cells[i][j].left = left;
						if (j)
							cells[i][j - 1].right = cells[i][j].left;
					} else
						cells[i][j - 1].right = left;
				}

				if (j < cols) {
					var top = lineContainer.appendChild(document.createElement("div"));
					top.classList.add("line", "horizontal");

					if (i < rows) {
						cells[i][j].top = top;
						if (i)
							cells[i - 1][j].bottom = cells[i][j].top;
					} else
						cells[i - 1][j].bottom = top;
				}
			}
		}

		var currentLine = null;

		var dotContainer = main.appendChild(document.createElement("div"));
		dotContainer.classList.add("container", "dots");

		for (var i = 0; i < rows + 1; ++i) {
			for (var j = 0; j < cols + 1; ++j) {
				var dot = dotContainer.appendChild(document.createElement("div"));
				dot.classList.add("dot");
			}
		}

		function markLine(line) {
			if (currentLine === line || (line && isFilled(line)))
				return;

			if (currentLine)
				currentLine.classList.remove("hover");

			if (line) {
				line.classList.add("hover");
				currentLine = line;
			} else
				currentLine = null;
		}

		function handleMainMousemove(event) {
			if (!socket || socket.readyState === 3 )
				return;

			var element = document.elementFromPoint(event.clientX, event.clientY);
			if (element === main) {
				markLine();
				return;
			}

			var cell = cells[parseInt(element.dataset.row)][parseInt(element.dataset.col)];
			var top = isFilled(cell.top) ? Infinity : distance(event, cell.top);
			var right = isFilled(cell.right) ? Infinity : distance(event, cell.right);
			var bottom = isFilled(cell.bottom) ? Infinity : distance(event, cell.bottom);
			var left = isFilled(cell.left) ? Infinity : distance(event, cell.left);

			switch (Math.min(top, right, bottom, left)) {
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
		}
		main.addEventListener("mousemove", handleMainMousemove);

		function handleMainClick(event) {
			if (!socket || socket.readyState === 3 || !currentLine)
				return;

			var element = document.elementFromPoint(event.clientX, event.clientY);
			var row = parseInt(element.dataset.row);
			var col = parseInt(element.dataset.col);

			var cell = cells[row][col];

			var move = {
				type: "move",
				player: playerId,
				line: {
					r: row,
					c: col,
					side: null
				},
				boxes: []
			};

			function fillCell(row, col) {
				if (row < 0 || row > rows || col < 0 || col > cols)
					return;

				var cell = cells[row][col];
				if (!cell)
					return;
				if (currentLine !== cell.top && !isFilled(cell.top))
					return;
				if (currentLine !== cell.right && !isFilled(cell.right))
					return;
				if (currentLine !== cell.bottom && !isFilled(cell.bottom))
					return;
				if (currentLine !== cell.left && !isFilled(cell.left))
					return;

				move.boxes.push({r: row, c: col});
			}

			fillCell(row, col); // Fill current cell if able

			switch (currentLine) { // Fill adjacent cell if able
			case cell.top:
				fillCell(row - 1, col);
				move.line.side = "t";
				break;
			case cell.right:
				fillCell(row, col + 1);
				move.line.side = "r";
				break;
			case cell.bottom:
				fillCell(row + 1, col);
				move.line.side = "b";
				break;
			case cell.left:
				fillCell(row, col - 1);
				move.line.side = "l";
				break;
			}

			socket.send(JSON.stringify(move));

			markLine();
			handleMainMousemove(event);
		}
		main.addEventListener("click", handleMainClick);

		socket = new WebSocket("ws://localhost:8080/Dots/websocket");
		socket.onopen = function() {
			console.log(socket);
		};
		socket.onmessage = function(event) {
			console.log(event);

			var content = JSON.parse(event.data);
			if (!content || !content.type)
				return;

			switch (content.type.toLowerCase()) {
			case "init":
				playerId = content.player;
				for (var r = 0; r < cells.length; ++r){
					for (var c = 0; c < cells[r].length; ++c) {
						cells[r][c].box.classList.toggle("p" + content.board[r][c].x, !!content.board[r][c].x);
						cells[r][c].top.classList.toggle("p" + content.board[r][c].t, !!content.board[r][c].t);
						cells[r][c].right.classList.toggle("p" + content.board[r][c].r, !!content.board[r][c].r);
						cells[r][c].bottom.classList.toggle("p" + content.board[r][c].b, !!content.board[r][c].b);
						cells[r][c].left.classList.toggle("p" + content.board[r][c].l, !!content.board[r][c].l);
					}
				}
				main.classList.remove("waiting");
				break;
			case "move":
				var line = null
				switch (content.line.side) {
				case "t":
					line = cells[content.line.r][content.line.c].top;
					break;
				case "r":
					line = cells[content.line.r][content.line.c].right;
					break;
				case "b":
					line = cells[content.line.r][content.line.c].bottom;
					break;
				case "l":
					line = cells[content.line.r][content.line.c].left;
					break;
				}
				if (!line)
					break;

				line.classList.add("p" + content.player);

				for (var i = 0; i < content.boxes.length; ++i)
					cells[content.boxes[i].r][content.boxes[i].c].box.classList.add("p" + content.player);

				if (playerId === content.player)
					scoreElement.textContent = parseInt(scoreElement.textContent) + content.boxes.length;

				break;
			case "leave":
				alert("The Opponent left the game");
				socket.close();
				break;
			case "end":
				alert("Game Over!\nThe Winner is Player " + content.winner);
				playedElement.textContent = parseInt(playedElement.textContent) + 1;
				if (playerId === content.winner)
					wonElement.textContent = parseInt(wonElement.textContent) + 1;

				socket.close();
				break;
			}
		};
		socket.onerror = function(error) {
			console.error(error);
			socket.close();
		};
		socket.onclose = function() {
			socket = null;
		};
	}
	initGame(10, 10);
})();
