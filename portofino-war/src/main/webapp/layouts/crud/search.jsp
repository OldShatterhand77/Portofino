<%@ page contentType="text/html;charset=ISO-8859-1" language="java"
         pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@taglib prefix="mde" uri="/manydesigns-elements"%>
<stripes:layout-render name="/skins/${skin}/portlet.jsp">
    <jsp:useBean id="actionBean" scope="request" type="com.manydesigns.portofino.actions.CrudAction"/>
    <stripes:layout-component name="portletTitle">
        <c:out value="${actionBean.crud.searchTitle}"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletHeaderButtons">
        <button name="configure" class="wrench">Configure</button>
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">
        <c:if test="${not empty actionBean.searchForm}">
            <div class="yui-gc">
                <div class="yui-u first">
                    <div class="search_results withSearchForm">
                        <mde:write name="actionBean" property="tableForm"/>
                        <stripes:submit name="create" value="Create new" class="portletButton"/>
                        <stripes:submit name="bulkEdit" value="Edit" class="portletButton"/>
                        <stripes:submit name="bulkDelete" value="Delete"  class="portletButton" onclick="return confirm ('Are you sure?');"/>
                        <stripes:submit name="print" value="Print" disabled="true" class="portletButton"/>
                        <stripes:submit name="exportSearchExcel" value="Excel" class="portletButton" disabled="true"/>
                        <stripes:submit name="exportSearchPdf" value="Pdf" class="portletButton" disabled="true"/>
                    </div>
                    <!-- TODO custom buttons -->
                </div>
                <div class="yui-u">
                        <div class="search_form">
                            <mde:write name="actionBean" property="searchForm"/>
                            <div class="searchFormButtons">
                                <stripes:submit name="search" value="Search" class="portletButton"/>
                                <stripes:submit name="resetSearch" value="Reset form" class="portletButton"/>
                            </div>
                        </div>
                </div>
            </div>
        </c:if><c:if test="${empty actionBean.searchForm}">
            <div class="search_results">
                <mde:write name="actionBean" property="tableForm"/>
                <stripes:submit name="create" value="Create new" class="portletButton"/>
                <stripes:submit name="bulkEdit" value="Edit" class="portletButton"/>
                <stripes:submit name="bulkDelete" value="Delete"  class="portletButton" onclick="return confirm ('Are you sure?');"/>
                <stripes:submit name="print" value="Print" disabled="true" class="portletButton"/>
                <stripes:submit name="exportSearchExcel" value="Excel" class="portletButton" disabled="true"/>
                <stripes:submit name="exportSearchPdf" value="Pdf" class="portletButton" disabled="true"/>
            </div>
            <!-- TODO custom buttons -->
        </c:if>

        <input type="hidden" name="cancelReturnUrl" value="<c:out value="${actionBean.cancelReturnUrl}"/>"/>
    </stripes:layout-component>
</stripes:layout-render>