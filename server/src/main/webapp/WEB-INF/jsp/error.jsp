<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sunrise Dental Clinic</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/site.css">
</head>
<body>
<main class="page">
    <section class="card">
        <h1><c:out value="${empty heading ? 'Something went wrong' : heading}"/></h1>
        <p><c:out value="${detail}"/></p>
        <a class="btn" href="${pageContext.request.contextPath}/appointments">Back to the diary</a>
    </section>
</main>
</body>
</html>
