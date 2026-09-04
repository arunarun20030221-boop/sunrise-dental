<%--
  Requirement 5 - step-by-step instructions for new staff.
--%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" scope="request" value="Help - Sunrise Dental Clinic"/>
<jsp:include page="layout/header.jsp"/>

<div class="page-head">
    <h1>How to use the clinic system</h1>
</div>

<section class="card help">
    <h2>1. Signing in and out</h2>
    <ol>
        <li>Enter the username and password given to you by the clinic administrator.</li>
        <li>Your name and role appear at the top right of every screen once you are signed in.</li>
        <li>Click <strong>Exit</strong> at the top right when you leave the desk. The system also
            signs you out automatically after 30 minutes of inactivity, because reception is a
            shared computer.</li>
    </ol>

    <h2>2. Registering a new appointment</h2>
    <ol>
        <li>Click <strong>New appointment</strong>.</li>
        <li>Fill in the patient's name, contact number and address. If the patient has visited
            before, type the same contact number and the system will reuse their record rather
            than create a duplicate.</li>
        <li>Choose the dentist, the treatment, and the date and time.</li>
        <li>Set <strong>Sessions</strong> above 1 only for a treatment spread over several
            visits, such as a root canal. The bill charges each session.</li>
        <li>Click <strong>Register appointment</strong>. The system allocates the appointment
            number automatically &mdash; write it on the patient's card.</li>
    </ol>

    <h2>3. If the system refuses a booking</h2>
    <p>Two rules can block a booking, and both messages tell you what to do next:</p>
    <ul>
        <li><strong>The dentist is already booked</strong> &mdash; the message names the clashing
            appointment and the time it ends, so you can offer the patient the next free slot.</li>
        <li><strong>Outside opening hours</strong> &mdash; the clinic is open
            ${clinic.openingTime} to ${clinic.closingTime}. A treatment must also <em>finish</em>
            before closing, so a long treatment cannot start late in the afternoon.</li>
    </ul>

    <h2>4. Finding an appointment</h2>
    <ol>
        <li>Use the <strong>Find</strong> box on the diary page.</li>
        <li>Type the full appointment number (for example APT-2026-000001) to go straight to it.</li>
        <li>Or type part of the patient's name to list all of their appointments.</li>
        <li>Use the <strong>Date</strong> box to see every appointment on a particular day.</li>
    </ol>

    <h2>5. Taking payment and printing the receipt</h2>
    <ol>
        <li>Open the appointment and check the <strong>Bill</strong> panel, which shows the
            consultation fee, the treatment cost and any additions such as anaesthesia.</li>
        <li>Click <strong>Issue bill</strong> once the patient is ready to pay.</li>
        <li>Click <strong>Print receipt</strong> on the receipt page.</li>
        <li>A bill can only be issued once. To reprint, open the appointment again and choose
            <strong>View / print receipt</strong>.</li>
    </ol>

    <h2>6. Recording what happened</h2>
    <ul>
        <li><strong>Mark attended</strong> once the patient has been seen.</li>
        <li><strong>Record no-show</strong> if they did not arrive. This feeds the reports, so
            please do it on the day.</li>
        <li><strong>Cancel appointment</strong> frees the slot for someone else.</li>
    </ul>

    <c:if test="${currentUser.admin}">
        <h2>7. Reports (administrators)</h2>
        <ul>
            <li><strong>Revenue by treatment</strong> &mdash; which treatments bring in the money
                over a chosen period.</li>
            <li><strong>Dentist workload</strong> &mdash; chair time booked per dentist, with
                cancellations and no-shows.</li>
        </ul>
    </c:if>

    <h2>Who to ask</h2>
    <p>For a forgotten password or a new staff account, contact the clinic administrator.
        For anything the system will not do, call <c:out value="${clinic.phone}"/>.</p>
</section>

<jsp:include page="layout/footer.jsp"/>
