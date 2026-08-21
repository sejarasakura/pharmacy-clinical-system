<%@ page contentType="text/html;charset=UTF-8" %><!doctype html><html lang="en"><head><%@ include file="../fragments/head.jspf" %></head><body>
<c:choose>
  <c:when test="${authenticated}">
    <%@ include file="../fragments/shell.jspf" %>
    <div class="status-workspace"><%@ include file="../fragments/status-panel.jspf" %></div>
    </main></div></div>
  </c:when>
  <c:otherwise>
    <main class="auth-workspace">
      <div class="auth-brand" aria-label="PharmaCare"><span class="auth-brand-mark">P</span><span>PharmaCare</span></div>
      <div class="status-workspace status-workspace-standalone"><%@ include file="../fragments/status-panel.jspf" %></div>
    </main>
  </c:otherwise>
</c:choose>
</body></html>
