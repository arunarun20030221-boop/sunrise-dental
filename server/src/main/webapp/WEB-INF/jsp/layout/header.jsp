<%--
  Shared page top: doctype, stylesheet link and the navigation bar.
  Included by every page so the markup and the menu exist in one place only.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${empty pageTitle ? 'Sunrise Dental Clinic' : pageTitle}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/site.css">
</head>
<body>
<header class="topbar">
    <div class="topbar-inner">
        <a class="brand" href="${pageContext.request.contextPath}/appointments">
            <c:out value="${clinic.name}"/>
        </a>
        <nav>
            <a href="${pageContext.request.contextPath}/appointments">Diary</a>
            <a href="${pageContext.request.contextPath}/appointments/new">New appointment</a>
            <%-- The reports link is hidden for receptionists. The interceptor blocks the route
                 regardless; hiding the link avoids offering an action that would be refused. --%>
            <c:if test="${currentUser.admin}">
                <a href="${pageContext.request.contextPath}/reports">Reports</a>
            </c:if>
            <a href="${pageContext.request.contextPath}/help">Help</a>
        </nav>
        <div class="session">
            <span class="who"><c:out value="${currentUser.fullName}"/></span>
            <span class="role"><c:out value="${currentUser.role}"/></span>
            <a class="logout" href="${pageContext.request.contextPath}/logout">Exit</a>
        </div>
    </div>
</header>
<main class="page">
    <c:if test="${not empty message}">
        <div class="alert alert-ok"><c:out value="${message}"/></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
