<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" scope="request" value="Search results - Sunrise Dental Clinic"/>
<jsp:include page="../layout/header.jsp"/>

<div class="page-head">
    <h1>Results for &ldquo;<c:out value="${query}"/>&rdquo;</h1>
    <a class="btn btn-quiet" href="${pageContext.request.contextPath}/appointments">Back to diary</a>
</div>

<c:choose>
    <c:when test="${empty results}">
        <p class="empty">No appointments found for a patient matching
            &ldquo;<c:out value="${query}"/>&rdquo;.</p>
    </c:when>
    <c:otherwise>
        <table class="grid">
            <thead>
            <tr>
                <th>Date</th>
                <th>Time</th>
                <th>Appointment no.</th>
                <th>Patient</th>
                <th>Dentist</th>
                <th>Treatment</th>
                <th>Status</th>
                <th></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="a" items="${results}">
                <tr>
                    <td>${a.appointmentDate}</td>
                    <td>${a.appointmentTime}</td>
                    <td class="mono">${a.appointmentNumber}</td>
                    <td><c:out value="${a.patient.name}"/></td>
                    <td><c:out value="${a.dentist.name}"/></td>
                    <td><c:out value="${a.treatmentType.name}"/></td>
                    <td><span class="status status-${a.status}">${a.status}</span></td>
                    <td>
                        <a href="${pageContext.request.contextPath}/appointments/${a.appointmentNumber}">View</a>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:otherwise>
</c:choose>

<jsp:include page="../layout/footer.jsp"/>
