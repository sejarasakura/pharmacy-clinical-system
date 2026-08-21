<%@ page contentType="text/html;charset=UTF-8" %><!doctype html><html lang="en"><head><%@ include file="../fragments/head.jspf" %></head><body>
<%@ include file="../fragments/shell.jspf" %>
<div class="status-workspace">
  <c:set var="statusCode" value="404"/><c:set var="statusTone" value="neutral"/>
  <c:set var="statusTitle" value="Page not found"/>
  <c:set var="statusMessage" value="The requested page or record is unavailable."/>
  <c:set var="primaryHref" value="${homeHref}"/><c:set var="primaryLabel" value="Return home"/>
  <c:set var="showBack" value="true"/>
  <%@ include file="../fragments/status-panel.jspf" %>
</div>
</main></div></div></body></html>
