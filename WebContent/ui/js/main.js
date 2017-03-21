(function() {
	"use strict";

	var playerIcons = [
		document.querySelector("header > nav > .players > div.p1"),
		document.querySelector("header > nav > .players > div.p2"),
		document.querySelector("header > nav > .players > div.p3"),
		document.querySelector("header > nav > .players > div.p4")
	];
	var main = document.querySelector("main");

	// https://github.com/WICG/EventListenerOptions/blob/gh-pages/explainer.md#feature-detection
	var eventListenerOptions = (function(){var s=!1;try{window.addEventListener("test",null,Object.defineProperty({},"passive",{get:function(){s=!0}}))}catch(n){}return s})() ? {passive: true} : undefined;

	function isEmpty(s) {
		return !s && !s.length && !s.trim().length;
	}

	function ajax(method, url, callback) {
		var xhr = new XMLHttpRequest();
		xhr.addEventListener("readystatechange", function(event) {
			if (xhr.readyState !== XMLHttpRequest.DONE || xhr.status !== 200)
				return;

			if (typeof callback === "function")
				callback(xhr);
		}, eventListenerOptions);
		xhr.open(method, url, true);
		xhr.send();
	}

	function invokeSoon(callback) {
		var animationFrame = window.requestAnimationFrame
		                  || window.webkitRequestAnimationFrame
		                  || window.mozRequestAnimationFrame
		                  || window.oRequestAnimationFrame
		                  || window.msRequestAnimationFrame
		                  || function(func) {
		                     	window.setTimeout(func, 0);
		                     };
		animationFrame(callback);
	}

	function notify(body) {
		if (!Notification)
			return;

		if (!document.hidden)
			return;

		var options = {
			body: body,
			icon: "http://devinrousso.com:8080/dots/favicon-194x194.png"
		};
		if (Notification.permission !== "denied") {
			Notification.requestPermission(function(permission) {
				new Notification("Dots", options);
			});
		} else if (Notification.permission === "granted")
			new Notification("Dots", options);
	}


	// ================================================== //
	// ===============       Styles       =============== //
	// ================================================== //

	invokeSoon(function() {
		var mainCSS = document.head.appendChild(document.createElement("link"));
		mainCSS.rel = "stylesheet";
		mainCSS.href = "/dots/ui/css/main.css";
	});


	// ================================================== //
	// ===============        Game        =============== //
	// ================================================== //

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
		socket: null
	};

	game.modes.container = document.createElement("div");
	game.modes.container.classList.add("modes");
	game.modes.handleClick = function(mode) {
		return function(event) {
			if (!game.socket || game.socket.readyState !== 1)
				return;

			playerIcons[2].classList.toggle("disabled", mode < 3);
			playerIcons[3].classList.toggle("disabled", mode < 4);

			game.socket.send(JSON.stringify({mode: mode}));
			main.className = "waiting";
		};
	};

	game.modes.ai = game.modes.container.appendChild(document.createElement("button"));
	game.modes.ai.textContent = "Play AI";
	game.modes.ai.addEventListener("click", game.modes.handleClick(1), eventListenerOptions);

	game.modes.two = game.modes.container.appendChild(document.createElement("button"));
	game.modes.two.textContent = "Two Players";
	game.modes.two.addEventListener("click", game.modes.handleClick(2), eventListenerOptions);

	game.modes.three = game.modes.container.appendChild(document.createElement("button"));
	game.modes.three.textContent = "Three Players";
	game.modes.three.addEventListener("click", game.modes.handleClick(3), eventListenerOptions);

	game.modes.four = game.modes.container.appendChild(document.createElement("button"));
	game.modes.four.textContent = "Four Players";
	game.modes.four.addEventListener("click", game.modes.handleClick(4), eventListenerOptions);

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
		default:
		case Infinity: // Current cell is filled
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
	}, eventListenerOptions);

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

		if (game.socket && game.socket.readyState === WebSocket.OPEN)
			game.socket.send(JSON.stringify(move));
	}, eventListenerOptions);

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
		playerIcons.forEach(function(icon) {
			icon.classList.remove("disabled");
		});

		document.body.className = "";

		main.textContent = ""; // Remove all children
		main.className = "mode-select";
		main.removeAttribute("data-winner");

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
	invokeSoon(resetMain);

	game.socket = new WebSocket("ws://devinrousso.com:8080/dots/websocket");
	game.socket.addEventListener("message", function(event) {
		var content = JSON.parse(event.data);
		if (!content || !content.type)
			return;

		switch (content.type.toLowerCase()) {
		case "init":
			game.playerId = content.player;

			document.body.className = "p" + game.playerId;
			playerIcons.forEach(function(icon, i) {
				icon.classList.toggle("current", (i + 1) === content.current);
			});

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
			}

			playerIcons.forEach(function(icon, i) {
				icon.classList.toggle("current", (i + 1) === content.current);
			});

			if (game.playerId === content.current)
				notify("It's your move.");

			break;
		case "leave":
			playerIcons.forEach(function(icon) {
				icon.classList.remove("disabled");
			});

			main.className = "leave";

			var restart = main.appendChild(document.createElement("button"));
			restart.textContent = "New Game";
			restart.focus();
			restart.addEventListener("click", function (event) {
				game.socket.send(JSON.stringify({
					type: "restart"
				}));
				resetMain();
				this.remove();
			});

			notify("An opponent left.");
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
				game.socket.send(JSON.stringify({
					type: "restart"
				}));
				resetMain();
				this.remove();
			});

			notify("Player " + content.winner + " won!");
			break;
		}
	}, eventListenerOptions);
	game.socket.addEventListener("error", function(event) {
		console.error(event);
		game.socket.close();
	}, eventListenerOptions);
	game.socket.addEventListener("close", function(event) {
		main.className = "disconnected";

		var reload = main.appendChild(document.createElement("button"));
		reload.textContent = "Reload";
		reload.focus();
		reload.addEventListener("click", function (event) {
			window.location.reload();
			this.remove();
		}, eventListenerOptions);
	}, eventListenerOptions);
})();
