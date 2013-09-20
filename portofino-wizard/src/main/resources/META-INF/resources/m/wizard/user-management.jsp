<%@ page contentType="text/html;charset=UTF-8" language="java"
         pageEncoding="UTF-8"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"
%><%@ taglib prefix="mde" uri="/manydesigns-elements"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ taglib tagdir="/WEB-INF/tags" prefix="portofino" %>
<stripes:layout-render name="/m/base/admin-theme/admin-page.jsp">
    <jsp:useBean id="actionBean" scope="request"
                 type="com.manydesigns.portofino.actions.admin.appwizard.ApplicationWizard"/>
    <stripes:layout-component name="customScripts">
        <link rel="stylesheet" type="text/css"
              href="<stripes:url value="/m/theme/portofino.css"/>"/>
    </stripes:layout-component>
    <stripes:layout-component name="pageTitle">
        <fmt:message key="appwizard.step3.title" />
    </stripes:layout-component>
    <stripes:layout-component name="portletHeader">
        <jsp:include page="wizard-content-header.jsp" />
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">
        <stripes:form beanclass="com.manydesigns.portofino.actions.admin.appwizard.ApplicationWizard"
                      method="post" class="form-horizontal">
            <p><fmt:message key="appwizard.userManagement.warning" /></p>
            <mde:write name="actionBean" property="userAndGroupTablesForm"/>
            <div style="display: none;">
                <mde:write name="actionBean" property="schemasForm"/>
                <input type="hidden" name="connectionProviderType" value="${actionBean.connectionProviderType}" />
                <mde:write name="actionBean" property="connectionProviderField" />
                <mde:write name="actionBean" property="jndiCPForm"/>
                <mde:write name="actionBean" property="jdbcCPForm"/>
            </div>
            <script type="text/javascript">
                $(function() {
                    var buttons = $(".form-actions button");
                    buttons.click(function() {
                        buttons.unbind("click");
                        buttons.click(function() {
                            alert("<fmt:message key='commons.waitOperation' />");
                            return false;
                        });
                    });
                });
            </script>
            <div class="form-actions" style="padding-left: 20px;">
                <portofino:buttons list="user-management" />
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>