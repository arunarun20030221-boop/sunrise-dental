<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="pageTitle" scope="request" value="Diary - Sunrise Dental Clinic"/>
<jsp:include page="../layout/header.jsp"/>

<div class="page-head">
    <h1>Appointment diary</h1>
    <a class="btn" href="${pageContext.request.contextPath}/appointments/new">Register appointment</a>
</div>

<div class="toolbar">
    <%-- Date picker: reloads the diary for whichever day is chosen. --%>
    <form method="get" action="${pageContext.request.contextPath}/appointments" class="inline-form">
        <label for="date">Date</label>
        <input type="date" id="date" name="date" value="${date}">
        <button type="submit">Show</button>
    </form>

    <%-- Search accepts either an appointment number or part of a patient name. --%>
    <form method="get" action="${pageContext.request.contextPath}/appointments/search" class="inline-form">
        <label for="query">Find</label>
        <input type="text" id="query" name="query" placeholder="APT-2026-000001 or patient name" required>
        <button type="submit">Search</button>
    </form>
</div>

<c:choose>
    <c:when test="${empty appointments}">
        <p class="empty">No appointments booked for
            <fmt:parseDate value="${date}" pattern="yyyy-MM-dd" var="parsedDate" type="date"/>
            <fmt:formatDate value="${parsedDate}" pattern="d MMMM yyyy"/>.
        </p>
    </c:when>
    <c:otherwise>
        <table class="grid">
            <thead>
            <tr>
                <th>Time</th>
                <th>Appointment no.</th>
                <th>Patient</th>
                <th>Contact</th>
                <th>Dentist</th>
                <th>Treatment</th>
                <th>Status</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="a" items="${appointments}">
                <tr>
                    <td>${a.appointmentTime}</td>
                    <td class="mono">${a.appointmentNumber}</td>
                    <td><c:out value="${a.patient.name}"/></td>
                    <td><c:out value="${a.patient.contactNumber}"/></td>
                    <td><c:out value="${a.dentist.name}"/></td>
                    <td><c:out value="${a.treatmentType.name}"/></td>
                    <td><span class="status status-${a.status}">${a.status}</span></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/appointments/${a.appointmentNumber}">
                            View
                        </a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp"/>
