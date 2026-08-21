<%@ page contentType="text/html;charset=UTF-8" %><!doctype html><html lang="en"><head><%@ include file="../fragments/head.jspf" %></head><body>
<%@ include file="../fragments/shell.jspf" %>
<div class="status-workspace">
  <c:set var="statusCode" value="403"/><c:set var="statusTone" value="warning"/>
  <c:set var="statusTitle" value="Access denied"/>
  <c:set var="statusMessage" value="Your account cannot access this function."/>
  <c:set var="primaryHref" value="${homeHref}"/><c:set var="primaryLabel" value="Go to authorised home"/>
  <c:set var="showBack" value="true"/>
  <%@ include file="../fragments/status-panel.jspf" %>
</div>
</main></div></div></body></html>
