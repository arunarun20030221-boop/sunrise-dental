<%--
  Login page. Standalone rather than using the shared header, because the header renders the
  navigation and the signed-in user, neither of which exists yet.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in &middot; Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/site.css">
</head>
<body class="login-body">
<div class="login-card">
    <h1>Sunrise Dental Clinic</h1>
    <p class="sub">Staff sign-in</p>

    <c:if test="${not empty error}">
        <div class="alert alert-error"><c:out value="${error}"/></div>
    </c:if>
    <c:if test="${loggedOut}">
        <div class="alert alert-ok">You have been signed out.</div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/login">
        <label for="username">Username</label>
        <input type="text" id="username" name="username" required autofocus
               value="<c:out value='${username}'/>">

        <label for="password">Password</label>
        <input type="password" id="password" name="password" required>

        <button type="submit">Sign in</button>
    </form>

    <p class="hint">
        Authorised clinic staff only. Contact the clinic administrator if you cannot sign in.
    </p>
</div>
</body>
</html>
