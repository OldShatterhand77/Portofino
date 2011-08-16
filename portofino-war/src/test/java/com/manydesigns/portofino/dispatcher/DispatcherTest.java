/*
 * Copyright (C) 2005-2011 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * There are special exceptions to the terms and conditions of the GPL
 * as it is applied to this software. View the full text of the
 * exception in file OPEN-SOURCE-LICENSE.txt in the directory of this
 * software distribution.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307  USA
 *
 */

package com.manydesigns.portofino.dispatcher;

import com.manydesigns.portofino.AbstractPortofinoTest;
import com.manydesigns.portofino.model.pages.CrudPage;
import com.manydesigns.portofino.model.pages.Page;
import com.manydesigns.portofino.model.pages.RootPage;
import com.manydesigns.portofino.navigation.Navigation;

import java.util.Collections;
import java.util.List;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla - alessio.stalla@manydesigns.com
*/
public class DispatcherTest extends AbstractPortofinoTest {

    public static final String CRUD_ACTION = "/crud.action";
    public static final String CHART_ACTION = "/chart.action";

    Dispatcher dispatcher;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dispatcher = new Dispatcher(application);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        application.closeSessions();
    }

    public void testProjectSearch() {
        String originalPath = "/projects";
        req.setServletPath(originalPath);
        Dispatch dispatch = dispatcher.createDispatch(req);
        assertNotNull(dispatch);

        assertEquals(originalPath, dispatch.getOriginalPath());
        assertEquals(CRUD_ACTION, dispatch.getRewrittenPath());

        PageInstance[] pageInstancePath =
                dispatch.getPageInstancePath();
        assertEquals(1, pageInstancePath.length);

        CrudPageInstance pageInstance = (CrudPageInstance) pageInstancePath[0];
        Page page = model.getRootPage().getChildPages().get(0);
        assertEquals(page, pageInstance.getPage());
        assertEquals(CrudPage.MODE_SEARCH, pageInstance.getMode());
        assertNull(pageInstance.getPk());

        Navigation navigation =
                new Navigation(application, dispatch, Collections.EMPTY_LIST);
        /*
        List<NavigationNode> rootPages = navigation.getRootNodes();

        // Navigation node per /projects
        assertEquals(1, rootPages.size());
        NavigationNode navigationNode = rootPages.get(0);
        assertEquals("/projects", navigationNode.getUrl());
        assertEquals(page, navigationNode.getPage());
        assertTrue(navigationNode.isEnabled());

        // Navigation node per /projects/tickets
        assertEquals(1, navigationNode.getChildNodes().size());
        navigationNode = navigationNode.getChildNodes().get(0);
        assertEquals("/projects/tickets", navigationNode.getUrl());
        page = page.getChildNodes().get(0);
        assertEquals(page, navigationNode.getPage());
        assertFalse(navigationNode.isEnabled());
*/
        String htmlOutput = elementToString(navigation);
        assertEquals("<ul><li class=\"selected\">" +
                "<a href=\"/projects\" title=\"projects\">projects</a></li>" +
                "</ul><hr /><ul><li><a href=\"/projects/report\" " +
                "title=\"report description\">report title</a></li></ul>",
                htmlOutput);
    }

    public void testProjectNew() {
        String originalPath = "/projects/new";
        req.setServletPath(originalPath);
        Dispatch dispatch = dispatcher.createDispatch(req);
        assertNotNull(dispatch);

        assertEquals(originalPath, dispatch.getOriginalPath());
        assertEquals(CRUD_ACTION, dispatch.getRewrittenPath());

        PageInstance[] pageInstancePath =
                dispatch.getPageInstancePath();
        assertEquals(1, pageInstancePath.length);

        CrudPageInstance pageInstance = (CrudPageInstance) pageInstancePath[0];
        Page page = model.getRootPage().getChildPages().get(0);
        assertEquals(page, pageInstance.getPage());
        assertEquals(CrudPage.MODE_NEW, pageInstance.getMode());
        assertNull(pageInstance.getPk());
        
        Navigation navigation =
                new Navigation(application, dispatch, Collections.EMPTY_LIST);
        /*
        List<NavigationNode> rootPages = navigation.getRootNodes();

        // Navigation node per /projects
        assertEquals(1, rootPages.size());
        NavigationNode navigationNode = rootPages.get(0);
        assertEquals("/projects", navigationNode.getUrl());
        assertEquals(page, navigationNode.getPage());
        assertTrue(navigationNode.isEnabled());

        // Navigation node per /projects/tickets
        assertEquals(1, navigationNode.getChildNodes().size());
        navigationNode = navigationNode.getChildNodes().get(0);
        assertEquals("/projects/tickets", navigationNode.getUrl());
        page = page.getChildNodes().get(0);
        assertEquals(page, navigationNode.getPage());
        assertFalse(navigationNode.isEnabled());
        */
    }

    public void testProjectReport() {
        String originalPath = "/projects/report";
        req.setServletPath(originalPath);
        Dispatch dispatch = dispatcher.createDispatch(req);
        assertNotNull(dispatch);

        assertEquals(originalPath, dispatch.getOriginalPath());
        assertEquals(CHART_ACTION, dispatch.getRewrittenPath());

        PageInstance[] pageInstancePath =
                dispatch.getPageInstancePath();
        assertEquals(2, pageInstancePath.length);

        CrudPageInstance pageInstance = (CrudPageInstance) pageInstancePath[0];
        Page page = model.getRootPage().getChildPages().get(0);
        assertEquals(page, pageInstance.getPage());
        assertEquals(CrudPage.MODE_SEARCH, pageInstance.getMode());
        assertNull(pageInstance.getPk());

        PageInstance reportNodeInstance = pageInstancePath[1];
        Page reportNode = page.getChildPages().get(0);
        assertEquals("report", reportNode.getId());
        assertEquals(reportNode, reportNodeInstance.getPage());
        assertNull(reportNodeInstance.getMode());
        assertNull(pageInstance.getPk());

        /*
        List<NavigationNode> rootPages = navigation.getRootNodes();

        // Navigation node per /projects
        assertEquals(1, rootPages.size());
        NavigationNode navigationNode = rootPages.get(0);
        assertEquals("/projects", navigationNode.getUrl());
        assertEquals(page, navigationNode.getPage());
        assertTrue(navigationNode.isEnabled());

        // Navigation node per /projects/tickets
        assertEquals(1, navigationNode.getChildNodes().size());
        navigationNode = navigationNode.getChildNodes().get(0);
        assertEquals("/projects/tickets", navigationNode.getUrl());
        page = page.getChildNodes().get(0);
        assertEquals(page, navigationNode.getPage());
        assertFalse(navigationNode.isEnabled());
        */
    }

    public void testProjectDetail() {
        String originalPath = "/projects/10";
        req.setServletPath(originalPath);
        Dispatch dispatch = dispatcher.createDispatch(req);
        assertNotNull(dispatch);

        assertEquals(originalPath, dispatch.getOriginalPath());
        assertEquals(CRUD_ACTION, dispatch.getRewrittenPath());

        PageInstance[] pageInstancePath =
                dispatch.getPageInstancePath();
        assertEquals(1, pageInstancePath.length);

        // nodo /project
        List<PageInstance> tree = dispatch.getNavigationPageInstances();
        assertNotNull(tree);
        assertEquals(1, tree.size());

        CrudPageInstance pageInstance = (CrudPageInstance) tree.get(0);
        assertEquals(pageInstancePath[0], pageInstance);
        RootPage rootPage = model.getRootPage();
        CrudPage page = (CrudPage) rootPage.getChildPages().get(0);
        assertEquals(page, pageInstance.getPage());
        assertEquals(CrudPage.MODE_DETAIL, pageInstance.getMode());
        assertEquals("10", pageInstance.getPk());

        // nodo issues
        tree = pageInstance.getChildPageInstances();
        assertNotNull(tree);
        assertEquals(1, tree.size());

        pageInstance = (CrudPageInstance) tree.get(0);
        page = (CrudPage) page.getDetailChildPages().get(0);
        assertEquals(page, pageInstance.getPage());
        assertEquals(CrudPage.MODE_SEARCH, pageInstance.getMode());
        assertNull(pageInstance.getPk());

        Navigation navigation =
                new Navigation(application, dispatch, Collections.EMPTY_LIST);
        /*
        List<NavigationNode> rootPages = navigation.getRootNodes();

        // Navigation node per /projects
        assertEquals(1, rootPages.size());
        NavigationNode navigationNode = rootPages.get(0);
        assertEquals("/projects", navigationNode.getUrl());
        assertEquals(page, navigationNode.getPage());
        assertTrue(navigationNode.isEnabled());
        */

        String htmlOutput = elementToString(navigation);
        assertEquals("<ul><li class=\"selected\">" +
                "<a href=\"/projects\" title=\"projects\">projects</a></li></ul>" +
                "<hr /><ul><li><a href=\"/projects/10/tickets\" title=\"tickets\">tickets</a></li></ul>",
                htmlOutput);

        /*
        // Navigation node per /projects/tickets
        assertEquals(1, navigationNode.getChildNodes().size());
        navigationNode = navigationNode.getChildNodes().get(0);
        assertEquals("/projects/10/tickets", navigationNode.getUrl());
        page = page.getChildNodes().get(0);
        assertEquals(page, navigationNode.getPage());
        assertTrue(navigationNode.isEnabled());
        */
    }


    public void testTicketSearch() {
        String originalPath = "/projects/10/tickets";
        req.setServletPath(originalPath);
        Dispatch dispatch = dispatcher.createDispatch(req);
        assertNotNull(dispatch);

        assertEquals(originalPath, dispatch.getOriginalPath());
        assertEquals(CRUD_ACTION, dispatch.getRewrittenPath());

        PageInstance[] pageInstancePath =
                dispatch.getPageInstancePath();
        assertEquals(2, pageInstancePath.length);

        Navigation navigation =
                new Navigation(application, dispatch, Collections.EMPTY_LIST);
        /*
        List<NavigationNode> rootPages = navigation.getRootNodes();
        */

        // Page e NavigationNode per /projects
        CrudPageInstance pageInstance = (CrudPageInstance) pageInstancePath[0];
        Page page = model.getRootPage().getChildPages().get(0);
        assertEquals(page, pageInstance.getPage());
        assertEquals(CrudPage.MODE_DETAIL, pageInstance.getMode());
        assertEquals("10", pageInstance.getPk());

        /*
        assertEquals(1, rootPages.size());
        NavigationNode navigationNode = rootPages.get(0);
        assertEquals("/projects", navigationNode.getUrl());
        assertEquals(page, navigationNode.getPage());
        assertTrue(navigationNode.isEnabled());
        */

        pageInstance = (CrudPageInstance) pageInstancePath[1];
        page = ((CrudPage) page).getDetailChildPages().get(0);
        assertEquals(page, pageInstance.getPage());
        assertEquals(CrudPage.MODE_SEARCH, pageInstance.getMode());
        assertNull(pageInstance.getPk());

        /*
        // Page e NavigationNode per /projects/tickets
        assertEquals(1, navigationNode.getChildNodes().size());
        navigationNode = navigationNode.getChildNodes().get(0);
        assertEquals("/projects/10/tickets", navigationNode.getUrl());
        assertEquals(page, navigationNode.getPage());
        assertTrue(navigationNode.isEnabled());
*/

        String htmlOutput = elementToString(navigation);
        assertEquals("<ul><li class=\"path\">" +
                "<a href=\"/projects\" title=\"projects\">projects</a></li></ul>" +
                "<hr /><ul><li class=\"selected\"><a href=\"/projects/10/tickets\" title=\"tickets\">tickets</a></li></ul>",
                htmlOutput);
    }


    public void testTicketDetail() {
        String originalPath = "/projects/10/tickets/20";
        req.setServletPath(originalPath);
        Dispatch dispatch = dispatcher.createDispatch(req);
        assertNotNull(dispatch);

        assertEquals(originalPath, dispatch.getOriginalPath());
        assertEquals(CRUD_ACTION, dispatch.getRewrittenPath());

        PageInstance[] pageInstancePath =
                dispatch.getPageInstancePath();
        assertEquals(2, pageInstancePath.length);

        CrudPageInstance pageInstance = (CrudPageInstance) pageInstancePath[0];
        CrudPage expected = (CrudPage) model.getRootPage().getChildPages().get(0);
        assertEquals(expected, pageInstance.getPage());
        assertEquals(CrudPage.MODE_DETAIL, pageInstance.getMode());
        assertEquals("10", pageInstance.getPk());

        pageInstance = (CrudPageInstance) pageInstancePath[1];
        expected = (CrudPage) expected.getDetailChildPages().get(0);
        assertEquals(expected, pageInstance.getPage());
        assertEquals(CrudPage.MODE_DETAIL, pageInstance.getMode());
        assertEquals("20", pageInstance.getPk());
    }

    public void testIllegal1() {
        String originalPath = "/projects/tickets/bla";
        req.setServletPath(originalPath);
        Dispatch dispatch = dispatcher.createDispatch(req);
        assertNull(dispatch);
    }

    public void testIllegal2() {
        String originalPath = "/projects/new/tickets/bla";
        req.setServletPath(originalPath);
        Dispatch dispatch = dispatcher.createDispatch(req);
        assertNull(dispatch);
    }
}