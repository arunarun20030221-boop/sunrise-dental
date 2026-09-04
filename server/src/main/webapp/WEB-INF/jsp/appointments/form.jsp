<%--
  Requirement 2 - register a new appointment.
  Uses Spring's form tags so that validation messages from the Bean Validation annotations on
  AppointmentRequest appear beside the field that caused them, with the typed value preserved.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<c:set var="pageTitle" scope="request" value="Register appointment - Sunrise Dental Clinic"/>
<jsp:include page="../layout/header.jsp"/>

<div class="page-head">
    <h1>Register appointment</h1>
    <a class="btn btn-quiet" href="${pageContext.request.contextPath}/appointments">Back to diary</a>
</div>

<form:form modelAttribute="appointmentRequest"
           action="${pageContext.request.contextPath}/appointments"
           method="post" cssClass="card form-grid">

    <fieldset>
        <legend>Patient</legend>

        <div class="field">
            <label for="patientName">Patient name</label>
            <form:input path="patientName" id="patientName" maxlength="100"/>
            <form:errors path="patientName" cssClass="field-error"/>
        </div>

        <div class="field">
            <label for="contactNumber">Contact number</label>
            <form:input path="contactNumber" id="contactNumber" placeholder="0771234567"/>
            <form:errors path="contactNumber" cssClass="field-error"/>
            <small>An existing patient is recognised by this number, so their record is reused.</small>
        </div>

        <div class="field field-wide">
            <label for="address">Address</label>
            <form:input path="address" id="address" maxlength="255"/>
            <form:errors path="address" cssClass="field-error"/>
        </div>

        <div class="field">
            <label for="email">Email <span class="optional">(optional)</span></label>
            <form:input path="email" id="email" type="email"/>
            <form:errors path="email" cssClass="field-error"/>
            <small>A confirmation email is sent when an address is given.</small>
        </div>
    </fieldset>

    <fieldset>
        <legend>Appointment</legend>

        <div class="field">
            <label for="dentistId">Dentist</label>
            <form:select path="dentistId" id="dentistId">
                <form:option value="" label="-- select --"/>
                <c:forEach var="d" items="${dentists}">
                    <form:option value="${d.id}" label="${d.name} (${d.speciality})"/>
                </c:forEach>
            </form:select>
            <form:errors path="dentistId" cssClass="field-error"/>
        </div>

        <div class="field">
            <label for="treatmentTypeId">Treatment</label>
            <form:select path="treatmentTypeId" id="treatmentTypeId">
                <form:option value="" label="-- select --"/>
                <c:forEach var="t" items="${treatments}">
                    <form:option value="${t.id}"
                                 label="${t.name} - ${clinic.currency} ${t.baseCost} (${t.durationMinutes} min)"/>
                </c:forEach>
            </form:select>
            <form:errors path="treatmentTypeId" cssClass="field-error"/>
        </div>

        <div class="field">
            <label for="appointmentDate">Date</label>
            <form:input path="appointmentDate" id="appointmentDate" type="date"/>
            <form:errors path="appointmentDate" cssClass="field-error"/>
        </div>

        <div class="field">
            <label for="appointmentTime">Time</label>
            <form:input path="appointmentTime" id="appointmentTime" type="time"/>
            <form:errors path="appointmentTime" cssClass="field-error"/>
            <small>Clinic hours: ${clinic.openingTime} to ${clinic.closingTime}.</small>
        </div>

        <div class="field">
            <label for="sessionCount">Sessions</label>
            <form:input path="sessionCount" id="sessionCount" type="number" min="1" max="10"/>
            <form:errors path="sessionCount" cssClass="field-error"/>
            <small>Root canal treatment is charged per session.</small>
        </div>

        <div class="field field-wide">
            <label for="notes">Notes <span class="optional">(optional)</span></label>
            <form:textarea path="notes" id="notes" rows="3" maxlength="500"/>
            <form:errors path="notes" cssClass="field-error"/>
        </div>
    </fieldset>

    <div class="actions">
        <button type="submit">Register appointment</button>
    </div>
</form:form>

<jsp:include page="../layout/footer.jsp"/>
