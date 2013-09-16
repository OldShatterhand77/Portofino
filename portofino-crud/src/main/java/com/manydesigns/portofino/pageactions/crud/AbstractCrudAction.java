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

package com.manydesigns.portofino.pageactions.crud;

import com.manydesigns.elements.ElementsThreadLocals;
import com.manydesigns.elements.FormElement;
import com.manydesigns.elements.Mode;
import com.manydesigns.elements.annotations.*;
import com.manydesigns.elements.blobs.Blob;
import com.manydesigns.elements.blobs.BlobManager;
import com.manydesigns.elements.fields.*;
import com.manydesigns.elements.forms.FieldSet;
import com.manydesigns.elements.forms.*;
import com.manydesigns.elements.messages.SessionMessages;
import com.manydesigns.elements.options.DefaultSelectionProvider;
import com.manydesigns.elements.options.DisplayMode;
import com.manydesigns.elements.options.SearchDisplayMode;
import com.manydesigns.elements.options.SelectionProvider;
import com.manydesigns.elements.reflection.ClassAccessor;
import com.manydesigns.elements.reflection.PropertyAccessor;
import com.manydesigns.elements.servlet.MutableHttpServletRequest;
import com.manydesigns.elements.servlet.ServletUtils;
import com.manydesigns.elements.text.OgnlTextFormat;
import com.manydesigns.elements.util.MimeTypes;
import com.manydesigns.elements.xml.XhtmlBuffer;
import com.manydesigns.elements.xml.XmlBuffer;
import com.manydesigns.portofino.PortofinoProperties;
import com.manydesigns.portofino.buttons.GuardType;
import com.manydesigns.portofino.buttons.annotations.Button;
import com.manydesigns.portofino.buttons.annotations.Buttons;
import com.manydesigns.portofino.buttons.annotations.Guard;
import com.manydesigns.portofino.dispatcher.PageInstance;
import com.manydesigns.portofino.files.TempFile;
import com.manydesigns.portofino.files.TempFileService;
import com.manydesigns.portofino.pageactions.AbstractPageAction;
import com.manydesigns.portofino.pageactions.crud.configuration.CrudConfiguration;
import com.manydesigns.portofino.pageactions.crud.configuration.CrudProperty;
import com.manydesigns.portofino.pageactions.crud.reflection.CrudAccessor;
import com.manydesigns.portofino.security.AccessLevel;
import com.manydesigns.portofino.security.RequiresPermissions;
import com.manydesigns.portofino.servlets.BlobCleanupListener;
import com.manydesigns.portofino.util.PkHelper;
import com.manydesigns.portofino.util.ShortNameUtils;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.DateFormat;
import jxl.write.*;
import jxl.write.Number;
import jxl.write.biff.RowsExceededException;
import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.util.UrlBuilder;
import ognl.OgnlContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.json.JSONException;
import org.json.JSONStringer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>A generic PageAction offering CRUD functionality, independently on the underlying data source.</p>
 * <p>Out of the box, instances of this class are capable of the following:
 *   <ul>
 *      <li>Presenting search, create, read, delete, update operations (the last two also in bulk mode) to the user,
 *          while delegating the actual implementation (e.g. accessing a database table, calling a web service,
 *          querying a JSON data source, etc.) to concrete subclasses;
 *      </li>
 *      <li>Performing exports to various formats (Pdf, Excel) of the Read view and the Search view,
 *          with the possibility for subclasses to customize the exports;</li>
 *      <li>Managing selection providers to constrain certain properties to values taken from a list, and aid
 *          the user in inserting those values (e.g. picking colours from a combo box, or cities with an
 *          autocompleted input field); the actual handling of selection providers is delegated to a
 *          companion object of type {@link SelectionProviderSupport} which must be provided by the concrete
 *          subclasses;</li>
 *      <li>Handling permissions so that only enabled users may create, edit or delete objects;</li>
 *      <li>Offering hooks for subclasses to easily customize certain key functions (e.g. execute custom code
 *          before or after saving an object).</li>
 *   </ul>
 * </p>
 * <p>This PageAction can handle a varying number of URL path parameters. Each parameter is assumed to be part
 * of an object identifier - for example, a database primary key (single or multi-valued). When no parameter is
 * specified, the page is in search mode. When the correct number of parameters is provided, the action attempts
 * to load an object with the appropriate identifier (for example, by loading a row from a database table with
 * the corresponding primary key). As any other page, crud pages can have children, and they always prevail over
 * the object key: a crud page with a child named &quot;child&quot; will never attempt to load an object with key
 * &quot;child&quot;.</p>
 * <!-- TODO popup mode -->
 *
 * @param <T> the types of objects that this crud can handle.
 *
 * @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
 * @author Angelo Lupo          - angelo.lupo@manydesigns.com
 * @author Giampiero Granatella - giampiero.granatella@manydesigns.com
 * @author Alessio Stalla       - alessio.stalla@manydesigns.com
 */
public abstract class AbstractCrudAction<T> extends AbstractPageAction {
    public static final String copyright =
            "Copyright (c) 2005-2013, ManyDesigns srl";

    public final static String SEARCH_STRING_PARAM = "searchString";
    public final static String prefix = "";
    public final static String searchPrefix = prefix + "search_";

    //**************************************************************************
    // Permissions
    //**************************************************************************

    /**
     * Constants for the permissions supported by instances of this class. Subclasses are recommended to
     * support at least the permissions defined here.
     */
    public static final String
            PERMISSION_CREATE = "crud-create",
            PERMISSION_EDIT = "crud-edit",
            PERMISSION_DELETE = "crud-delete";

    public static final Logger logger =
            LoggerFactory.getLogger(AbstractCrudAction.class);

    //--------------------------------------------------------------------------
    // Export
    //--------------------------------------------------------------------------

    protected static final String TEMPLATE_FOP_SEARCH = "templateFOP-Search.xsl";
    protected static final String TEMPLATE_FOP_READ = "templateFOP-Read.xsl";

    //--------------------------------------------------------------------------
    // Web parameters
    //--------------------------------------------------------------------------

    public String[] pk;
    public String propertyName;
    public String[] selection;
    public String searchString;
    public String successReturnUrl;
    public Integer firstResult;
    public Integer maxResults;
    public String sortProperty;
    public String sortDirection;
    public boolean searchVisible;

    //--------------------------------------------------------------------------
    // Popup
    //--------------------------------------------------------------------------

    protected String popupCloseCallback;

    //--------------------------------------------------------------------------
    // UI forms
    //--------------------------------------------------------------------------

    public SearchForm searchForm;
    public TableForm tableForm;
    public Form form;

    //--------------------------------------------------------------------------
    // Selection providers
    //--------------------------------------------------------------------------

    protected SelectionProviderSupport selectionProviderSupport;
    
    protected String relName;
    protected int selectionProviderIndex;
    protected String selectFieldMode;
    protected String labelSearch;

    //--------------------------------------------------------------------------
    // Data objects
    //--------------------------------------------------------------------------

    public ClassAccessor classAccessor;
    public PkHelper pkHelper;

    public T object;
    public List<? extends T> objects;

    //--------------------------------------------------------------------------
    // Configuration
    //--------------------------------------------------------------------------

    public CrudConfiguration crudConfiguration;
    public Form crudConfigurationForm;
    public TableForm propertiesTableForm;
    public CrudPropertyEdit[] propertyEdits;
    public TableForm selectionProvidersForm;
    public CrudSelectionProviderEdit[] selectionProviderEdits;

    //--------------------------------------------------------------------------
    // Navigation
    //--------------------------------------------------------------------------

    public String returnToParentTarget;
    public final Map<String, String> returnToParentParams = new HashMap<String, String>();
    protected ResultSetNavigation resultSetNavigation;

    //--------------------------------------------------------------------------
    // Crud operations
    //--------------------------------------------------------------------------

    /**
     * Loads a list of objects filtered using the current search criteria and limited by the current
     * first and max results parameters. If the load is successful, the implementation must assign
     * the result to the <code>objects</code> field.
     */
    public abstract void loadObjects();

    /**
     * Loads an object by its identifier and returns it. The object must satisfy the current search criteria.
     * @param pkObject the object used as an identifier; the actual implementation is regulated by subclasses.
     * The only constraint is that it is serializable.
     * @return the loaded object, or null if it couldn't be found or it didn't satisfy the search criteria.
     */
    protected abstract T loadObjectByPrimaryKey(Serializable pkObject);

    /**
     * Saves a new object to the persistent storage. The actual implementation is left to subclasses.
     * @param object the object to save.
     * @throws RuntimeException if the object could not be saved.
     */
    protected abstract void doSave(T object);

    /**
     * Saves an existing object to the persistent storage. The actual implementation is left to subclasses.
     * @param object the object to update.
     * @throws RuntimeException if the object could not be saved.
     */
    protected abstract void doUpdate(T object);

    /**
     * Deletes an object from the persistent storage. The actual implementation is left to subclasses.
     * @param object the object to delete.
     * @throws RuntimeException if the object could not be deleted.
     */
    protected abstract void doDelete(T object);

    @DefaultHandler
    public Resolution execute() {
        if (object == null) {
            return doSearch();
        } else {
            return read();
        }
    }

    /**
     * @see #loadObjectByPrimaryKey(java.io.Serializable)
     * @param identifier the object identifier in String form
     */
    protected void loadObject(String... identifier) {
        Serializable pkObject = pkHelper.getPrimaryKey(identifier);
        object = loadObjectByPrimaryKey(pkObject);
    }

    //**************************************************************************
    // Search
    //**************************************************************************

    @Buttons({
        @Button(list = "crud-search-form", key = "commons.search", order = 1, type = Button.TYPE_PRIMARY),
        @Button(list = "portlet-default-button", key = "commons.search") //XXX non va bene, posso avere diversi default su form diversi
    })
    public Resolution search() {
        searchVisible = true;
        searchString = null;
        return doSearch();
    }

    protected Resolution doSearch() {
        if(!isConfigured()) {
            logger.debug("Crud not correctly configured");
            return forwardToPortletNotConfigured();
        }

        try {
            setupSearchForm();
            if(maxResults == null) {
                //Load only the first page if the crud is paginated
                maxResults = getCrudConfiguration().getRowsPerPage();
            }
            loadObjects();
            setupTableForm(Mode.VIEW);

            if(isEmbedded()) {
                return getEmbeddedSearchView();
            } else {
                cancelReturnUrl = new UrlBuilder(
                    context.getLocale(), getDispatch().getAbsoluteOriginalPath(), false)
                    .toString();
                cancelReturnUrl = appendSearchStringParamIfNecessary(cancelReturnUrl);
                setupReturnToParentTarget();
                return getSearchView();
            }
        } catch(Exception e) {
            logger.warn("Crud not correctly configured", e);
            return forwardToPortletNotConfigured();
        }
    }

