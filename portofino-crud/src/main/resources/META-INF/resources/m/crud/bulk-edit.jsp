<%@ page contentType="text/html;charset=UTF-8" language="java"
         pageEncoding="UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"
%><%@taglib prefix="mde" uri="/manydesigns-elements"
%><%@ taglib tagdir="/WEB-INF/tags" prefix="portofino"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><jsp:useBean id="actionBean" scope="request" type="com.manydesigns.portofino.pageactions.crud.AbstractCrudAction"
/><stripes:layout-render name="/m/theme${actionBean.pageTemplate}/modal.jsp">
    <stripes:layout-component name="portletTitle">
        <c:out value="${actionBean.editTitle}"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">
        <p><fmt:message key = "layouts.crud.bulk-edit.select_columns"/></p>
        <stripes:form action="${actionBean.dispatch.originalPath}" method="post"
                      class="form-horizontal">
            <mde:write name="actionBean" property="form"/>
            <stripes:hidden name="selection"/>
            <c:if test="${not empty actionBean.searchString}">
                <input type="hidden" name="searchString" value="<c:out value="${actionBean.searchString}"/>"/>
            </c:if>
            <input type="hidden" name="cancelReturnUrl" value="<c:out value="${actionBean.cancelReturnUrl}"/>"/>
            <div class="form-actions">
                <portofino:buttons list="crud-bulk-edit" />
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>