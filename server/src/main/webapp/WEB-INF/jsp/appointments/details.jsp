<%--
  Requirement 3 - display complete patient and appointment information for one appointment
  number, together with the calculated bill preview from requirement 4.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" scope="request" value="${appointment.appointmentNumber} - Sunrise Dental Clinic"/>
<jsp:include page="../layout/header.jsp"/>

<div class="page-head">
    <h1>Appointment <span class="mono">${appointment.appointmentNumber}</span></h1>
    <span class="status status-${appointment.status}">${appointment.status}</span>
</div>

<div class="columns">
    <section class="card">
        <h2>Patient</h2>
        <dl>
            <dt>Name</dt>
            <dd><c:out value="${appointment.patient.name}"/></dd>
            <dt>Address</dt>
            <dd><c:out value="${appointment.patient.address}"/></dd>
            <dt>Contact</dt>
            <dd><c:out value="${appointment.patient.contactNumber}"/></dd>
            <dt>Email</dt>
            <dd>
                <c:choose>
                    <c:when test="${empty appointment.patient.email}"><span class="muted">Not given</span></c:when>
                    <c:otherwise><c:out value="${appointment.patient.email}"/></c:otherwise>
                </c:choose>
            </dd>
        </dl>
    </section>

    <section class="card">
        <h2>Appointment</h2>
        <dl>
            <dt>Dentist</dt>
            <dd><c:out value="${appointment.dentist.name}"/></dd>
            <dt>Treatment</dt>
            <dd><c:out value="${appointment.treatmentType.name}"/></dd>
            <dt>Date</dt>
            <dd>${appointment.appointmentDate}</dd>
            <dt>Time</dt>
            <dd>${appointment.appointmentTime} &ndash; ${appointment.endsAt.toLocalTime()}</dd>
            <dt>Sessions</dt>
            <dd>${appointment.sessionCount}</dd>
            <dt>Registered by</dt>
            <dd><c:out value="${appointment.createdBy}"/></dd>
        </dl>
        <c:if test="${not empty appointment.notes}">
            <p class="notes"><strong>Notes:</strong> <c:out value="${appointment.notes}"/></p>
        </c:if>
    </section>

    <section class="card">
        <h2>Bill</h2>
        <table class="totals">
            <tr>
                <td>Consultation fee</td>
                <td class="amount">${clinic.currency} ${preview.consultationFee}</td>
            </tr>
            <tr>
                <td><c:out value="${appointment.treatmentType.name}"/></td>
                <td class="amount">${clinic.currency} ${preview.treatmentCost}</td>
            </tr>
            <c:if test="${preview.adjustment > 0}">
                <tr>
                    <td><c:out value="${preview.adjustmentReason}"/></td>
                    <td class="amount">${clinic.currency} ${preview.adjustment}</td>
                </tr>
            </c:if>
            <tr class="total-row">
                <td>Total payable</td>
                <td class="amount">${clinic.currency} ${preview.total}</td>
            </tr>
        </table>

        <c:choose>
            <c:when test="${hasBill}">
                <a class="btn" href="${pageContext.request.contextPath}/bills/${appointment.appointmentNumber}">
                    View / print receipt
                </a>
            </c:when>
            <c:otherwise>
                <form method="post"
                      action="${pageContext.request.contextPath}/bills/${appointment.appointmentNumber}">
                    <button type="submit">Issue bill</button>
                </form>
            </c:otherwise>
        </c:choose>
    </section>
</div>

<%-- Status actions are only offered while the appointment is still open. --%>
<c:if test="${appointment.status == 'BOOKED'}">
    <section class="card">
        <h2>Update status</h2>
        <div class="button-row">
            <form method="post"
                  action="${pageContext.request.contextPath}/appointments/${appointment.appointmentNumber}/attended">
                <button type="submit">Mark attended</button>
            </form>
            <form method="post"
                  action="${pageContext.request.contextPath}/appointments/${appointment.appointmentNumber}/no-show">
                <button type="submit" class="btn-quiet">Record no-show</button>
            </form>
            <form method="post"
                  action="${pageContext.request.contextPath}/appointments/${appointment.appointmentNumber}/cancel">
                <button type="submit" class="btn-danger">Cancel appointment</button>
            </form>
        </div>
    </section>
</c:if>

<jsp:include page="../layout/footer.jsp"/>