    public Resolution getSearchResultsPage() {
        if(!isConfigured()) {
            logger.debug("Crud not correctly configured");
            return new ErrorResolution(500, "Crud not correctly configured");
        }

        try {
            setupSearchForm();
            if(maxResults == null) {
                //Load only the first page if the crud is paginated
                maxResults = getCrudConfiguration().getRowsPerPage();
            }
            loadObjects();
            setupTableForm(Mode.VIEW);

            context.getRequest().setAttribute("actionBean", this);
            return getSearchResultsPageView();
        } catch(Exception e) {
            logger.warn("Crud not correctly configured", e);
            return new ErrorResolution(500, "Crud not correctly configured");
        }
    }

    public Resolution jsonSearchData() throws JSONException {
        setupSearchForm();
        loadObjects();

        long totalRecords = getTotalSearchRecords();

        setupTableForm(Mode.VIEW);
        JSONStringer js = new JSONStringer();
        js.object()
                .key("recordsReturned")
                .value(objects.size())
                .key("totalRecords")
                .value(totalRecords)
                .key("startIndex")
                .value(firstResult == null ? 0 : firstResult)
                .key("Result")
                .array();
        for (TableForm.Row row : tableForm.getRows()) {
            js.object()
                    .key("__rowKey")
                    .value(row.getKey());
            fieldsToJson(js, row);
            js.endObject();
        }
        js.endArray();
        js.endObject();
        String jsonText = js.toString();
        return new StreamingResolution(MimeTypes.APPLICATION_JSON_UTF8, jsonText);
    }

    /**
     * Returns the number of objects matching the current search criteria, not considering set limits
     * (first and max results).
     * @return the number of objects.
     */
    public abstract long getTotalSearchRecords();

    @Button(list = "crud-search-form", key = "commons.resetSearch", order = 2)
    public Resolution resetSearch() {
        return new RedirectResolution(getDispatch().getOriginalPath()).addParameter("searchVisible", true);
    }

    //**************************************************************************
    // Read
    //**************************************************************************

    public Resolution read() {
        if(!crudConfiguration.isLargeResultSet()) {
            setupSearchForm(); // serve per la navigazione del result set
            loadObjects();
            setupPagination();
        }

        setupForm(Mode.VIEW);
        form.readFromObject(object);
        refreshBlobDownloadHref();

        cancelReturnUrl = new UrlBuilder(
                Locale.getDefault(), getDispatch().getAbsoluteOriginalPath(), false)
                .toString();

        cancelReturnUrl = appendSearchStringParamIfNecessary(cancelReturnUrl);

        setupReturnToParentTarget();

        if(isEmbedded()) {
            return getEmbeddedReadView();
        } else {
            return getReadView();
        }
    }

    public Resolution jsonReadData() throws JSONException {
        if(object == null) {
            throw new IllegalStateException("Object not loaded. Are you including the primary key in the URL?");
        }

        setupForm(Mode.VIEW);
        form.readFromObject(object);
        refreshBlobDownloadHref();
        JSONStringer js = new JSONStringer();
        js.object();
        List<Field> fields = new ArrayList<Field>();
        collectVisibleFields(form, fields);
        fieldsToJson(js, fields);
        js.endObject();
        String jsonText = js.toString();
        return new StreamingResolution(MimeTypes.APPLICATION_JSON_UTF8, jsonText);
    }

    //**************************************************************************
    // Form handling
    //**************************************************************************

    /**
     * Writes the contents of the create or edit form into the persistent object.
     * Assumes that the form has already been validated.
     * Also processes rich-text (HTML) fields by cleaning the submitted HTML according
     * to the {@link #getWhitelist() whitelist}.
     */
    protected void writeFormToObject() {
        form.writeToObject(object);
        for(TextField textField : getEditableRichTextFields()) {
            PropertyAccessor propertyAccessor = textField.getPropertyAccessor();
            String stringValue = textField.getStringValue();
            String cleanText;
            try {
                Whitelist whitelist = getWhitelist();
                cleanText = Jsoup.clean(stringValue, whitelist);
            } catch (Throwable t) {
                logger.error("Could not clean HTML, falling back to escaped text", t);
                cleanText = StringEscapeUtils.escapeHtml(stringValue);
            }
            propertyAccessor.set(object, cleanText);
        }
    }

    /**
     * Returns the JSoup whitelist used to clean user-provided HTML in rich-text fields.
     * @return the default implementation returns the "basic" whitelist ({@see Whitelist#basic()}).
     */
    protected Whitelist getWhitelist() {
        return Whitelist.basic();
    }

    //**************************************************************************
    // Create/Save
    //**************************************************************************

    @Button(list = "crud-search", key = "commons.create", order = 1, type = Button.TYPE_SUCCESS,
            icon = Button.ICON_PLUS + Button.ICON_WHITE, group = "crud")
    @RequiresPermissions(permissions = PERMISSION_CREATE)
    public Resolution create() {
        setupForm(Mode.CREATE);
        object = (T) classAccessor.newInstance();
        createSetup(object);
        form.readFromObject(object);

        return getCreateView();
    }

    @Button(list = "crud-create", key = "commons.save", order = 1, type = Button.TYPE_PRIMARY)
    @RequiresPermissions(permissions = PERMISSION_CREATE)
    public Resolution save() {
        setupForm(Mode.CREATE);
        object = (T) classAccessor.newInstance();
        createSetup(object);
        form.readFromObject(object);
        form.readFromRequest(context.getRequest());
        List<Blob> blobs = getBlobs();
        if (form.validate()) {
            writeFormToObject();
            if(createValidate(object)) {
                try {
                    doSave(object);
                    createPostProcess(object);
                    //Before commit, worst case: blobs leak. After commit, worst case: corrupt data (missing blobs).
                    for(Blob blob : blobs) {
                        BlobCleanupListener.forgetBlob(blob);
                    }
                    commitTransaction();
                } catch (Throwable e) {
                    String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
                    logger.warn(rootCauseMessage, e);
                    SessionMessages.addErrorMessage(rootCauseMessage);
                    return getCreateView();
                }
                if(isPopup()) {
                    popupCloseCallback += "(true)";
                    return new ForwardResolution("/m/crud/popup/close.jsp");
                } else {
                    pk = pkHelper.generatePkStringArray(object);
                    String url = getDispatch().getOriginalPath() + "/" + getPkForUrl(pk);
                    XhtmlBuffer buffer = new XhtmlBuffer();
                    buffer.write(ElementsThreadLocals.getText("commons.save.successful") + ". ");
                    String createUrl = getDispatch().getAbsoluteOriginalPath();
                    if(!createUrl.contains("?")) {
                        createUrl += "?";
                    } else {
                        createUrl += "&";
                    }
                    createUrl += "create=";
                    createUrl = appendSearchStringParamIfNecessary(createUrl);
                    buffer.writeAnchor(createUrl, ElementsThreadLocals.getText("commons.create.another"));
                    SessionMessages.addInfoMessage(buffer);
                    return new RedirectResolution(url);
                }
            }
        } else {
            for(Blob blob : blobs) {
                BlobCleanupListener.recordBlob(blob);
            }
        }

        return getCreateView();
    }

    //**************************************************************************
    // Edit/Update
    //**************************************************************************

    @Button(list = "crud-read", key = "commons.edit", order = 1, icon = Button.ICON_EDIT + Button.ICON_WHITE,
            group = "crud", type = Button.TYPE_SUCCESS)
    @RequiresPermissions(permissions = PERMISSION_EDIT)
    public Resolution edit() {
        setupForm(Mode.EDIT);
        editSetup(object);
        form.readFromObject(object);
        return getEditView();
    }

    @Button(list = "crud-edit", key = "commons.update", order = 1, type = Button.TYPE_PRIMARY)
    @RequiresPermissions(permissions = PERMISSION_EDIT)
    public Resolution update() {
        setupForm(Mode.EDIT);
        editSetup(object);
        form.readFromObject(object);
        List<Blob> blobsBefore = getBlobs();
        form.readFromRequest(context.getRequest());
        List<Blob> blobsAfter = getBlobs();
        if (form.validate()) {
            writeFormToObject();
            if(editValidate(object)) {
                try {
                    doUpdate(object);
                    editPostProcess(object);
                    //Before commit, worst case: blobs leak. After commit, worst case: corrupt data (missing blobs).
                    for(Blob blob : blobsAfter) {
                        BlobCleanupListener.forgetBlob(blob);
                    }
                    commitTransaction();
                    //Physically delete overwritten or deleted blobs
                    //(after commit to avoid losing data in case of exception)
                    for(int i = 0; i < blobsBefore.size(); i++) {
                        Blob before = blobsBefore.get(i);
                        Blob after = blobsAfter.get(i);
                        if(before != null && !before.equals(after)) {
                            ElementsThreadLocals.getBlobManager().deleteBlob(before.getCode());
                        }
                    }
                } catch (Throwable e) {
                    String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
                    logger.warn(rootCauseMessage, e);
                    SessionMessages.addErrorMessage(rootCauseMessage);
                    return getEditView();
                }
                SessionMessages.addInfoMessage(ElementsThreadLocals.getText("commons.update.successful"));
                return new RedirectResolution(
                        appendSearchStringParamIfNecessary(getDispatch().getOriginalPath()));
            }
        } else {
            //Mark new blobs as to be deleted (GC)
            for(int i = 0; i < blobsBefore.size(); i++) {
                Blob before = blobsBefore.get(i);
                Blob after = blobsAfter.get(i);
                if(before != null && !before.equals(after)) {
                    BlobCleanupListener.recordBlob(after);
                }
            }
        }
        return getEditView();
    }

    //**************************************************************************
    // Bulk Edit/Update
    //**************************************************************************

    public boolean isBulkOperationsEnabled() {
        return (objects != null && !objects.isEmpty()) ||
                "bulkEdit".equals(context.getEventName()) ||
                "bulkDelete".equals(context.getEventName());
    }

    @Button(list = "crud-search", key = "commons.edit", order = 2, icon = Button.ICON_EDIT, group = "crud")
    @Guard(test = "isBulkOperationsEnabled()", type = GuardType.VISIBLE)
    @RequiresPermissions(permissions = PERMISSION_EDIT)
    public Resolution bulkEdit() {
        if (selection == null || selection.length == 0) {
            SessionMessages.addWarningMessage(ElementsThreadLocals.getText("commons.bulkUpdate.nothingSelected"));
            return new RedirectResolution(cancelReturnUrl, false);
        }

        if (selection.length == 1) {
            pk = selection[0].split("/");
            String url = getDispatch().getOriginalPath() + "/" + getPkForUrl(pk);
            url = appendSearchStringParamIfNecessary(url);
            return new RedirectResolution(url)
                    .addParameter("cancelReturnUrl", cancelReturnUrl)
                    .addParameter("edit");
        }

        setupForm(Mode.BULK_EDIT);

        return getBulkEditView();
    }

