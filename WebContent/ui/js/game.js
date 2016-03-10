(function() {
	"use strict";

	var currentPlayer = "p1";

	var rows = 10;
	var cols = 10;

	var main = document.querySelector("main");

	var boxContainer = main.querySelector(".boxes");
	var boxElements = [];
	for (var i = 0; i < rows; ++i) {
		boxElements[i] = [];
		for (var j = 0; j < cols; ++j) {
			boxElements[i][j] = boxContainer.appendChild(document.createElement("div"));
			boxElements[i][j].classList.add("box");
			boxElements[i][j].dataset.row = i;
			boxElements[i][j].dataset.col = j;
		}
	}

	var lineContainer = main.querySelector(".lines");
	var lineElements = [];
	for (var i = 0; i < rows + 1; ++i) {
		lineElements[i] = [];
		for (var j = 0; j < cols + 1; ++j) {
			lineElements[i][j] = {};
			if (i < rows) {
				lineElements[i][j].vertical = lineContainer.appendChild(document.createElement("div"));
				lineElements[i][j].vertical.classList.add("line", "vertical");
				lineElements[i][j].vertical.dataset.row = i;
				lineElements[i][j].vertical.dataset.col = j;
			}
			if (j < cols) {
				lineElements[i][j].horizontal = lineContainer.appendChild(document.createElement("div"));
				lineElements[i][j].horizontal.classList.add("line", "horizontal");
				lineElements[i][j].horizontal.dataset.row = i;
				lineElements[i][j].horizontal.dataset.col = j;
			}
		}
	}

	var dotContainer = main.querySelector(".dots");
	for (var i = 0; i < rows + 1; ++i) {
		for (var j = 0; j < cols + 1; ++j) {
			var dot = dotContainer.appendChild(document.createElement("div"));
			dot.classList.add("dot");
		}
	}

	function getAdjacentLines(row, col, event) {
		row = parseInt(row);
		col = parseInt(col);

		function distance(element) {
			if (!event)
				return Infinity;

			var rect = element.getBoundingClientRect();
			var x = (rect.left + rect.right) / 2;
			var y = (rect.top + rect.bottom) / 2;
			return Math.sqrt(Math.pow(event.clientX - x, 2) + Math.pow(event.clientY - y, 2));
		}

		function createLineObject(line) {
			if (!line || isNaN(row) || isNaN(col))
				return {};

			var object = {element: line};
			object.row = parseInt(line.dataset.row);
			object.col = parseInt(line.dataset.col);
			object.selected = line.classList.contains("p1") || line.classList.contains("p2");
			object.distance = object.selected ? Infinity : distance(object.element);
			return object;
		}

		return {
			top: createLineObject(lineElements[row][col].horizontal),
			right: createLineObject(col < cols ? lineElements[row][col + 1].vertical : null),
			bottom: createLineObject(row < rows ? lineElements[row + 1][col].horizontal : null),
			left: createLineObject(lineElements[row][col].vertical)
		};
	}

	function markLine(line) {
		if (currentLine && line && currentLine.element === line.element)
			return;

		if (currentLine)
			currentLine.element.classList.remove("hover");

		if (line && !line.selected) {
			line.element.classList.add("hover");
			currentLine = line;
		} else
			currentLine = null;
	}

	var currentLine = null;
	function handleMainMousemove(event) {
		var element = document.elementFromPoint(event.clientX, event.clientY);
		if (element === main) {
			markLine();
			return;
		}

		var lines = getAdjacentLines(parseInt(element.dataset.row), parseInt(element.dataset.col), event);
		switch (Math.min(lines.top.distance, lines.right.distance, lines.bottom.distance, lines.left.distance)) {
		case lines.top.distance:
			if (!lines.top.selected)
				markLine(lines.top);
			break;
		case lines.right.distance:
			if (!lines.right.selected)
				markLine(lines.right);
			break;
		case lines.bottom.distance:
			if (!lines.bottom.selected)
				markLine(lines.bottom);
			break;
		case lines.left.distance:
			if (!lines.left.selected)
				markLine(lines.left);
			break;
		}
	}
	main.addEventListener("mousemove", handleMainMousemove);

	function handleMainClick(event) {
		if (!currentLine)
			return;

		function checkAdjacent(row, col) {
			if (row < 0 || row > rows || col < 0 || col > cols)
				return false;

			var lines = getAdjacentLines(row, col);
			for (var key in lines) {
				if (!lines[key].selected)
					return false;
			}
			return true;
		}

		currentLine.element.classList.add(currentPlayer);

		var filled = false;
		if (checkAdjacent(currentLine.row, currentLine.col)) {
			boxElements[currentLine.row][currentLine.col].classList.add(currentPlayer);
			filled = true;
		}

		if (currentLine.element.classList.contains("horizontal")) {
			if (checkAdjacent(currentLine.row - 1, currentLine.col)) {
				boxElements[currentLine.row - 1][currentLine.col].classList.add(currentPlayer);
				filled = true;
			}
		} else if (currentLine.element.classList.contains("vertical")) {
			if (checkAdjacent(currentLine.row, currentLine.col - 1)) {
				boxElements[currentLine.row][currentLine.col - 1].classList.add(currentPlayer);
				filled = true;
			}
		}

		markLine();
		handleMainMousemove(event);

		if (!filled)
			currentPlayer = currentPlayer === "p1" ? "p2" : "p1";
	}
	main.addEventListener("click", handleMainClick);
})();
