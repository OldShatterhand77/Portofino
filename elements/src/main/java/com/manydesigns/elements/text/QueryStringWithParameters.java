/*
 * Copyright (C) 2005-2012 ManyDesigns srl.  All rights reserved.
 * http://www.manydesigns.com/
 *
 * Unless you have purchased a commercial license agreement from ManyDesigns srl,
 * the following license terms apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.manydesigns.elements.text;

import java.io.Serializable;
import java.util.Arrays;

/*
* @author Paolo Predonzani     - paolo.predonzani@manydesigns.com
* @author Angelo Lupo          - angelo.lupo@manydesigns.com
* @author Giampiero Granatella - giampiero.granatella@manydesigns.com
* @author Alessio Stalla       - alessio.stalla@manydesigns.com
*/
public class QueryStringWithParameters implements Serializable {
    public static final String copyright =
            "Copyright (c) 2005-2012, ManyDesigns srl";

    protected final String queryString;
    protected final Object[] paramaters;

    public QueryStringWithParameters(String queryString, Object[] paramaters) {
        this.queryString = queryString;
        this.paramaters = paramaters;
    }

    public String getQueryString() {
        return queryString;
    }

    public Object[] getParameters() {
        return paramaters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryStringWithParameters that = (QueryStringWithParameters) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(paramaters, that.paramaters)) return false;
        if (queryString != null ? !queryString.equals(that.queryString) : that.queryString != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = queryString != null ? queryString.hashCode() : 0;
        result = 31 * result + (paramaters != null ? Arrays.hashCode(paramaters) : 0);
        return result;
    }
}
