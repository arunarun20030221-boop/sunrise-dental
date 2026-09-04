<%--
  Requirement 4 - the printable patient bill/receipt.
  The print stylesheet in site.css hides the navigation and buttons, so what reaches the printer
  is just the receipt itself.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" scope="request" value="Receipt ${bill.billNumber} - Sunrise Dental Clinic"/>
<jsp:include page="../layout/header.jsp"/>

<div class="page-head no-print">
    <h1>Receipt</h1>
    <div class="button-row">
        <button type="button" onclick="window.print()">Print receipt</button>
        <a class="btn btn-quiet"
           href="${pageContext.request.contextPath}/appointments/${bill.appointment.appointmentNumber}">
            Back to appointment
        </a>
    </div>
</div>

<section class="receipt">
    <header class="receipt-head">
        <h2><c:out value="${clinic.name}"/></h2>
        <p>
            <c:out value="${clinic.address}"/><br>
            Tel: <c:out value="${clinic.phone}"/>
        </p>
    </header>

    <table class="receipt-meta">
        <tr>
            <th>Receipt no.</th>
            <td class="mono">${bill.billNumber}</td>
            <th>Issued</th>
            <td>${bill.issuedAt}</td>
        </tr>
        <tr>
            <th>Appointment no.</th>
            <td class="mono">${bill.appointment.appointmentNumber}</td>
            <th>Issued by</th>
            <td><c:out value="${bill.issuedBy}"/></td>
        </tr>
        <tr>
            <th>Patient</th>
            <td><c:out value="${bill.appointment.patient.name}"/></td>
            <th>Dentist</th>
            <td><c:out value="${bill.appointment.dentist.name}"/></td>
        </tr>
        <tr>
            <th>Date of visit</th>
            <td>${bill.appointment.appointmentDate}</td>
            <th>Contact</th>
            <td><c:out value="${bill.appointment.patient.contactNumber}"/></td>
        </tr>
    </table>

    <table class="receipt-lines">
        <thead>
        <tr>
            <th>Description</th>
            <th class="amount">Amount (<c:out value="${clinic.currency}"/>)</th>
        </tr>
        </thead>
        <tbody>
        <tr>
            <td>Consultation fee</td>
            <td class="amount">${bill.consultationFee}</td>
        </tr>
        <tr>
            <td><c:out value="${bill.appointment.treatmentType.name}"/></td>
            <td class="amount">${bill.treatmentCost}</td>
        </tr>
        <c:if test="${bill.adjustment != 0}">
            <tr>
                <td><c:out value="${bill.adjustmentReason}"/></td>
                <td class="amount">${bill.adjustment}</td>
            </tr>
        </c:if>
        </tbody>
        <tfoot>
        <tr>
            <th>Total paid</th>
            <th class="amount">${bill.total}</th>
        </tr>
        </tfoot>
    </table>

    <footer class="receipt-foot">
        <p>Thank you for visiting <c:out value="${clinic.name}"/>. Please retain this receipt.</p>
    </footer>
</section>

<jsp:include page="../layout/footer.jsp"/>
