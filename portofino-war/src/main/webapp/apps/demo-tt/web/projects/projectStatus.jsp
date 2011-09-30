<%@ page import="com.manydesigns.portofino.application.Application" %>
<%@ page import="org.apache.commons.collections.MultiHashMap" %>
<%@ page import="org.apache.commons.collections.MultiMap" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="ognl.Ognl" %>
<%@ page contentType="text/html;charset=ISO-8859-1" language="java"
         pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@taglib prefix="mde" uri="/manydesigns-elements"%>
<jsp:useBean id="actionBean" scope="request" type="com.manydesigns.portofino.actions.JspAction"/>
<stripes:layout-render name="/skins/${skin}/portlet.jsp">
    <stripes:layout-component name="portletTitle">
        <c:out value="${actionBean.jspPage.title}"/>
    </stripes:layout-component>
    <stripes:layout-component name="portletHeaderButtons">
        <button name="configure" class="wrench">Configure</button>
    </stripes:layout-component>
    <stripes:layout-component name="portletBody">

            <%
                Application appl = (Application) request.getAttribute("application");
                List<?> objects = appl.runSql("redmine", "select count(*), project_id, status.name, projects.name from issues join issue_statuses status on status_id = status.id join projects on project_id = projects.id group by status_id, project_id order by project_id");%>
            <table id="projectTree">
            <tr width="100%" style="background-color: #ECEEF0">
                <th width="40%">Project</th>
                <th width="40%">Status</th>
                <th width="20%">Issues</th>
            </tr>

            <%
                String lastProject = "none";
                int lastFather = 0;
                int id = 1;
                boolean odd = true;
                String color = "#ECE9D8";
                for(Object obj : objects) {
                    Object[] obArr = (Object[]) obj;
                    String count = ((Long) obArr[0]).toString();
                    Integer projId = (Integer) obArr[1];
                    String statusName = (String) obArr[2];
                    String projName = (String) obArr[3];
                    if(!projName.equals(lastProject)){
                        out.print("<tr id=\"node-" + id + "\"><td colspan=3>"+projName+"</td></tr>");
                        lastFather = id;
                        id++;
                        lastProject=projName;
                    }

                    if (odd) {
                        color = "#ECE9D8";
                    } else {
                        color = "#FFFDF0";
                    }
                    odd = !odd;
                    out.print("<tr id=\"node-" + id + "\" class=\"child-of-node-" + lastFather + "\" style=\"background-color: "+color+"\">");
                    out.print("<td ></td><td>"+statusName+"</td><td>"+count+"</td>");
                    id++;
                    out.print("</tr>");
                }
            %>
            </table>
            <script src="<%= request.getContextPath() %>/jquery-treetable-2.3.0/jquery.treeTable.min.js" >
            </script>

            <script>
                $("#projectTree").treeTable({"clickableNodeNames": true, "expandable":true, "treeColumn":0, "indent":20 });
            </script>

    </stripes:layout-component>
    <stripes:layout-component name="portletFooter">
    </stripes:layout-component>
</stripes:layout-render>