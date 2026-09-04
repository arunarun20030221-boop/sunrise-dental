<%--
  Management reports, backed by the PostgreSQL stored functions revenue_by_treatment() and
  dentist_workload(). Admin-only; the interceptor refuses the route for a receptionist.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" scope="request" value="Reports - Sunrise Dental Clinic"/>
<jsp:include page="../layout/header.jsp"/>

<div class="page-head">
    <h1>Management reports</h1>
</div>

<form method="get" action="${pageContext.request.contextPath}/reports" class="toolbar inline-form">
    <label for="from">From</label>
    <input type="date" id="from" name="from" value="${from}">
    <label for="to">To</label>
    <input type="date" id="to" name="to" value="${to}">
    <button type="submit">Update</button>
</form>

<section class="card">
    <h2>Revenue by treatment</h2>
    <p class="muted">Which treatments earn the clinic money, so the price list and appointment
        mix can be reviewed against evidence rather than impression.</p>
    <table class="grid">
        <thead>
        <tr>
            <th>Treatment</th>
            <th class="amount">Bills issued</th>
            <th class="amount">Total revenue (<c:out value="${clinic.currency}"/>)</th>
            <th class="amount">Average bill</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="row" items="${revenue}">
            <tr>
                <td><c:out value="${row.treatmentName}"/></td>
                <td class="amount">${row.appointmentCount}</td>
                <td class="amount">${row.totalRevenue}</td>
                <td class="amount">${row.averageBill}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</section>

<section class="card">
    <h2>Dentist workload</h2>
    <p class="muted">Chair time booked per dentist with a breakdown by outcome, to support
        staffing decisions and to show where no-shows are concentrated.</p>
    <table class="grid">
        <thead>
        <tr>
            <th>Dentist</th>
            <th class="amount">Booked</th>
            <th class="amount">Attended</th>
            <th class="amount">Cancelled</th>
            <th class="amount">No-show</th>
            <th class="amount">Chair time (min)</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="row" items="${workload}">
            <tr>
                <td><c:out value="${row.dentistName}"/></td>
                <td class="amount">${row.booked}</td>
                <td class="amount">${row.attended}</td>
                <td class="amount">${row.cancelled}</td>
                <td class="amount">${row.noShow}</td>
                <td class="amount">${row.totalMinutes}</td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</section>

<jsp:include page="../layout/footer.jsp"/>
