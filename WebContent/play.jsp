<jsp:include page="/WEB-INF/templates/head.jsp">
	<jsp:param name="pagetype" value="game"/>
</jsp:include>
		<header>
			<nav>
				<a href="${pageContext.request.contextPath}/" title="Homepage">Home</a>
				<a href="${pageContext.request.contextPath}/logout" title="Logout">Logout</a>
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
<jsp:include page="/WEB-INF/templates/footer.jsp">
	<jsp:param name="pagetype" value="game"/>
</jsp:include>
