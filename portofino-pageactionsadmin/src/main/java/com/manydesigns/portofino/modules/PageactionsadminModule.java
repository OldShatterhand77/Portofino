/*
 * Copyright (C) 2005-2013 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.manydesigns.portofino.modules;

import com.manydesigns.portofino.PortofinoProperties;
import com.manydesigns.portofino.actions.admin.page.RootChildrenAction;
import com.manydesigns.portofino.actions.admin.page.RootPermissionsAction;
import com.manydesigns.portofino.di.Inject;
import com.manydesigns.portofino.menu.MenuBuilder;
import com.manydesigns.portofino.menu.SimpleMenuAppender;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public class PageactionsadminModule implements Module {
    public static final String copyright =
            "Copyright (c) 2005-2013, ManyDesigns srl";

    //**************************************************************************
    // Fields
    //**************************************************************************

    @Inject(BaseModule.PORTOFINO_CONFIGURATION)
    public Configuration configuration;

    @Inject(BaseModule.ADMIN_MENU)
    public MenuBuilder adminMenu;

    protected ModuleStatus status = ModuleStatus.CREATED;

    //**************************************************************************
    // Logging
    //**************************************************************************

    public static final Logger logger =
            LoggerFactory.getLogger(PageactionsadminModule.class);

    @Override
    public String getModuleVersion() {
        return configuration.getString(PortofinoProperties.PORTOFINO_VERSION);
    }

    @Override
    public int getMigrationVersion() {
        return 1;
    }

    @Override
    public double getPriority() {
        return 1;
    }

    @Override
    public String getId() {
        return "pageactionsadmin";
    }

    @Override
    public String getName() {
        return "Pageactions Administration";
    }

    @Override
    public int install() {
        return 1;
    }

    @Override
    public void init() {
        appendToAdminMenu();
        status = ModuleStatus.ACTIVE;
    }

    protected void appendToAdminMenu() {
        SimpleMenuAppender group;
        SimpleMenuAppender link;

        group = SimpleMenuAppender.group("security", null, "Security", 2.0);
        adminMenu.menuAppenders.add(group);

        link = SimpleMenuAppender.link(
                "security", "rootPermissions", null, "Root permissions", RootPermissionsAction.URL_BINDING, 1.0);
        adminMenu.menuAppenders.add(link);

        link = SimpleMenuAppender.link(
                "configuration", "topLevelPages", null, "Top-level pages", RootChildrenAction.URL_BINDING, 3.0);
        adminMenu.menuAppenders.add(link);
    }

    @Override
    public void destroy() {
        status = ModuleStatus.DESTROYED;
    }

    @Override
    public ModuleStatus getStatus() {
        return status;
    }
}
