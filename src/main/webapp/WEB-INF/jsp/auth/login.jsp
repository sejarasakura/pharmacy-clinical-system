<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html><html lang="en"><head><%@ include file="../fragments/head.jspf" %></head>
<body class="auth-page"><main class="auth-workspace">
  <div class="auth-brand" aria-label="PharmaCare">
    <span class="brand-logo" aria-hidden="true">+</span>
    <span class="brand-name">PharmaCare</span>
  </div>
  <section class="card auth-card" aria-labelledby="login-title"><div class="card-body">
  <header class="auth-heading">
    <h1 id="login-title" class="page-title">Welcome back</h1>
    <p class="page-subtitle">Sign in to continue to PharmaCare.</p>
  </header>
  <c:if test="${not empty authenticationError}"><div class="alert alert-danger" role="alert">${authenticationError}</div></c:if>
  <%@ include file="../fragments/flash.jspf" %>
  <form class="form-grid auth-form" method="post" action="${pageContext.request.contextPath}/login" data-submit-lock>
    <%@ include file="../fragments/csrf.jspf" %><input type="hidden" name="returnTo" value="${returnTo}">
    <div class="field"><label for="identifier">Identifier</label><input id="identifier" name="identifier" value="${identifier}" autocomplete="username" class="${not empty identifierError ? 'input-error' : ''}" aria-describedby="${not empty identifierError ? 'identifier-error' : ''}" ${not empty identifierError ? 'autofocus' : ''}>
      <c:if test="${not empty identifierError}"><div id="identifier-error" class="field-error">${identifierError}</div></c:if></div>
    <div class="field"><label for="password">Password</label><div class="input-with-action"><input id="password" name="password" type="password" autocomplete="current-password" class="${not empty passwordError ? 'input-error' : ''}" aria-describedby="${not empty passwordError ? 'password-error' : ''}"><button class="btn btn-secondary" type="button" data-password-toggle="password" aria-pressed="false">Show</button></div>
      <c:if test="${not empty passwordError}"><div id="password-error" class="field-error">${passwordError}</div></c:if></div>
    <button class="btn btn-primary auth-submit" type="submit">Sign In</button>
  </form>
  <div class="auth-secondary"><a href="${pageContext.request.contextPath}/password/recovery">Forgot Password?</a></div>
</div></section>
  <p class="auth-footnote">Secure pharmacy inventory and prescription management</p>
</main></body></html>
