(function() {
	"use strict";

	var main = document.querySelector("main");
	var scoreElement = document.getElementById("score");
	var playedElement = document.getElementById("played");
	var wonElement = document.getElementById("won");
	var pointsElement = document.getElementById("points");

	function ajax(method, url, callback) {
		var xhr = new XMLHttpRequest();
		xhr.onreadystatechange = function() {
			if (xhr.readyState != 4 || xhr.status != 200)
				return;

			callback(xhr);
		};
		xhr.open(method, url, true);
		xhr.send();
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
		if (!element)
			return false;
		return element.classList.contains("p1") || element.classList.contains("p2");
	}

	// ============================== //
	// ==========  Header  ========== //
	// ============================== //

	var authenticationLink = document.querySelector(".link.authentication");
	authenticationLink.addEventListener("click", function(event) {
		event.preventDefault();

		if (this.classList.contains("login")) {
			var form = document.body.appendChild(document.createElement("form"));
			form.id = "authentication";

			var usernameInput = form.appendChild(document.createElement("input"));
			usernameInput.type = "text";
			usernameInput.placeholder = "Username";
			usernameInput.required = true;

			var passwordInput = form.appendChild(document.createElement("input"));
			passwordInput.type = "password";
			passwordInput.placeholder = "Password";
			passwordInput.required = true;

			var submit = form.appendChild(document.createElement("button"));
			submit.textContent = "Login";
			submit.addEventListener("click", function(event) {
				event.preventDefault();

				if (!usernameInput.value || !passwordInput.value)
					return;

				var query = "username=" + usernameInput.value + "&password=" + passwordInput.value;

				ajax("POST", "authentication?" + query, function(xhr) {
					var json = JSON.parse(xhr.responseText);
					if (!json || json.error) {
						form.classList.add("error");
						form.dataset.error = json ? json.error : "Error";
						return;
					}

					authenticationLink.classList.remove("login");
					authenticationLink.classList.add("logout");
					authenticationLink.textContent = "Logout";
					authenticationLink.title = "Logout";
					form.remove();

					playedElement.textContent = json.played || 0;
					wonElement.textContent = json.won || 0;
					pointsElement.textContent = json.points || 0;
				});
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

	var currentPlayer = "p1";

	function initGame(rows, cols) {
		main.textContent = ""; // Remove all children

		var cells = [];

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
			if (currentLine === line || isFilled(line))
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
			if (!currentLine)
				return;

			var element = document.elementFromPoint(event.clientX, event.clientY);
			var row = parseInt(element.dataset.row);
			var col = parseInt(element.dataset.col);

			currentLine.classList.add(currentPlayer);

			var cell = cells[row][col];

			function fillCell(row, col) {
				if (row < 0 || row > rows || col < 0 || col > cols)
					return;

				var cell = cells[row][col];
				if (!isFilled(cell.top) || !isFilled(cell.right) || !isFilled(cell.bottom) || !isFilled(cell.left))
					return;

				cell.box.classList.add(currentPlayer);
				scoreElement.textContent = parseInt(scoreElement.textContent) + 1;
			}

			fillCell(row, col); // Fill current cell if able

			switch (currentLine) { // Fill adjacent cell if able
			case cell.top:
				fillCell(row - 1, col);
				break;
			case cell.right:
				fillCell(row, col + 1);
				break;
			case cell.bottom:
				fillCell(row + 1, col);
				break;
			case cell.left:
				fillCell(row, col - 1);
				break;
			}

			markLine();
			handleMainMousemove(event);
		}
		main.addEventListener("click", handleMainClick);
	}
	initGame(10, 10);
})();
