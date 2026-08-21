<%@ page contentType="text/html;charset=UTF-8" %><!doctype html><html lang="en"><head><%@ include file="../fragments/head.jspf" %></head><body><%@ include file="../fragments/shell.jspf" %>
<div class="security-dialog-layer" data-modal>
  <section class="security-dialog" role="dialog" aria-modal="true" aria-labelledby="change-password-title" aria-describedby="change-password-description">
    <header class="security-dialog-header">
      <div><h1 id="change-password-title" class="page-title">Change password</h1><p id="change-password-description" class="page-subtitle">Update the password used to access your PharmaCare account.</p></div>
      <a class="dialog-close" href="${pageContext.request.contextPath}/profile" aria-label="Close change password dialog" data-modal-cancel>×</a>
    </header>
    <form class="security-form" method="post" action="${pageContext.request.contextPath}/profile/password" data-submit-lock><%@ include file="../fragments/csrf.jspf" %>
      <div class="security-dialog-body">
        <c:if test="${not empty passwordError}"><div class="alert alert-danger" role="alert">${passwordError}</div></c:if>
        <div class="field"><label for="currentPassword">Current password</label><div class="input-with-action"><input id="currentPassword" name="currentPassword" type="password" autocomplete="current-password" ${not empty passwordError ? 'class="input-error" aria-describedby="currentPassword-error" autofocus' : 'autofocus'}><button class="btn btn-secondary" type="button" data-password-toggle="currentPassword" aria-pressed="false">Show</button></div><c:if test="${not empty passwordError}"><div id="currentPassword-error" class="field-error">Check your current password and try again.</div></c:if></div>
        <div class="field"><label for="newPassword">New password</label><div class="input-with-action"><input id="newPassword" name="newPassword" type="password" autocomplete="new-password" ${not empty newPasswordError ? 'class="input-error" aria-describedby="newPassword-policy newPassword-error"' : 'aria-describedby="newPassword-policy"'}><button class="btn btn-secondary" type="button" data-password-toggle="newPassword" aria-pressed="false">Show</button></div><c:if test="${not empty newPasswordError}"><div id="newPassword-error" class="field-error">${newPasswordError}</div></c:if></div>
        <div class="field"><label for="confirmPassword">Confirm new password</label><div class="input-with-action"><input id="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" ${not empty confirmPasswordError ? 'class="input-error" aria-describedby="confirmPassword-error"' : ''}><button class="btn btn-secondary" type="button" data-password-toggle="confirmPassword" aria-pressed="false">Show</button></div><c:if test="${not empty confirmPasswordError}"><div id="confirmPassword-error" class="field-error">${confirmPasswordError}</div></c:if></div>
        <div id="newPassword-policy" class="password-policy"><span class="password-policy-icon" aria-hidden="true">✓</span><div><strong>Password requirement</strong><span>Use at least eight characters.</span></div></div>
      </div>
      <footer class="security-dialog-footer"><a class="btn btn-secondary" href="${pageContext.request.contextPath}/profile" data-modal-cancel>Cancel</a><button class="btn btn-primary" type="submit">Change Password</button></footer>
    </form>
  </section>
</div>
</main></div></div></body></html>