    @Button(list = "crud-bulk-edit", key = "commons.update", order = 1, type = Button.TYPE_PRIMARY)
    @RequiresPermissions(permissions = PERMISSION_EDIT)
    public Resolution bulkUpdate() {
        int updated = 0;
        setupForm(Mode.BULK_EDIT);
        form.readFromRequest(context.getRequest());
        if (form.validate()) {
            for (String current : selection) {
                loadObject(current.split("/"));
                editSetup(object);
                writeFormToObject();
                if(editValidate(object)) {
                    doUpdate(object);
                    editPostProcess(object);
                    updated++;
                }
            }
            try {
                commitTransaction();
            } catch (Throwable e) {
                String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
                logger.warn(rootCauseMessage, e);
                SessionMessages.addErrorMessage(rootCauseMessage);
                return getBulkEditView();
            }
            SessionMessages.addInfoMessage(
                    ElementsThreadLocals.getText("commons.bulkUpdate.successful", updated));
            return new RedirectResolution(
                    appendSearchStringParamIfNecessary(getDispatch().getOriginalPath()));
        } else {
            return getBulkEditView();
        }
    }

    //**************************************************************************
    // Delete
    //**************************************************************************

    @Button(list = "crud-read", key = "commons.delete", order = 2, icon = Button.ICON_TRASH, group = "crud")
    @RequiresPermissions(permissions = PERMISSION_DELETE)
    public Resolution delete() {
        String url = calculateBaseSearchUrl();
        if(deleteValidate(object)) {
            doDelete(object);
            try {
                deletePostProcess(object);
                commitTransaction();
                deleteFileBlobs(object);
                SessionMessages.addInfoMessage(ElementsThreadLocals.getText("commons.delete.successful"));

                // invalidate the pk on this crud
                pk = null;
            } catch (Exception e) {
                String rootCauseMessage = ExceptionUtils.getRootCauseMessage(e);
                logger.debug(rootCauseMessage, e);
                SessionMessages.addErrorMessage(rootCauseMessage);
            }
        }
        return new RedirectResolution(appendSearchStringParamIfNecessary(url), false);
    }

    @Button(list = "crud-search", key = "commons.delete", order = 3, icon = Button.ICON_TRASH, group = "crud")
    @Guard(test = "isBulkOperationsEnabled()", type = GuardType.VISIBLE)
    @RequiresPermissions(permissions = PERMISSION_DELETE)
    public Resolution bulkDelete() {
        int deleted = 0;
        if (selection == null) {
            SessionMessages.addWarningMessage(ElementsThreadLocals.getText("commons.bulkDelete.nothingSelected"));
            return new RedirectResolution(appendSearchStringParamIfNecessary(getDispatch().getOriginalPath()));
        }
        List<T> objects = new ArrayList<T>(selection.length);
        for (String current : selection) {
            String[] pkArr = current.split("/");
            Serializable pkObject = pkHelper.getPrimaryKey(pkArr);
            T obj = loadObjectByPrimaryKey(pkObject);
            if(deleteValidate(obj)) {
                doDelete(obj);
                deletePostProcess(obj);
                objects.add(obj);
                deleted++;
            }
        }
        try {
            commitTransaction();
            for(T obj : objects) {
                deleteFileBlobs(obj);
            }
            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("commons.bulkDelete.successful", deleted));
        } catch (Exception e) {
            logger.warn(ExceptionUtils.getRootCauseMessage(e), e);
            SessionMessages.addErrorMessage(ExceptionUtils.getRootCauseMessage(e));
        }

