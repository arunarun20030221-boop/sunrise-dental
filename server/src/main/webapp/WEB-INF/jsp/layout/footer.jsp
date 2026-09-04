<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
</main>
<footer class="foot">
    <c:out value="${clinic.name}"/> &middot; <c:out value="${clinic.address}"/>
    &middot; <c:out value="${clinic.phone}"/>
</footer>
</body>
</html>
