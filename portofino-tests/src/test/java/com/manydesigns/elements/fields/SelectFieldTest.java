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

package com.manydesigns.elements.fields;

import com.manydesigns.elements.AbstractElementsTest;
import com.manydesigns.elements.Mode;
import com.manydesigns.elements.options.DefaultSelectionProvider;
import com.manydesigns.elements.options.DisplayMode;
import com.manydesigns.elements.options.SelectionModel;
import com.manydesigns.elements.reflection.ClassAccessor;
import com.manydesigns.elements.reflection.JavaClassAccessor;
import com.manydesigns.elements.reflection.PropertyAccessor;
import com.manydesigns.elements.util.Util;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public class SelectFieldTest extends AbstractElementsTest {
    public static final String copyright =
            "Copyright (c) 2005-2013, ManyDesigns srl";

    public String myText;

    private SelectField selectField;
    private SelectField selectField2;
    private SelectField selectField3;

    private String[][] valuesArray = {
            {"value1"},
            {"value2"},
            {"value3"}
    };
    private String[][] labelsArray = {
            {"label1"},
            {"label2"},
            {"label3"}
    };

    private String[][] valuesArray2 = {
            {"value1"}
    };
    private String[][] labelsArray2 = {
            {"label1"}
    };

    protected DefaultSelectionProvider selectionProvider;
    protected SelectionModel selectionModel;

    protected DefaultSelectionProvider selectionProvider2;
    protected SelectionModel selectionModel2;

    protected DefaultSelectionProvider selectionProvider3;
    protected SelectionModel selectionModel3;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        myText = null;

        selectionProvider = new DefaultSelectionProvider("selectionProvider");
        for(int i = 0; i < valuesArray.length; i++) {
            selectionProvider.appendRow(valuesArray[i], labelsArray[i], true);
        }
        selectionModel = selectionProvider.createSelectionModel();

        selectionProvider2 = new DefaultSelectionProvider("selectionProvider");
        for(int i = 0; i < valuesArray2.length; i++) {
            selectionProvider2.appendRow(valuesArray2[i], labelsArray2[i], true);
        }
        selectionModel2 = selectionProvider2.createSelectionModel();

        selectionProvider3 = new DefaultSelectionProvider("selectionProvider");
        selectionProvider3.appendRow("value1", "label1", true);
        selectionProvider3.appendRow("value2", "label2", false);

        selectionModel3 = selectionProvider3.createSelectionModel();
    }

    private void setupSelectFields(Mode mode) {
        ClassAccessor classAccessor =
                JavaClassAccessor.getClassAccessor(this.getClass());
        PropertyAccessor myPropertyAccessor =
                null;
        try {
            myPropertyAccessor = classAccessor.getProperty("myText");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            fail();
        }

        // impostiamo selectField1
        selectField = new SelectField(myPropertyAccessor, mode, null);
        selectField.setSelectionModel(selectionModel);
        // impostiamo selectField2
        selectField2 = new SelectField(myPropertyAccessor, mode, null);
        selectField2.setSelectionModel(selectionModel2);
        // impostiamo selectField3
        selectField3 = new SelectField(myPropertyAccessor, mode, null);
        selectField3.setSelectionModel(selectionModel3);
    }

    public void testSimple() {
        setupSelectFields(Mode.EDIT);
        assertEquals(DisplayMode.DROPDOWN,
                selectField.getDisplayMode());
        assertEquals(DisplayMode.DROPDOWN,
                selectField2.getDisplayMode());

        assertNotNull(selectField.getComboLabel());
        assertEquals("-- Select my text --", selectField.getComboLabel());
        assertNull(selectField.getValue());
        assertNotNull(selectField.getErrors());
        assertEquals(0, selectField.getErrors().size());
        assertNull(selectField.getHelp());
        assertEquals("myText", selectField.getId());
        assertEquals("myText", selectField.getInputName());
        assertEquals("my text", selectField.getLabel());
        assertEquals(Mode.EDIT, selectField.getMode());
        assertFalse(selectField.isRequired());
        assertEquals(3, selectField.getOptions().size());
        assertEquals(1, selectField2.getOptions().size());
    }

    public void testSimpleRadio() {
        setupSelectFields(Mode.EDIT);
        selectField.setDisplayMode(DisplayMode.RADIO);
        assertEquals(DisplayMode.RADIO,
                selectField.getDisplayMode());
    }

    //--------------------------------------------------------------------------
    // Mode.EDIT
    //--------------------------------------------------------------------------

    public void testEditNull() {
        setupSelectFields(Mode.EDIT);
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);
    }

    public void testEditNullRadio() {
        setupSelectFields(Mode.EDIT);
        selectField.setDisplayMode(DisplayMode.RADIO);

        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td><fieldset id=\"myText\" class=\"radio\">" +
                "<input type=\"radio\" id=\"myText_0\" name=\"myText\" value=\"\" checked=\"checked\" />&nbsp;<label for=\"myText_0\">None</label><br />" +
                "<input type=\"radio\" id=\"myText_1\" name=\"myText\" value=\"value1\" />&nbsp;<label for=\"myText_1\">label1</label><br />" +
                "<input type=\"radio\" id=\"myText_2\" name=\"myText\" value=\"value2\" />&nbsp;<label for=\"myText_2\">label2</label><br />" +
                "<input type=\"radio\" id=\"myText_3\" name=\"myText\" value=\"value3\" />&nbsp;<label for=\"myText_3\">label3</label><br />" +
                "</fieldset></td>", text);
    }

    public void testEditNullRequired() {
        setupSelectFields(Mode.EDIT);

        selectField.setRequired(true);
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">" +
                "<span class=\"required\">*</span>&nbsp;My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);

        assertFalse(selectField.validate());
        text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">" +
                "<span class=\"required\">*</span>&nbsp;My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select><ul class=\"errors\">" +
                "<li>Required field" +
                "</li></ul></td>", text);
    }

    public void testEditNullRadioRequired() {
        setupSelectFields(Mode.EDIT);
        selectField.setDisplayMode(DisplayMode.RADIO);
                
        selectField.setRequired(true);
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\"><span class=\"required\">*</span>&nbsp;My text:" +
                "</label></th><td><fieldset id=\"myText\" class=\"radio\">" +
                "<input type=\"radio\" id=\"myText_0\" name=\"myText\" value=\"value1\" />&nbsp;<label for=\"myText_0\">label1</label><br />" +
                "<input type=\"radio\" id=\"myText_1\" name=\"myText\" value=\"value2\" />&nbsp;<label for=\"myText_1\">label2</label><br />" +
                "<input type=\"radio\" id=\"myText_2\" name=\"myText\" value=\"value3\" />&nbsp;<label for=\"myText_2\">label3</label><br />" +
                "</fieldset></td>", text);
    }

    public void testEditNullWithComboLabel() {
        setupSelectFields(Mode.EDIT);

        selectField.setComboLabel("-- Scegli opzione --");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Scegli opzione --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);
        assertEquals("-- Scegli opzione --", selectField.getComboLabel());
    }

    public void testEditValidSelection() {
        setupSelectFields(Mode.EDIT);

        selectField.setValue("value1");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\">-- Select my text --</option>" +
                "<option value=\"value1\" selected=\"selected\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);
    }

    public void testEditInactiveSelection() {
        setupSelectFields(Mode.EDIT);

        selectField3.setValue("value1");
        String text = Util.elementToString(selectField3);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\">-- Select my text --</option>" +
                "<option value=\"value1\" selected=\"selected\">label1</option>" +
                "</select></td>", text);

        selectionProvider3.ensureActive("value2");
        selectField3.setValue("value2");
        text = Util.elementToString(selectField3);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\" selected=\"selected\">label2</option>" +
                "</select></td>", text);
    }

    public void testEditInvalidSelection() {
        setupSelectFields(Mode.EDIT);

        selectField.setValue("value4");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "<option value=\"value2\">label2</option>" +
                "<option value=\"value3\">label3</option>" +
                "</select></td>", text);
    }

    //--------------------------------------------------------------------------
    // Mode.VIEW
    //--------------------------------------------------------------------------

    public void testViewNull() {
        setupSelectFields(Mode.VIEW);

        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\"></div>" +
                "</td>", text);
    }

    public void testViewValidSelection() {
        setupSelectFields(Mode.VIEW);

        selectField.setValue("value1");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label1</div>" +
                "</td>", text);
    }

    public void testViewValidSelectionNoUrl() {
        setupSelectFields(Mode.VIEW);

        selectField.setValue("value3");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label3</div>" +
                "</td>", text);
    }

    public void testViewInactiveSelection() {
        setupSelectFields(Mode.VIEW);

        selectionProvider3.ensureActive("value2");
        selectField3.setValue("value2");
        String text = Util.elementToString(selectField3);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label2</div>" +
                "</td>", text);
    }

    public void testViewInvalidSelection() {
        setupSelectFields(Mode.VIEW);

        selectField.setValue("value4");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\"></div>" +
                "</td>", text);
    }

    //--------------------------------------------------------------------------
    // Mode.PREVIEW
    //--------------------------------------------------------------------------

    public void testPreviewNull() {
        setupSelectFields(Mode.PREVIEW);

        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\"></div>" +
                "<input type=\"hidden\" id=\"myText\" name=\"myText\" />" +
                "</td>", text);
    }

    public void testPreviewValidSelection() {
        setupSelectFields(Mode.PREVIEW);

        selectField.setValue("value1");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label1</div>" +
                "<input type=\"hidden\" id=\"myText\" name=\"myText\" value=\"value1\" />" +
                "</td>", text);
    }

    public void testPreviewValidSelectionNoUrl() {
        setupSelectFields(Mode.PREVIEW);

        selectField.setValue("value3");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\">label3</div>" +
                "<input type=\"hidden\" id=\"myText\" name=\"myText\" value=\"value3\" />" +
                "</td>", text);
    }

    public void testPreviewInvalidSelection() {
        setupSelectFields(Mode.PREVIEW);

        selectField.setValue("value4");
        String text = Util.elementToString(selectField);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">My text:" +
                "</label></th><td>" +
                "<div class=\"value\" id=\"myText\"></div>" +
                "<input type=\"hidden\" id=\"myText\" name=\"myText\" />" +
                "</td>", text);
    }


    //--------------------------------------------------------------------------
    // Mode.HIDDEN
    //--------------------------------------------------------------------------

    public void testHiddenNull() {
        setupSelectFields(Mode.HIDDEN);

        String text = Util.elementToString(selectField);
        assertEquals("<input type=\"hidden\" id=\"myText\" name=\"myText\" />", text);
    }

    public void testHiddenValidSelection() {
        setupSelectFields(Mode.HIDDEN);

        selectField.setValue("value1");
        String text = Util.elementToString(selectField);
        assertEquals("<input type=\"hidden\" id=\"myText\" name=\"myText\" value=\"value1\" />", text);
    }

    public void testHiddenValidSelectionNoUrl() {
        setupSelectFields(Mode.HIDDEN);

        selectField.setValue("value3");
        String text = Util.elementToString(selectField);
        assertEquals("<input type=\"hidden\" id=\"myText\" name=\"myText\" value=\"value3\" />", text);
    }

    public void testHiddenInvalidSelection() {
        setupSelectFields(Mode.HIDDEN);

        selectField.setValue("value4");
        String text = Util.elementToString(selectField);
        assertEquals("<input type=\"hidden\" id=\"myText\" name=\"myText\" />", text);
    }

    //--------------------------------------------------------------------------
    // metodi di Element
    //--------------------------------------------------------------------------

    public void testReadFromRequestWrongValue() {
        setupSelectFields(Mode.EDIT);

        assertFalse(selectField.isRequired());

        req.setParameter("myText", "wrongValue");
        assertNull(selectField.getValue());
        selectField.readFromRequest(req);
        assertNull(selectField.getValue());
    }

    public void testReadFromRequest() {
        setupSelectFields(Mode.EDIT);

        assertFalse(selectField.isRequired());

        req.setParameter("myText", "value1");
        assertNull(selectField.getValue());
        selectField.readFromRequest(req);
        assertEquals("value1", selectField.getValue());
    }

    public void testReadFromRequestRequired() {
        setupSelectFields(Mode.EDIT);

        assertEquals(1, selectField2.getOptions().size());
        selectField2.setRequired(true);

        assertNull(selectField2.getValue());
        selectField2.readFromRequest(req);
        String text = Util.elementToString(selectField2);
        assertEquals("<th><label for=\"myText\" class=\"mde-field-label\">" +
                "<span class=\"required\">*</span>&nbsp;My text:" +
                "</label></th><td><select id=\"myText\" name=\"myText\">" +
                "<option value=\"\" selected=\"selected\">-- Select my text --</option>" +
                "<option value=\"value1\">label1</option>" +
                "</select></td>", text);
    }

    public void testReadFromRequestNotRequired() {
        setupSelectFields(Mode.EDIT);

        assertFalse(selectField2.isRequired());
        assertEquals(1, selectField2.getOptions().size());

        assertNull(selectField2.getValue());
        selectField2.readFromRequest(req);
        assertNull(selectField2.getValue());
    }

    public void testReadFromObject() {
        setupSelectFields(Mode.EDIT);

        assertNull(selectField.getValue());
        myText = "value2";
        selectField.readFromObject(this);
        assertEquals("value2", selectField.getValue());
    }

    /**
     * Questo test verifica un bug:
     * Se un SelectField è stato valorizzato tramite una readFromObject(),
     * una lettura tramite readFromRequest(req) dove req NON contiene
     * il parametro (req.getParameter(inputName) == null)
     * deve lasciare il valore inalterato
     */
    public void testReadFromObject2() {
        setupSelectFields(Mode.EDIT);

        assertNull(selectField.getValue());
        myText = "value2";
        selectField.readFromObject(this);
        assertEquals("value2", selectField.getValue());

        // leggiamo dalla request vuota (senza parametri)
        selectField.readFromRequest(req);

        // verifichiamo che abbia tenuto il valore
        assertEquals("value2", selectField.getValue());
    }

    public void testWriteToObject() {
        setupSelectFields(Mode.EDIT);

        assertNull(myText);
        selectField.setValue("value2");
        selectField.writeToObject(this);
        assertEquals("value2", myText);
    }
}