        return new RedirectResolution(appendSearchStringParamIfNecessary(getDispatch().getOriginalPath()));
    }

    //**************************************************************************
    // Hooks/scripting
    //**************************************************************************

    /**
     * Hook method called just after a new object has been created.
     * @param object the new object.
     */
    protected void createSetup(T object) {}

    /**
     * Hook method called after values from the create form have been propagated to the new object.
     * @param object the new object.
     * @return true if the object is to be considered valid, false otherwise. In the latter case, the
     * object will not be saved; it is suggested that the cause of the validation failure be displayed
     * to the user (e.g. by using SessionMessages).
     */
    protected boolean createValidate(T object) {
        return true;
    }

    /**
     * Hook method called just before a new object is actually saved to persistent storage.
     * @param object the new object.
     */
    protected void createPostProcess(T object) {}

    /**
     * Executes any pending updates on persistent objects. E.g. saves them to the database, or calls the
     * appropriate operation of a web service, etc.
     */
    protected void commitTransaction() {}

    /**
     * Hook method called just before an object is used to populate the edit form.
     * @param object the object.
     */
    protected void editSetup(T object) {}

    /**
     * Hook method called after values from the edit form have been propagated to the object.
     * @param object the object.
     * @return true if the object is to be considered valid, false otherwise. In the latter case, the
     * object will not be saved; it is suggested that the cause of the validation failure be displayed
     * to the user (e.g. by using SessionMessages).
     */
    protected boolean editValidate(T object) {
        return true;
    }

    /**
     * Hook method called just before an existing object is actually saved to persistent storage.
     * @param object the object just edited.
     */
    protected void editPostProcess(T object) {}

    /**
     * Hook method called before an object is deleted.
     * @param object the object.
     * @return true if the delete operation is to be performed, false otherwise. In the latter case,
     * it is suggested that the cause of the validation failure be displayed to the user
     * (e.g. by using SessionMessages).
     */
    protected boolean deleteValidate(T object) {
        return true;
    }

    /**
     * Hook method called just before an object is deleted from persistent storage, but after the doDelete
     * method has been called.
     * @param object the object.
     */
    protected void deletePostProcess(T object) {}

    /**
     * Returns the Resolution used to show the Bulk Edit page.
     */
    protected Resolution getBulkEditView() {
        return new ForwardResolution("/m/crud/bulk-edit.jsp");
    }

    /**
     * Returns the Resolution used to show the Create page.
     */
    protected Resolution getCreateView() { //TODO spezzare in popup/non-popup?
        if(isPopup()) {
            return new ForwardResolution("/m/crud/popup/create.jsp");
        } else {
            return new ForwardResolution("/m/crud/create.jsp");
        }
    }

    /**
     * Returns the Resolution used to show the Edit page.
     */
    protected Resolution getEditView() {
        return new ForwardResolution("/m/crud/edit.jsp");
    }

    /**
     * Returns the Resolution used to show the Read page.
     */
    protected Resolution getReadView() {
        return forwardTo("/m/crud/read.jsp");
    }

    /**
     * Returns the Resolution used to show the Search page when this page is embedded in its parent.
     */
    protected Resolution getEmbeddedReadView() {
        return new ForwardResolution("/m/crud/read.jsp");
    }

    /**
     * Returns the Resolution used to show the Search page.
     */
    protected Resolution getSearchView() {
        return forwardTo("/m/crud/search.jsp");
    }

    /**
     * Returns the Resolution used to show the Search page when this page is embedded in its parent.
     */
    protected Resolution getEmbeddedSearchView() {
        return new ForwardResolution("/m/crud/search.jsp");
    }

    /**
     * Returns the Resolution used to display the search results when paginating or sorting via AJAX.
     */
    protected Resolution getSearchResultsPageView() {
        return new ForwardResolution("/m/crud/datatable.jsp");
    }

    //--------------------------------------------------------------------------
    // Setup
    //--------------------------------------------------------------------------

    public Resolution preparePage() {
        Resolution resolution = super.preparePage();
        if(resolution != null) {
            return resolution;
        }
        this.crudConfiguration = (CrudConfiguration) pageInstance.getConfiguration();

        if (crudConfiguration == null) {
            logger.warn("Crud is not configured: " + pageInstance.getPath());
            return null;
        }

        ClassAccessor innerAccessor = prepare(pageInstance);
        if (innerAccessor == null) {
            return null;
        }
        classAccessor = new CrudAccessor(crudConfiguration, innerAccessor);
        pkHelper = new PkHelper(classAccessor);

        List<String> parameters = pageInstance.getParameters();
        if(!parameters.isEmpty()) {
            String encoding = portofinoConfiguration.getString(PortofinoProperties.URL_ENCODING);
            pk = parameters.toArray(new String[parameters.size()]);
            try {
                for(int i = 0; i < pk.length; i++) {
                    pk[i] = URLDecoder.decode(pk[i], encoding);
                }
            } catch (UnsupportedEncodingException e) {
                throw new Error(e);
            }
            OgnlContext ognlContext = ElementsThreadLocals.getOgnlContext();

            Serializable pkObject;
            try {
                pkObject = pkHelper.getPrimaryKey(pk);
            } catch (Exception e) {
                logger.warn("Invalid primary key", e);
                return notInUseCase(context, parameters);
            }
            object = loadObjectByPrimaryKey(pkObject);
            if(object != null) {
                ognlContext.put(crudConfiguration.getActualVariable(), object);
                String description = ShortNameUtils.getName(classAccessor, object);
                pageInstance.setDescription(description);
            } else {
                return notInUseCase(context, parameters);
            }
        }
        return null;
    }

    protected Resolution notInUseCase(ActionBeanContext context, List<String> parameters) {
        logger.info("Not in use case: " + crudConfiguration.getName());
        String msg = ElementsThreadLocals.getText("crud.notInUseCase", StringUtils.join(parameters, "/"));
        SessionMessages.addWarningMessage(msg);
        return new ForwardResolution("/m/pageactions/redirect-to-last-working-page.jsp");
    }

    /**
     * <p>Builds the ClassAccessor used to create, manipulate and introspect persistent objects.</p>
     * <p>This method is called during the prepare phase.</p>
     * @param pageInstance the PageInstance corresponding to this action in the current dispatch.
     * @return the ClassAccessor.
     */
    protected abstract ClassAccessor prepare(PageInstance pageInstance);

    public boolean isConfigured() {
        return (classAccessor != null);
    }

    protected void setupPagination() {
        resultSetNavigation = new ResultSetNavigation();
        int position = objects.indexOf(object);
        int size = objects.size();
        resultSetNavigation.setPosition(position);
        resultSetNavigation.setSize(size);
        String baseUrl = calculateBaseSearchUrl();
        if(position >= 0) {
            if(position > 0) {
                resultSetNavigation.setFirstUrl(generateObjectUrl(baseUrl, 0));
                resultSetNavigation.setPreviousUrl(
                        generateObjectUrl(baseUrl, position - 1));
            }
            if(position < size - 1) {
                resultSetNavigation.setLastUrl(
                        generateObjectUrl(baseUrl, size - 1));
                resultSetNavigation.setNextUrl(
                        generateObjectUrl(baseUrl, position + 1));
            }
        }
    }

    protected String calculateBaseSearchUrl() {
        assert pk != null; //Ha senso solo in modalita' read/detail
        String baseUrl = getDispatch().getAbsoluteOriginalPath();
        for(int i = 0; i < pk.length; i++) {
            int lastSlashIndex = baseUrl.lastIndexOf('/');
            baseUrl = baseUrl.substring(0, lastSlashIndex);
        }
        return baseUrl;
    }

    protected String generateObjectUrl(String baseUrl, int index) {
        Object o = objects.get(index);
        return generateObjectUrl(baseUrl, o);
    }

    protected String generateObjectUrl(String baseUrl, Object o) {
        String[] objPk = pkHelper.generatePkStringArray(o);
        String url = baseUrl + "/" + getPkForUrl(objPk);
        return new UrlBuilder(
                Locale.getDefault(), appendSearchStringParamIfNecessary(url), false)
                .toString();
    }

    protected void setupSearchForm() {
        SearchFormBuilder searchFormBuilder =
                new SearchFormBuilder(classAccessor);

        // setup option providers
        for (CrudSelectionProvider current : selectionProviderSupport.getCrudSelectionProviders()) {
            SelectionProvider selectionProvider =
                    current.getSelectionProvider();
            if(selectionProvider == null) {
                continue;
            }
            String[] fieldNames = current.getFieldNames();
            searchFormBuilder.configSelectionProvider(selectionProvider, fieldNames);
        }

        searchForm = searchFormBuilder
                .configPrefix(searchPrefix)
                .build();

        if(!isEmbedded()) {
            logger.debug("Search form not embedded, no risk of clashes - reading parameters from request");
            readSearchFormFromRequest();
        }
    }

    protected void readSearchFormFromRequest() {
        if (StringUtils.isBlank(searchString)) {
            searchForm.readFromRequest(context.getRequest());
            searchString = searchForm.toSearchString();
            if (searchString.length() == 0) {
                searchString = null;
            } else {
                searchVisible = true;
            }
        } else {
            MutableHttpServletRequest dummyRequest = new MutableHttpServletRequest();
            String[] parts = searchString.split(",");
            Pattern pattern = Pattern.compile("(.*)=(.*)");
            for (String part : parts) {
                Matcher matcher = pattern.matcher(part);
                if (matcher.matches()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    logger.debug("Matched part: {}={}", key, value);
                    dummyRequest.addParameter(key, value);
                } else {
                    logger.debug("Could not match part: {}", part);
                }
            }
            searchForm.readFromRequest(dummyRequest);
            searchVisible = true;
        }
    }

    protected void setupTableForm(Mode mode) {
        TableFormBuilder tableFormBuilder = createTableFormBuilder();
        configureTableFormSelectionProviders(tableFormBuilder);

        int nRows;
        if (objects == null) {
            nRows = 0;
        } else {
            nRows = objects.size();
        }

        configureTableFormBuilder(tableFormBuilder, mode, nRows);
        tableForm = buildTableForm(tableFormBuilder);

        if (objects != null) {
            tableForm.readFromObject(objects);
            refreshTableBlobDownloadHref();
        }
    }

    protected void configureTableFormSelectionProviders(TableFormBuilder tableFormBuilder) {
        // setup option providers
        for (CrudSelectionProvider current : selectionProviderSupport.getCrudSelectionProviders()) {
            SelectionProvider selectionProvider = current.getSelectionProvider();
            if(selectionProvider == null) {
                continue;
            }
            String[] fieldNames = current.getFieldNames();
            tableFormBuilder.configSelectionProvider(selectionProvider, fieldNames);
        }
    }

    protected void configureDetailLink(TableFormBuilder tableFormBuilder) {
        boolean isShowingKey = false;
        for (PropertyAccessor property : classAccessor.getKeyProperties()) {
            if(tableFormBuilder.getPropertyAccessors().contains(property) &&
               tableFormBuilder.isPropertyVisible(property)) {
                isShowingKey = true;
                break;
            }
        }

        String readLinkExpression = getReadLinkExpression();
        OgnlTextFormat hrefFormat =
                OgnlTextFormat.create(readLinkExpression);
        hrefFormat.setUrl(true);
        String encoding = portofinoConfiguration.getString(PortofinoProperties.URL_ENCODING);
        hrefFormat.setEncoding(encoding);

        if(isShowingKey) {
            logger.debug("TableForm: configuring detail links for primary key properties");
            for (PropertyAccessor property : classAccessor.getKeyProperties()) {
                tableFormBuilder.configHrefTextFormat(property.getName(), hrefFormat);
            }
        } else {
            logger.debug("TableForm: configuring detail link for the first visible property");
            for (PropertyAccessor property : classAccessor.getProperties()) {
                if(tableFormBuilder.getPropertyAccessors().contains(property) &&
                   tableFormBuilder.isPropertyVisible(property)) {
                    tableFormBuilder.configHrefTextFormat(
                        property.getName(), hrefFormat);
                    break;
                }
            }
        }
    }

    protected void configureSortLinks(TableFormBuilder tableFormBuilder) {
        for(PropertyAccessor propertyAccessor : classAccessor.getProperties()) {
            String propName = propertyAccessor.getName();
            String sortDirection;
            if(propName.equals(sortProperty) && "asc".equals(this.sortDirection)) {
                sortDirection = "desc";
            } else {
                sortDirection = "asc";
            }

            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("sortProperty", propName);
            parameters.put("sortDirection", sortDirection);
            if(!isEmbedded()) {
                parameters.put(SEARCH_STRING_PARAM, searchString);
            }
            parameters.put(context.getEventName(), "");

            UrlBuilder urlBuilder =
                    new UrlBuilder(Locale.getDefault(), dispatch.getAbsoluteOriginalPath(), false)
                            .addParameters(parameters);

            XhtmlBuffer xb = new XhtmlBuffer();
            xb.openElement("a");
            xb.addAttribute("class", "sort-link");
            xb.addAttribute("href", urlBuilder.toString());
            xb.writeNoHtmlEscape("%{label}");
            if(propName.equals(sortProperty)) {
                xb.openElement("i");
                xb.addAttribute("class", "pull-right icon-chevron-" + ("desc".equals(sortDirection) ? "up" : "down"));
                xb.closeElement("i");
            }
            xb.closeElement("a");
            OgnlTextFormat hrefFormat = OgnlTextFormat.create(xb.toString());
            String encoding = portofinoConfiguration.getString(PortofinoProperties.URL_ENCODING);
            hrefFormat.setEncoding(encoding);
            tableFormBuilder.configHeaderTextFormat(propName, hrefFormat);
        }
    }

    public String getLinkToPage(int page) {
        int rowsPerPage = getCrudConfiguration().getRowsPerPage();
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("sortProperty", getSortProperty());
        parameters.put("sortDirection", getSortDirection());
        parameters.put("firstResult", page * rowsPerPage);
        parameters.put("maxResults", rowsPerPage);
        if(!isEmbedded()) {
            parameters.put(AbstractCrudAction.SEARCH_STRING_PARAM, getSearchString());
        }

        UrlBuilder urlBuilder =
                new UrlBuilder(Locale.getDefault(), getDispatch().getAbsoluteOriginalPath(), false)
                        .addParameters(parameters);
        return urlBuilder.toString();
    }

    protected TableForm buildTableForm(TableFormBuilder tableFormBuilder) {
        TableForm tableForm = tableFormBuilder.build();

        tableForm.setKeyGenerator(pkHelper.createPkGenerator());
        tableForm.setSelectable(true);
        tableForm.setCondensed(true);

        return tableForm;
    }

    protected TableFormBuilder createTableFormBuilder() {
        return new TableFormBuilder(classAccessor);
    }

    /**
     * Configures the builder for the search results form. You can override this method to customize how
     * the form is generated (e.g. adding custom links on specific columns, hiding or showing columns
     * based on some runtime condition, etc.).
     * @param tableFormBuilder the table form builder.
     * @param mode the mode of the form.
     * @param nRows number of rows to display.
     * @return the table form builder.
     */
    protected TableFormBuilder configureTableFormBuilder(TableFormBuilder tableFormBuilder, Mode mode, int nRows) {
        tableFormBuilder.configPrefix(prefix).configNRows(nRows).configMode(mode);
        if(tableFormBuilder.getPropertyAccessors() == null) {
            tableFormBuilder.configReflectiveFields();
        }

        configureDetailLink(tableFormBuilder);
        configureSortLinks(tableFormBuilder);

        return tableFormBuilder;
    }

    protected void setupForm(Mode mode) {
        FormBuilder formBuilder = createFormBuilder();
        configureFormBuilder(formBuilder, mode);
        form = buildForm(formBuilder);
    }

    protected void configureFormSelectionProviders(FormBuilder formBuilder) {
        // setup option providers
        for (CrudSelectionProvider current : selectionProviderSupport.getCrudSelectionProviders()) {
            SelectionProvider selectionProvider = current.getSelectionProvider();
            if(selectionProvider == null) {
                continue;
            }
            String[] fieldNames = current.getFieldNames();
            if(object != null) {
                Object[] values = new Object[fieldNames.length];
                boolean valuesRead = true;
                for(int i = 0; i < fieldNames.length; i++) {
                    String fieldName = fieldNames[i];
                    try {
                        PropertyAccessor propertyAccessor = classAccessor.getProperty(fieldName);
                        values[i] = propertyAccessor.get(object);
                    } catch (Exception e) {
                        logger.error("Couldn't read property " + fieldName, e);
                        valuesRead = false;
                    }
                }
                if(valuesRead) {
                    selectionProvider.ensureActive(values);
                }
            }
            formBuilder.configSelectionProvider(selectionProvider, fieldNames);
        }
    }

    protected Form buildForm(FormBuilder formBuilder) {
        return formBuilder.build();
    }

    protected FormBuilder createFormBuilder() {
        return new FormBuilder(classAccessor);
    }

    /**
     * Configures the builder for the search detail (view, create, edit) form.
     * You can override this method to customize how the form is generated
     * (e.g. adding custom links on specific properties, hiding or showing properties
     * based on some runtime condition, etc.).
     * @param formBuilder the form builder.
     * @param mode the mode of the form.
     * @return the form builder.
     */
    protected FormBuilder configureFormBuilder(FormBuilder formBuilder, Mode mode) {
        formBuilder.configPrefix(prefix).configMode(mode);
        configureFormSelectionProviders(formBuilder);
        return formBuilder;
    }

    //**************************************************************************
    // Return to parent
    //**************************************************************************

    @Override
    public String getDescription() {
        if(pageInstance.getParameters().isEmpty()) {
            return crudConfiguration.getSearchTitle();
        } else {
            return ShortNameUtils.getName(classAccessor, object);
        }
    }

    /**
     * <p>Detects the parent page to return to (using the return to... button). This can be another PageAction,
     * or, at the discretion of subclasses, a different view of the same action.</p>
     * <p>This method assigns a value to the returnToParentTarget field.</p>
     */
    public void setupReturnToParentTarget() {
        if (pk != null) {
            if(!StringUtils.isBlank(searchString)) {
                returnToParentParams.put(SEARCH_STRING_PARAM, searchString);
            }
            returnToParentTarget = ElementsThreadLocals.getText("layouts.crud.search");
        }/* else {
            PageInstance[] pageInstancePath =
                dispatch.getPageInstancePath();
            boolean hasPrevious = getPage().getActualNavigationRoot() == NavigationRoot.INHERIT;
            hasPrevious = hasPrevious && pageInstancePath.length > 1;
            if(hasPrevious) {
                Page parentPage = pageInstancePath[pageInstancePath.length - 2].getPage();
                hasPrevious = parentPage.getActualNavigationRoot() != NavigationRoot.GHOST_ROOT;
            }
            returnToParentTarget = null;
            if (hasPrevious) {
                int previousPos = pageInstancePath.length - 2;
                PageInstance previousPageInstance = pageInstancePath[previousPos];
                //Page previousPage = previousPageInstance.getPage();

                //TODO ripristinare
                //if(!previousPage.isShowInNavigation()) {
                //    return;
                //}

                PageAction actionBean = previousPageInstance.getActionBean();
                if(actionBean != null) {
                    returnToParentTarget = actionBean.getDescription();
                }
            }
        }*/
    }

    public Resolution returnToParent() throws Exception {
        if (pk != null) {
            return new RedirectResolution(
                    appendSearchStringParamIfNecessary(calculateBaseSearchUrl()), false);
        } else {
            return new ErrorResolution(500);
        }/* else {
            PageInstance[] pageInstancePath =
                    getDispatch().getPageInstancePath();
            int previousPos = pageInstancePath.length - 2;
            if (previousPos >= 0) {
                PageInstance previousPageInstance = pageInstancePath[previousPos];
                String url = previousPageInstance.getPath();
                resolution = new RedirectResolution(url, true);
            } else {
                resolution = new RedirectResolution(
                        appendSearchStringParamIfNecessary(calculateBaseSearchUrl()), false);
            }
        }*/
    }

    @Override
    @Buttons({
        @Button(list = "crud-edit", key = "commons.cancel", order = 99),
        @Button(list = "crud-create", key = "commons.cancel", order = 99),
        @Button(list = "crud-bulk-edit", key = "commons.cancel", order = 99),
        @Button(list = "configuration", key = "commons.cancel", order = 99)
    })
    public Resolution cancel() {
        if(isPopup()) {
            popupCloseCallback += "(false)";
            return new ForwardResolution("/m/crud/popup/close.jsp");
        } else {
            return super.cancel();
        }
    }

    //--------------------------------------------------------------------------
    // Blob management
    //--------------------------------------------------------------------------

    protected void refreshBlobDownloadHref() {
        for (FieldSet fieldSet : form) {
            for (Field field : fieldSet.fields()) {
                if (field instanceof FileBlobField) {
                    FileBlobField fileBlobField = (FileBlobField) field;
                    Blob blob = fileBlobField.getValue();
                    if (blob != null) {
                        String url = getBlobDownloadUrl(fileBlobField);
                        field.setHref(url);
                    }
                }
            }
        }
    }

    protected void refreshTableBlobDownloadHref() {
        Iterator<?> objIterator = objects.iterator();
        for (TableForm.Row row : tableForm.getRows()) {
            Iterator<Field> fieldIterator = row.iterator();
            Object obj = objIterator.next();
            String baseUrl = null;
            while (fieldIterator.hasNext()) {
                Field field = fieldIterator.next();
                if (field instanceof FileBlobField) {
                    if(baseUrl == null) {
                        String readLinkExpression = getReadLinkExpression();
                        String encoding = portofinoConfiguration.getString(PortofinoProperties.URL_ENCODING);
                        OgnlTextFormat hrefFormat =
                                OgnlTextFormat.create(readLinkExpression);
                        hrefFormat.setUrl(true);
                        hrefFormat.setEncoding(encoding);
                        baseUrl = hrefFormat.format(obj);
                    }

                    Blob blob = ((FileBlobField) field).getValue();
                    if(blob != null) {
                        UrlBuilder urlBuilder = new UrlBuilder(Locale.getDefault(), baseUrl, false)
                            .addParameter("downloadBlob", "")
                            .addParameter("propertyName", field.getPropertyAccessor().getName())
                            .addParameter("code", blob.getCode());

                        field.setHref(urlBuilder.toString());
                    }
                }
            }
        }
    }

    public String getBlobDownloadUrl(FileBlobField field) {
        UrlBuilder urlBuilder = new UrlBuilder(
                Locale.getDefault(), getDispatch().getAbsoluteOriginalPath(), false)
                .addParameter("downloadBlob","")
                .addParameter("propertyName", field.getPropertyAccessor().getName())
                .addParameter("code", field.getValue().getCode());
        return urlBuilder.toString();
    }

    public Resolution downloadBlob() throws IOException, NoSuchFieldException {
        PropertyAccessor propertyAccessor =
                classAccessor.getProperty(propertyName);
        String code = (String) propertyAccessor.get(object);

        BlobManager blobManager = ElementsThreadLocals.getBlobManager();
        Blob blob = blobManager.loadBlob(code);
        long contentLength = blob.getSize();
        String contentType = blob.getContentType();
        InputStream inputStream = new FileInputStream(blob.getDataFile());
        String fileName = blob.getFilename();

        //Cache blobs (they're immutable)
        HttpServletResponse response = context.getResponse();
        ServletUtils.markCacheableForever(response);

        return new StreamingResolution(contentType, inputStream)
                .setFilename(fileName)
                .setLength(contentLength)
                .setLastModified(blob.getCreateTimestamp().getMillis());
    }

    /**
     * Removes all the file blobs associated with the object from the file system.
     * @param object the persistent object.
     */
    protected void deleteFileBlobs(T object) {
        setupForm(Mode.VIEW);
        form.readFromObject(object);
        BlobManager blobManager = ElementsThreadLocals.getBlobManager();
        for(FieldSet fieldSet : form) {
            for(FormElement field : fieldSet) {
                if(field instanceof FileBlobField) {
                    Blob blob = ((FileBlobField) field).getValue();
                    if(blob != null) {
                        if(!blobManager.deleteBlob(blob.getCode())) {
                            logger.warn("Could not delete blob: " + blob.getCode());
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns a list of the blobs loaded by the form.
     * @return the list of blobs.
     */
    protected List<Blob> getBlobs() {
        List<Blob> blobs = new ArrayList<Blob>();
        for(FieldSet fieldSet : form) {
            for(FormElement field : fieldSet) {
                if(field instanceof FileBlobField) {
                    FileBlobField fileBlobField = (FileBlobField) field;
                    Blob blob = fileBlobField.getValue();
                    blobs.add(blob);
                }
            }
        }
        return blobs;
    }

    //**************************************************************************
    // ExportSearch
    //**************************************************************************

    @Button(list = "crud-search", key = "commons.exportExcel", order = 5, group = "export")
    public Resolution exportSearchExcel() {
        try {
            TempFileService fileService = TempFileService.getInstance();
            TempFile tempFile =
                    fileService.newTempFile("application/vnd.ms-excel", crudConfiguration.getSearchTitle() + ".xls");
            OutputStream outputStream = tempFile.getOutputStream();
            exportSearchExcel(outputStream);
            outputStream.flush();
            outputStream.close();
            return fileService.stream(tempFile);
        } catch (Exception e) {
            logger.error("Excel export failed", e);
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("commons.export.failed"));
            return new RedirectResolution(getDispatch().getOriginalPath());
        }
    }

    public void exportSearchExcel(OutputStream outputStream) {
        setupSearchForm();
        loadObjects();
        setupTableForm(Mode.VIEW);

        writeFileSearchExcel(outputStream);
    }

    private void writeFileSearchExcel(OutputStream outputStream) {
        WritableWorkbook workbook = null;
        try {
            WorkbookSettings workbookSettings = new WorkbookSettings();
            workbookSettings.setUseTemporaryFileDuringWrite(false);
            workbook = Workbook.createWorkbook(outputStream, workbookSettings);
            String title = crudConfiguration.getSearchTitle();
            if(StringUtils.isBlank(title)) {
                title = "export";
            }
            WritableSheet sheet =
                    workbook.createSheet(title, 0);

            addHeaderToSearchSheet(sheet);

            int i = 1;
            for ( TableForm.Row row : tableForm.getRows()) {
                exportRows(sheet, i, row);
                i++;
            }

            workbook.write();
        } catch (IOException e) {
            logger.warn("IOException", e);
            SessionMessages.addErrorMessage(e.getMessage());
        } catch (RowsExceededException e) {
            logger.warn("RowsExceededException", e);
            SessionMessages.addErrorMessage(e.getMessage());
        } catch (WriteException e) {
            logger.warn("WriteException", e);
            SessionMessages.addErrorMessage(e.getMessage());
        } finally {
            try {
                if (workbook != null)
                    workbook.close();
            }
            catch (Exception e) {
                logger.warn("IOException", e);
                SessionMessages.addErrorMessage(e.getMessage());
            }
        }
    }

    //**************************************************************************
    // ExportRead
    //**************************************************************************

    @Button(list = "crud-read", key = "commons.exportExcel", order = 4, group = "export")
    public Resolution exportReadExcel() {
        try {
            TempFileService fileService = TempFileService.getInstance();
            TempFile tempFile =
                    fileService.newTempFile("application/vnd.ms-excel", getReadTitle() + ".xls");
            OutputStream outputStream = tempFile.getOutputStream();
            exportReadExcel(outputStream);
            outputStream.flush();
            outputStream.close();
            return fileService.stream(tempFile);
        } catch (Exception e) {
            logger.error("Excel export failed", e);
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("commons.export.failed"));
            return new RedirectResolution(getDispatch().getOriginalPath());
        }
    }

    public void exportReadExcel(OutputStream outputStream)
            throws IOException, WriteException {
        setupSearchForm();

        loadObjects();

        setupForm(Mode.VIEW);
        form.readFromObject(object);

        writeFileReadExcel(outputStream);
    }

    private void writeFileReadExcel(OutputStream outputStream)
            throws IOException, WriteException {
        WritableWorkbook workbook = null;
        try {
            WorkbookSettings workbookSettings = new WorkbookSettings();
            workbookSettings.setUseTemporaryFileDuringWrite(false);
            workbook = Workbook.createWorkbook(outputStream, workbookSettings);
            WritableSheet sheet =
                workbook.createSheet(getReadTitle(), workbook.getNumberOfSheets());

            addHeaderToReadSheet(sheet);

            int i = 1;
            for (FieldSet fieldset : form) {
                int j = 0;
                for (Field field : fieldset.fields()) {
                    addFieldToCell(sheet, i, j, field);
                    j++;
                }
                i++;
            }
            workbook.write();
        } catch (IOException e) {
            logger.warn("IOException", e);
            SessionMessages.addErrorMessage(e.getMessage());
        } catch (RowsExceededException e) {
            logger.warn("RowsExceededException", e);
            SessionMessages.addErrorMessage(e.getMessage());
        } catch (WriteException e) {
            logger.warn("WriteException", e);
            SessionMessages.addErrorMessage(e.getMessage());
        } finally {
            try {
                if (workbook != null)
                    workbook.close();
            }
            catch (Exception e) {
                logger.warn("IOException", e);
                SessionMessages.addErrorMessage(e.getMessage());
            }
        }
    }

    private WritableCellFormat headerExcel() {
        WritableFont fontCell = new WritableFont(WritableFont.ARIAL, 12,
             WritableFont.BOLD, true);
        return new WritableCellFormat (fontCell);
    }

    private void exportRows(WritableSheet sheet, int i,
                            TableForm.Row row) throws WriteException {
        int j = 0;
        for (Field field : row) {
            addFieldToCell(sheet, i, j, field);
            j++;
        }
    }

    private void addHeaderToReadSheet(WritableSheet sheet) throws WriteException {
        WritableCellFormat formatCell = headerExcel();
        int i = 0;
        for (FieldSet fieldset : form) {
            for (Field field : fieldset.fields()) {
                sheet.addCell(new jxl.write.Label(i, 0, field.getLabel(), formatCell));
                i++;
            }
        }
    }

    private void addHeaderToSearchSheet(WritableSheet sheet) throws WriteException {
        WritableCellFormat formatCell = headerExcel();
        int l = 0;
        for (TableForm.Column col : tableForm.getColumns()) {
            sheet.addCell(new jxl.write.Label(l, 0, col.getLabel(), formatCell));
            l++;
        }
    }

    private void addFieldToCell(WritableSheet sheet, int i, int j,
                                Field field) throws WriteException {
        if (field instanceof NumericField) {
            NumericField numField = (NumericField) field;
            if (numField.getValue() != null) {
                Number number;
                BigDecimal decimalValue = numField.getValue();
                if (numField.getDecimalFormat() == null) {
                    number = new Number(j, i,
                            decimalValue == null
                                    ? null : decimalValue.doubleValue());
                } else {
                    NumberFormat numberFormat = new NumberFormat(
                            numField.getDecimalFormat().toPattern());
                    WritableCellFormat writeCellNumberFormat =
                            new WritableCellFormat(numberFormat);
                    number = new Number(j, i,
                            decimalValue == null
                                    ? null : decimalValue.doubleValue(),
                            writeCellNumberFormat);
                }
                sheet.addCell(number);
            }
        } else if (field instanceof PasswordField) {
            jxl.write.Label label = new jxl.write.Label(j, i,
                    PasswordField.PASSWORD_PLACEHOLDER);
            sheet.addCell(label);
        } else if (field instanceof DateField) {
            DateField dateField = (DateField) field;
            DateTime dateCell;
            Date date = dateField.getValue();
            if (date != null) {
                DateFormat dateFormat = new DateFormat(
                        dateField.getDatePattern());
                WritableCellFormat wDateFormat =
                        new WritableCellFormat(dateFormat);
                dateCell = new DateTime(j, i,
                        dateField.getValue() == null
                                ? null : dateField.getValue(),
                        wDateFormat);
                sheet.addCell(dateCell);
            }
        } else {
            jxl.write.Label label = new jxl.write.Label(j, i, field.getStringValue());
            sheet.addCell(label);
        }
    }

    //**************************************************************************
    // exportSearchPdf
    //**************************************************************************

    @Button(list = "crud-search", key = "commons.exportPdf", order = 4, group = "export")
    public Resolution exportSearchPdf() {
        try {
            //final File tmpFile = File.createTempFile(crudConfiguration.getName() + ".search", ".pdf");
            TempFileService fileService = TempFileService.getInstance();
            TempFile tempFile =
                    fileService.newTempFile(MimeTypes.APPLICATION_PDF, crudConfiguration.getSearchTitle() + ".pdf");
            OutputStream outputStream = tempFile.getOutputStream();
            exportSearchPdf(outputStream);
            outputStream.flush();
            outputStream.close();
            return fileService.stream(tempFile);
        } catch (Exception e) {
            logger.error("PDF export failed", e);
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("commons.export.failed"));
            return new RedirectResolution(getDispatch().getOriginalPath());
        }
    }

    public void exportSearchPdf(OutputStream outputStream) throws FOPException,
            IOException, TransformerException {

        setupSearchForm();

        loadObjects();

        setupTableForm(Mode.VIEW);

        FopFactory fopFactory = FopFactory.newInstance();

        InputStream xsltStream = null;
        try {
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, outputStream);

            xsltStream = getSearchPdfXsltStream();

            // Setup XSLT
            TransformerFactory Factory = TransformerFactory.newInstance();
            Transformer transformer = Factory.newTransformer(new StreamSource(
                    xsltStream));

            // Set the value of a <param> in the stylesheet
            transformer.setParameter("versionParam", "2.0");

            // Setup input for XSLT transformation
            Reader reader = composeXmlSearch();
            Source src = new StreamSource(reader);

            // Resulting SAX events (the generated FO) must be piped through to
            // FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);
            reader.close();

            outputStream.flush();
        } finally {
            IOUtils.closeQuietly(xsltStream);
        }
    }

    /**
     * Returns a stream producing the contents of a XSLT document to produce the PDF export of the
     * current search results.
     */
    protected InputStream getSearchPdfXsltStream() {
        String templateFop = TEMPLATE_FOP_SEARCH;
        return getXsltStream(templateFop);
    }

    /**
     * Returns a XSLT stream by searching for a file first in this action's directory, then at
     * the root of the classpath.
     * @param templateFop the file to search for
     * @return the stream
     */
    protected InputStream getXsltStream(String templateFop) {
        File fopFile = new File(pageInstance.getDirectory(), templateFop);
        if(fopFile.exists()) {
            logger.debug("Custom FOP template found: {}", fopFile);
            try {
                return new FileInputStream(fopFile);
            } catch (FileNotFoundException e) {
                throw new Error(e);
            }
        } else {
            logger.debug("Using default FOP template: {}", templateFop);
            ClassLoader cl = getClass().getClassLoader();
            return cl.getResourceAsStream(templateFop);
        }
    }

    /**
     * Composes an XML document representing the current search results.
     * @return
     * @throws IOException
     */
    protected Reader composeXmlSearch() throws IOException {
        XmlBuffer xb = new XmlBuffer();
        xb.writeXmlHeader("UTF-8");
        xb.openElement("class");
        xb.openElement("table");
        xb.write(crudConfiguration.getSearchTitle());
        xb.closeElement("table");

        double[] columnSizes = setupXmlSearchColumnSizes();

        for (double columnSize : columnSizes) {
            xb.openElement("column");
            xb.openElement("width");
            xb.write(columnSize + "em");
            xb.closeElement("width");
            xb.closeElement("column");
        }

        for (TableForm.Column col : tableForm.getColumns()) {
            xb.openElement("header");
            xb.openElement("nameColumn");
            xb.write(col.getLabel());
            xb.closeElement("nameColumn");
            xb.closeElement("header");
        }


        for (TableForm.Row row : tableForm.getRows()) {
            xb.openElement("rows");
            for (Field field : row) {
                xb.openElement("row");
                xb.openElement("value");
                xb.write(field.getStringValue());
                xb.closeElement("value");
                xb.closeElement("row");
            }
            xb.closeElement("rows");
        }

        xb.closeElement("class");

        return new StringReader(xb.toString());
    }

    /**
     * <p>Returns an array of column sizes (in characters) for the search export.<br />
     * By default, sizes are computed comparing the relative sizes of each column,
     * consisting of the header and the values produced by the search.</p>
     * <p>Users can override this method to compute the sizes using a different algorithm,
     * or hard-coding them for a particular CRUD instance.</p>
     */
    protected double[] setupXmlSearchColumnSizes() {
        double[] headerSizes = new double[tableForm.getColumns().length];
        for(int i = 0; i < headerSizes.length; i++) {
            TableForm.Column col = tableForm.getColumns()[i];
            int length = StringUtils.length(col.getLabel());
            headerSizes[i] = length;
        }

        double[] columnSizes = new double[tableForm.getColumns().length];
        for (TableForm.Row row : tableForm.getRows()) {
            int i = 0;
            for (Field field : row) {
                int size = StringUtils.length(field.getStringValue());
                double relativeSize = ((double) size) / tableForm.getRows().length;
                columnSizes[i++] += relativeSize;
            }
        }

        double totalSize = 0;
        for (int i = 0; i < columnSizes.length; i++) {
            double effectiveSize = Math.max(columnSizes[i], headerSizes[i]);
            columnSizes[i] = effectiveSize;
            totalSize += effectiveSize;
        }
        while(totalSize > 75) {
            int maxIndex = 0;
            double max = 0;
            for(int i = 0; i < columnSizes.length; i++) {
                if(columnSizes[i] > max) {
                    max = columnSizes[i];
                    maxIndex = i;
                }
            }
            columnSizes[maxIndex] -= 1;
            totalSize -= 1;
        }
        while(totalSize < 70) {
            int minIndex = 0;
            double min = Double.MAX_VALUE;
            for(int i = 0; i < columnSizes.length; i++) {
                if(columnSizes[i] < min) {
                    min = columnSizes[i];
                    minIndex = i;
                }
            }
            columnSizes[minIndex] += 1;
            totalSize += 1;
        }
        return columnSizes;
    }

    //**************************************************************************
    // ExportRead
    //**************************************************************************

    /**
     * Composes an XML document representing the current object.
     * @return
     * @throws IOException
     */
    protected Reader composeXmlPort()
            throws IOException, WriteException {
        setupSearchForm();

        loadObjects();

        setupTableForm(Mode.VIEW);
        setupForm(Mode.VIEW);
        form.readFromObject(object);


        XmlBuffer xb = new XmlBuffer();
        xb.writeXmlHeader("UTF-8");
        xb.openElement("class");
        xb.openElement("table");
        xb.write(crudConfiguration.getReadTitle());
        xb.closeElement("table");

        for (FieldSet fieldset : form) {
            xb.openElement("tableData");
            xb.openElement("rows");

            for (Field field : fieldset.fields()) {
                xb.openElement("row");
                xb.openElement("nameColumn");
                xb.write(field.getLabel());
                xb.closeElement("nameColumn");

                xb.openElement("value");
                xb.write(field.getStringValue());
                xb.closeElement("value");
                xb.closeElement("row");

            }
            xb.closeElement("rows");
            xb.closeElement("tableData");
        }

        xb.closeElement("class");

        return new StringReader(xb.toString());
    }

     public void exportReadPdf(File tempPdfFile) throws FOPException,
            IOException, TransformerException {
        setupSearchForm();

        loadObjects();

        setupTableForm(Mode.VIEW);

        FopFactory fopFactory = FopFactory.newInstance();

        FileOutputStream out = null;
        InputStream xsltStream = null;
        try {
            out = new FileOutputStream(tempPdfFile);

            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

            xsltStream = getXsltStream(TEMPLATE_FOP_READ);

            // Setup XSLT
            TransformerFactory Factory = TransformerFactory.newInstance();
            Transformer transformer = Factory.newTransformer(new StreamSource(
                    xsltStream));

            // Set the value of a <param> in the stylesheet
            transformer.setParameter("versionParam", "2.0");

            // Setup input for XSLT transformation
            Reader reader = composeXmlPort();
            Source src = new StreamSource(reader);

            // Resulting SAX events (the generated FO) must be piped through to
            // FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            reader.close();
            out.flush();
        } catch (Exception e) {
            logger.warn("IOException", e);
            SessionMessages.addErrorMessage(e.getMessage());
        } finally {
            IOUtils.closeQuietly(xsltStream);
            try {
                if (out != null)
                    out.close();
            }
            catch (Exception e) {
                logger.warn("IOException", e);
                SessionMessages.addErrorMessage(e.getMessage());
            }
        }
    }

    @Button(list = "crud-read", key = "commons.exportPdf", order = 3, group = "export")
    public Resolution exportReadPdf() {
        try {
            final File tmpFile = File.createTempFile("export." + crudConfiguration.getName(), ".read.pdf");
            exportReadPdf(tmpFile);
            FileInputStream fileInputStream = new FileInputStream(tmpFile);
            return new StreamingResolution("application/pdf", fileInputStream) {
                @Override
                protected void stream(HttpServletResponse response) throws Exception {
                    super.stream(response);
                    if(!tmpFile.delete()) {
                        logger.warn("Temporary file {} could not be deleted", tmpFile.getAbsolutePath());
                    }
                }
            }.setFilename(getReadTitle() + ".pdf");
        } catch (Exception e) {
            logger.error("PDF export failed", e);
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("commons.export.failed"));
            return new RedirectResolution(getDispatch().getOriginalPath());
        }
    }

    //**************************************************************************
    // Configuration
    //**************************************************************************

    @Button(list = "portletHeaderButtons", titleKey = "commons.configure", order = 1, icon = Button.ICON_WRENCH)
    @RequiresPermissions(level = AccessLevel.DEVELOP)
    public Resolution configure() {
        prepareConfigurationForms();

        crudConfigurationForm.readFromObject(crudConfiguration);
        if(propertyEdits != null) {
            propertiesTableForm.readFromObject(propertyEdits);
        }

        if(selectionProviderEdits != null) {
            selectionProvidersForm.readFromObject(selectionProviderEdits);
        }

        return getConfigurationView();
    }

    /**
     * Returns the Resolution used to show the configuration page.
     */
    protected abstract Resolution getConfigurationView();

    @Override
    protected void prepareConfigurationForms() {
        super.prepareConfigurationForms();

        setupPropertyEdits();

        if(propertyEdits != null) {
            TableFormBuilder tableFormBuilder =
                    new TableFormBuilder(CrudPropertyEdit.class)
                        .configNRows(propertyEdits.length);
            propertiesTableForm = tableFormBuilder.build();
            propertiesTableForm.setCondensed(true);
        }

        if(selectionProviderSupport != null) {
            Map<List<String>, Collection<String>> selectionProviderNames =
                    selectionProviderSupport.getAvailableSelectionProviderNames();
            if(!selectionProviderNames.isEmpty()) {
                setupSelectionProviderEdits();
                setupSelectionProvidersForm(selectionProviderNames);
            }
        }
    }

    protected void setupSelectionProvidersForm(Map<List<String>, Collection<String>> selectionProviderNames) {
        TableFormBuilder tableFormBuilder = new TableFormBuilder(CrudSelectionProviderEdit.class);
        tableFormBuilder.configNRows(selectionProviderNames.size());
        for(int i = 0; i < selectionProviderEdits.length; i++) {
            Collection<String> availableProviders =
                    selectionProviderNames.get(Arrays.asList(selectionProviderEdits[i].fieldNames));
            if(availableProviders == null || availableProviders.size() == 0) {
                continue;
            }
            DefaultSelectionProvider selectionProvider =
                    new DefaultSelectionProvider(selectionProviderEdits[i].columns);
            selectionProvider.appendRow(null, "None", true);
            for(String spName : availableProviders) {
                selectionProvider.appendRow(spName, spName, true);
            }
            tableFormBuilder.configSelectionProvider(i, selectionProvider, "selectionProvider");
        }
        selectionProvidersForm = tableFormBuilder.build();
        selectionProvidersForm.setCondensed(true);
    }

    protected void setupPropertyEdits() {
        if(classAccessor == null) {
            return;
        }
        PropertyAccessor[] propertyAccessors = classAccessor.getProperties();
        propertyEdits = new CrudPropertyEdit[propertyAccessors.length];
        for (int i = 0; i < propertyAccessors.length; i++) {
            CrudPropertyEdit edit = new CrudPropertyEdit();
            PropertyAccessor propertyAccessor = propertyAccessors[i];
            edit.name = propertyAccessor.getName();
            com.manydesigns.elements.annotations.Label labelAnn = propertyAccessor.getAnnotation(com.manydesigns.elements.annotations.Label.class);
            edit.label = labelAnn != null ? labelAnn.value() : null;
            Enabled enabledAnn = propertyAccessor.getAnnotation(Enabled.class);
            edit.enabled = enabledAnn != null && enabledAnn.value();
            InSummary inSummaryAnn = propertyAccessor.getAnnotation(InSummary.class);
            edit.inSummary = inSummaryAnn != null && inSummaryAnn.value();
            Insertable insertableAnn = propertyAccessor.getAnnotation(Insertable.class);
            edit.insertable = insertableAnn != null && insertableAnn.value();
            Updatable updatableAnn = propertyAccessor.getAnnotation(Updatable.class);
            edit.updatable = updatableAnn != null && updatableAnn.value();
            Searchable searchableAnn = propertyAccessor.getAnnotation(Searchable.class);
            edit.searchable = searchableAnn != null && searchableAnn.value();
            propertyEdits[i] = edit;
        }
    }

    protected void setupSelectionProviderEdits() {
        Map<List<String>, Collection<String>> availableSelectionProviders =
                selectionProviderSupport.getAvailableSelectionProviderNames();
        selectionProviderEdits = new CrudSelectionProviderEdit[availableSelectionProviders.size()];
        int i = 0;
        for(List<String> key : availableSelectionProviders.keySet()) {
            selectionProviderEdits[i] = new CrudSelectionProviderEdit();
            String[] fieldNames = key.toArray(new String[key.size()]);
            selectionProviderEdits[i].fieldNames = fieldNames;
            selectionProviderEdits[i].columns = StringUtils.join(fieldNames, ", ");
            for(CrudSelectionProvider cp : selectionProviderSupport.getCrudSelectionProviders()) {
                if(Arrays.equals(cp.fieldNames, fieldNames)) {
                    SelectionProvider selectionProvider = cp.getSelectionProvider();
                    if(selectionProvider != null) {
                        selectionProviderEdits[i].selectionProvider = selectionProvider.getName();
                        selectionProviderEdits[i].displayMode = selectionProvider.getDisplayMode();
                        selectionProviderEdits[i].searchDisplayMode = selectionProvider.getSearchDisplayMode();
                        selectionProviderEdits[i].createNewHref = cp.getCreateNewValueHref();
                        selectionProviderEdits[i].createNewText = cp.getCreateNewValueText();
                    } else {
                        selectionProviderEdits[i].selectionProvider = null;
                        selectionProviderEdits[i].displayMode = DisplayMode.DROPDOWN;
                        selectionProviderEdits[i].searchDisplayMode = SearchDisplayMode.DROPDOWN;
                    }
                }
            }
            i++;
        }
    }

    @Button(list = "configuration", key = "commons.updateConfiguration", order = 1, type = Button.TYPE_PRIMARY)
    @RequiresPermissions(level = AccessLevel.DEVELOP)
    public Resolution updateConfiguration() {
        prepareConfigurationForms();

        crudConfigurationForm.readFromObject(crudConfiguration);

        readPageConfigurationFromRequest();

        crudConfigurationForm.readFromRequest(context.getRequest());

        boolean valid = crudConfigurationForm.validate();
        valid = validatePageConfiguration() && valid;

        if(propertiesTableForm != null) {
            propertiesTableForm.readFromObject(propertyEdits);
            propertiesTableForm.readFromRequest(context.getRequest());
            valid = propertiesTableForm.validate() && valid;
        }

        if(selectionProvidersForm != null) {
            selectionProvidersForm.readFromRequest(context.getRequest());
            valid = selectionProvidersForm.validate() && valid;
        }

        if (valid) {
            updatePageConfiguration();
            if(crudConfiguration == null) {
                crudConfiguration = new CrudConfiguration();
            }
            crudConfigurationForm.writeToObject(crudConfiguration);

            if(propertiesTableForm != null) {
                updateProperties();
            }

            if(selectionProviderSupport != null &&
               !selectionProviderSupport.getAvailableSelectionProviderNames().isEmpty()) {
                updateSelectionProviders();
            }

            saveConfiguration(crudConfiguration);

            SessionMessages.addInfoMessage(ElementsThreadLocals.getText("commons.configuration.updated"));
            return cancel();
        } else {
            SessionMessages.addErrorMessage(ElementsThreadLocals.getText("commons.configuration.notUpdated"));
            return getConfigurationView();
        }
    }

    protected void updateSelectionProviders() {
        selectionProvidersForm.writeToObject(selectionProviderEdits);
        crudConfiguration.getSelectionProviders().clear();
        for(CrudSelectionProviderEdit sp : selectionProviderEdits) {
            List<String> key = Arrays.asList(sp.fieldNames);
            if(sp.selectionProvider == null) {
                selectionProviderSupport.disableSelectionProvider(key);
            } else {
                selectionProviderSupport.configureSelectionProvider(
                        key, sp.selectionProvider, sp.displayMode, sp.searchDisplayMode,
                        sp.createNewHref, sp.createNewText);
            }
        }
    }

    protected void updateProperties() {
        propertiesTableForm.writeToObject(propertyEdits);

        List<CrudProperty> newProperties = new ArrayList<CrudProperty>();
        for (CrudPropertyEdit edit : propertyEdits) {
            CrudProperty crudProperty = findProperty(edit.name, crudConfiguration.getProperties());
            if(crudProperty == null) {
                crudProperty = new CrudProperty();
            }

            crudProperty.setName(edit.name);
            crudProperty.setLabel(edit.label);
            crudProperty.setInSummary(edit.inSummary);
            crudProperty.setSearchable(edit.searchable);
            crudProperty.setEnabled(edit.enabled);
            crudProperty.setInsertable(edit.insertable);
            crudProperty.setUpdatable(edit.updatable);

            newProperties.add(crudProperty);
        }
        crudConfiguration.getProperties().clear();
        crudConfiguration.getProperties().addAll(newProperties);
    }

    public boolean isRequiredFieldsPresent() {
        return form.isRequiredFieldsPresent();
    }

    //**************************************************************************
    // Ajax
    //**************************************************************************

    public Resolution jsonSelectFieldOptions() {
        return jsonOptions(prefix, true);
    }

    public Resolution jsonSelectFieldSearchOptions() {
        return jsonOptions(searchPrefix, true);
    }

    public Resolution jsonAutocompleteOptions() {
        return jsonOptions(prefix, false);
    }

    public Resolution jsonAutocompleteSearchOptions() {
        return jsonOptions(searchPrefix, false);
    }

    /**
     * Returns values to update multiple related select fields or a single autocomplete
     * text field, in JSON form.
     * @param prefix form prefix, to read values from the request.
     * @param includeSelectPrompt controls if the first option is a label with no value indicating
     * what field is being selected. For combo boxes you would generally pass true as the value of
     * this parameter; for autocomplete fields, you would likely pass false.
     * @return a Resolution to produce the JSON.
     */
    protected Resolution jsonOptions(String prefix, boolean includeSelectPrompt) {
        CrudSelectionProvider crudSelectionProvider = null;
        for (CrudSelectionProvider current : selectionProviderSupport.getCrudSelectionProviders()) {
            SelectionProvider selectionProvider =
                    current.getSelectionProvider();
            if (selectionProvider.getName().equals(relName)) {
                crudSelectionProvider = current;
                break;
            }
        }
        if (crudSelectionProvider == null) {
            return new ErrorResolution(500);
        }

        SelectionProvider selectionProvider =
                crudSelectionProvider.getSelectionProvider();
        String[] fieldNames = crudSelectionProvider.getFieldNames();

        Form form = buildForm(createFormBuilder()
                .configFields(fieldNames)
                .configSelectionProvider(selectionProvider, fieldNames)
                .configPrefix(prefix)
                .configMode(Mode.EDIT));

        FieldSet fieldSet = form.get(0);
        //Ensure the value is actually read from the request
        for(Field field : fieldSet.fields()) {
            field.setUpdatable(true);
        }
        form.readFromRequest(context.getRequest());

        SelectField targetField =
                (SelectField) fieldSet.get(selectionProviderIndex);
        targetField.setLabelSearch(labelSearch);

        String text = targetField.jsonSelectFieldOptions(includeSelectPrompt);
        logger.debug("jsonOptions: {}", text);
        return new StreamingResolution(MimeTypes.APPLICATION_JSON_UTF8, text);
    }

    //--------------------------------------------------------------------------
    // Utilities
    //--------------------------------------------------------------------------

    /**
     * Searches in a list of properties for a property with a given name.
     * @param name the name of the properties.
     * @param properties the list to search.
     * @return the property with the given name, or null if it couldn't be found.
     */
    protected CrudProperty findProperty(String name, List<CrudProperty> properties) {
        for(CrudProperty p : properties) {
            if(p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Encodes the exploded object indentifier to include it in a URL.
     * @param pk the object identifier as a String array.
     * @return the string to append to the URL.
     */
    protected String getPkForUrl(String[] pk) {
        String encoding = portofinoConfiguration.getString(PortofinoProperties.URL_ENCODING);
        try {
            return pkHelper.getPkStringForUrl(pk, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    /**
     * Returns an OGNL expression that, when evaluated against a persistent object, produces a
     * URL path suitable to be used as a link to that object.
     */
    protected String getReadLinkExpression() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDispatch().getOriginalPath());
        sb.append("/");
        boolean first = true;

        for (PropertyAccessor property : classAccessor.getKeyProperties()) {
            if (first) {
                first = false;
            } else {
                sb.append("/");
            }
            sb.append("%{");
            sb.append(property.getName());
            sb.append("}");
        }
        appendSearchStringParamIfNecessary(sb);
        return sb.toString();
    }

    /**
     * If a search has been executed, appends a URL-encoded String representation of the search criteria
     * to the given string, as a GET parameter.
     * @param s the base string.
     * @return the base string with the search criteria appended
     */
    protected String appendSearchStringParamIfNecessary(String s) {
        return appendSearchStringParamIfNecessary(new StringBuilder(s)).toString();
    }

    /**
     * If a search has been executed, appends a URL-encoded String representation of the search criteria
     * to the given StringBuilder, as a GET parameter. The StringBuilder's contents are modified.
     * @param sb the base string.
     * @return sb.
     */
    protected StringBuilder appendSearchStringParamIfNecessary(StringBuilder sb) {
        String searchStringParam = getEncodedSearchStringParam();
        if(searchStringParam != null) {
            if(sb.indexOf("?") == -1) {
                sb.append('?');
            } else {
                sb.append('&');
            }
            sb.append(searchStringParam);
        }
        return sb;
    }

    /**
     * Encodes the current search string (a representation of the current search criteria as a series of GET
     * parameters) to an URL-encoded GET parameter.
     * @return the encoded search string.
     */
    protected String getEncodedSearchStringParam() {
        if(StringUtils.isBlank(searchString)) {
            return null;
        }
        String encodedSearchString = "searchString=";
        try {
            String encoding = portofinoConfiguration.getString(PortofinoProperties.URL_ENCODING);
            String encoded = URLEncoder.encode(searchString, encoding);
            if(searchString.equals(URLDecoder.decode(encoded, encoding))) {
                encodedSearchString += encoded;
            } else {
                logger.warn("Could not encode search string \"" + StringEscapeUtils.escapeJava(searchString) +
                            "\" with encoding " + encoding);
                return null;
            }
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
        return encodedSearchString;
    }

    /**
     * Writes a collection of fields as properties of a JSON object.
     * @param js the JSONStringer to write to. Must have a JSON object open for writing.
     * @param fields the fields to output
     * @throws JSONException if the JSON can not be generated.
     */
    protected void fieldsToJson(JSONStringer js, Collection<Field> fields) throws JSONException {
        for (Field field : fields) {
            Object value = field.getValue();
            String displayValue = field.getDisplayValue();
            String href = field.getHref();
            js.key(field.getPropertyAccessor().getName());
            js.object()
                    .key("value")
                    .value(value)
                    .key("displayValue")
                    .value(displayValue)
                    .key("href")
                    .value(href)
                    .endObject();
        }
    }

    protected List<Field> collectVisibleFields(Form form, List<Field> fields) {
        for(FieldSet fieldSet : form) {
             collectVisibleFields(fieldSet, fields);
        }
        return fields;
    }

    protected List<Field> collectVisibleFields(FieldSet fieldSet, List<Field> fields) {
        for(FormElement element : fieldSet) {
            if(element instanceof Field) {
                Field field = (Field) element;
                if(field.isEnabled()) {
                    fields.add(field);
                }
            } else if(element instanceof FieldSet) {
                collectVisibleFields((FieldSet) element, fields);
            }
        }
        return fields;
    }

    //--------------------------------------------------------------------------
    // Accessors
    //--------------------------------------------------------------------------

    public String getReadTitle() {
        String title = crudConfiguration.getReadTitle();
        if(StringUtils.isEmpty(title)) {
            return ShortNameUtils.getName(getClassAccessor(), object);
        } else {
            OgnlTextFormat textFormat = OgnlTextFormat.create(title);
            return textFormat.format(this);
        }
    }

    public String getSearchTitle() {
        String title = crudConfiguration.getSearchTitle();
        if(StringUtils.isBlank(title)) {
            title = getPage().getTitle();
        }
        OgnlTextFormat textFormat = OgnlTextFormat.create(StringUtils.defaultString(title));
        return textFormat.format(this);
    }

    public String getEditTitle() {
        String title = crudConfiguration.getEditTitle();
        OgnlTextFormat textFormat = OgnlTextFormat.create(StringUtils.defaultString(title));
        return textFormat.format(this);
    }

    public String getCreateTitle() {
        String title = crudConfiguration.getCreateTitle();
        OgnlTextFormat textFormat = OgnlTextFormat.create(StringUtils.defaultString(title));
        return textFormat.format(this);
    }

    public CrudConfiguration getCrudConfiguration() {
        return crudConfiguration;
    }

    public void setCrudConfiguration(CrudConfiguration crudConfiguration) {
        this.crudConfiguration = crudConfiguration;
    }

    public ClassAccessor getClassAccessor() {
        return classAccessor;
    }

    public void setClassAccessor(ClassAccessor classAccessor) {
        this.classAccessor = classAccessor;
    }

    public PkHelper getPkHelper() {
        return pkHelper;
    }

    public void setPkHelper(PkHelper pkHelper) {
        this.pkHelper = pkHelper;
    }

    public List<CrudSelectionProvider> getCrudSelectionProviders() {
        return selectionProviderSupport.getCrudSelectionProviders();
    }

    public String[] getSelection() {
        return selection;
    }

    public void setSelection(String[] selection) {
        this.selection = selection;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public String getSuccessReturnUrl() {
        return successReturnUrl;
    }

    public void setSuccessReturnUrl(String successReturnUrl) {
        this.successReturnUrl = successReturnUrl;
    }

    public SearchForm getSearchForm() {
        return searchForm;
    }

    public void setSearchForm(SearchForm searchForm) {
        this.searchForm = searchForm;
    }

    public List<? extends T> getObjects() {
        return objects;
    }

    public void setObjects(List<? extends T> objects) {
        this.objects = objects;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public boolean isMultipartRequest() {
        return form != null && form.isMultipartRequest();
    }

    public List<TextField> getEditableRichTextFields() {
        List<TextField> richTextFields = new ArrayList<TextField>();
        for(FieldSet fieldSet : form) {
            for(FormElement field : fieldSet) {
                if(field instanceof TextField &&
                   ((TextField) field).isEnabled() &&
                   !form.getMode().isView(((TextField) field).isInsertable(), ((TextField) field).isUpdatable()) &&
                   ((TextField) field).isRichText()) {
                    richTextFields.add(((TextField) field));
                }
            }
        }
        return richTextFields;
    }

    public boolean isFormWithRichTextFields() {
        return !getEditableRichTextFields().isEmpty();
    }

    public Form getCrudConfigurationForm() {
        return crudConfigurationForm;
    }

    public void setCrudConfigurationForm(Form crudConfigurationForm) {
        this.crudConfigurationForm = crudConfigurationForm;
    }

    public TableForm getPropertiesTableForm() {
        return propertiesTableForm;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public TableForm getTableForm() {
        return tableForm;
    }

    public void setTableForm(TableForm tableForm) {
        this.tableForm = tableForm;
    }

    public TableForm getSelectionProvidersForm() {
        return selectionProvidersForm;
    }

    public Integer getFirstResult() {
        return firstResult;
    }

    public void setFirstResult(Integer firstResult) {
        this.firstResult = firstResult;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public String getSortProperty() {
        return sortProperty;
    }

    public void setSortProperty(String sortProperty) {
        this.sortProperty = sortProperty;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public boolean isSearchVisible() {
        //If embedded, search is always closed by default
        return searchVisible && !isEmbedded();
    }

    public void setSearchVisible(boolean searchVisible) {
        this.searchVisible = searchVisible;
    }

    public String getRelName() {
        return relName;
    }

    public void setRelName(String relName) {
        this.relName = relName;
    }

    public int getSelectionProviderIndex() {
        return selectionProviderIndex;
    }

    public void setSelectionProviderIndex(int selectionProviderIndex) {
        this.selectionProviderIndex = selectionProviderIndex;
    }

    public String getSelectFieldMode() {
        return selectFieldMode;
    }

    public void setSelectFieldMode(String selectFieldMode) {
        this.selectFieldMode = selectFieldMode;
    }

    public String getLabelSearch() {
        return labelSearch;
    }

    public void setLabelSearch(String labelSearch) {
        this.labelSearch = labelSearch;
    }

    public boolean isPopup() {
        return !StringUtils.isEmpty(popupCloseCallback);
    }

    public String getPopupCloseCallback() {
        return popupCloseCallback;
    }

    public void setPopupCloseCallback(String popupCloseCallback) {
        this.popupCloseCallback = popupCloseCallback;
    }

    public ResultSetNavigation getResultSetNavigation() {
        return resultSetNavigation;
    }

    public void setResultSetNavigation(ResultSetNavigation resultSetNavigation) {
        this.resultSetNavigation = resultSetNavigation;
    }

    public String getReturnToParentTarget() {
        return returnToParentTarget;
    }

    public Map<String, String> getReturnToParentParams() {
        return returnToParentParams;
    }

}