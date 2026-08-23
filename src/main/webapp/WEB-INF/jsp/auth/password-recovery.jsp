<%@ page contentType="text/html;charset=UTF-8" %><!doctype html><html lang="en"><head><%@ include file="../fragments/head.jspf" %></head>
<body class="auth-page"><main class="auth-workspace"><div class="auth-brand" aria-label="PharmaCare"><span class="brand-logo" aria-hidden="true">+</span><span class="brand-name">PharmaCare</span></div><section class="card auth-card" aria-labelledby="recovery-title"><div class="card-body"><header class="auth-heading"><h1 id="recovery-title" class="page-title">Recover password</h1><p class="page-subtitle">Enter your username or email and we will send recovery instructions.</p></header>
  <c:if test="${not empty recoveryMessage}"><div class="alert alert-success" role="status">${recoveryMessage}</div></c:if>
  <c:if test="${not empty recoveryHref}"><a class="btn btn-primary auth-submit" href="${pageContext.request.contextPath}${recoveryHref}">Continue to reset password</a></c:if>
  <form class="form-grid auth-form" method="post" action="${pageContext.request.contextPath}/password/recovery" data-submit-lock><%@ include file="../fragments/csrf.jspf" %>
    <div class="field"><label for="identity">Username or email</label><input id="identity" name="identity" value="${identity}" autocomplete="username"></div>
    <button class="btn btn-primary auth-submit" type="submit">Send recovery instructions</button></form>
  <div class="auth-secondary"><a href="${pageContext.request.contextPath}/login">Back to sign in</a></div>
</div></section><p class="auth-footnote">Secure pharmacy inventory and prescription management</p></main></body></html>
