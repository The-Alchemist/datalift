/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.fwk.project;


import java.net.URI;
import java.net.URISyntaxException;

import org.datalift.fwk.Module;
import org.datalift.fwk.util.web.MenuEntry;


/**
 * A project module is a specific type of module that participates in
 * the data lifting process by providing an interface allowing the user
 * to interact with the data transformation process.
 *
 * @author lbihanic
 */
public interface ProjectModule extends Module
{
    // ------------------------------------------------------------------------
    // ProjectModule contract definition
    // ------------------------------------------------------------------------

    /**
     * Returns whether this module applies to the specified project,
     * i.e. the project current state allows data manipulation (e.g.
     * transformation, conversion, data completion...) by this module.
     * @param  p   a data-lifting project.
     *
     * @return the description of the module entry page applicable to
     *         the specified project or <code>null</code> if this
     *         module can not handle the project in its current state.
     */
    public abstract UriDesc canHandle(Project p);

    // ------------------------------------------------------------------------
    // UriDesc nested class
    // ------------------------------------------------------------------------

    /**
     * A description of the access to a module page.
     */
    public class UriDesc extends MenuEntry
    {
        // --------------------------------------------------------------------
        // Instance members
        // --------------------------------------------------------------------

        private final URI uri;
        private final HttpMethod method;
        private final String label;
        private URI icon;
        private int position = 5000;

        // --------------------------------------------------------------------
        // Constructors
        // --------------------------------------------------------------------

        /**
         * Creates the description of a URI accessible using the HTTP
         * {@link HttpMethod#GET} method.
         * @param  uri     the page (relative) URI as a string.
         * @param  label   the page description to display to the user.
         *
         * @throws URISyntaxException if the specified URI is not valid.
         *
         * @see    #UriDesc(URI, HttpMethod, String)
         */
        public UriDesc(String uri, String label) throws URISyntaxException {
            this(new URI(uri), HttpMethod.GET, label);
        }

        /**
         * Creates the description of a URI accessible using the HTTP
         * {@link HttpMethod#GET} method.
         * @param  uri     the page (relative) URI.
         * @param  label   the page description to display to the user.
         *
         * @see    #UriDesc(URI, HttpMethod, String)
         */
        public UriDesc(URI uri, String label) {
            this(uri, HttpMethod.GET, label);
        }

        /**
         * Creates the description of a URI accessible using the
         * specified HTTP method.
         * @param  uri      the page (relative) URI as a string.
         * @param  method   the HTTP method to access the URI.
         * @param  label    the page description to display to the user.
         *
         * @throws URISyntaxException if the specified URI is not valid.
         *
         * @see    #UriDesc(URI, HttpMethod, String)
         */
        public UriDesc(String uri, HttpMethod method, String label)
                                                    throws URISyntaxException {
            this(new URI(uri), method, label);
        }

        /**
         * Creates the description of a URI accessible using the
         * specified HTTP method.
         * @param  uri      the page (relative) URI as a string.
         * @param  method   the HTTP method to access the URI.
         * @param  label    the page description to display to the user.
         */
        public UriDesc(URI uri, HttpMethod method, String label) {
            super();
            if (uri == null) {
                throw new IllegalArgumentException("uri");
            }
            if (method == null) {
                throw new IllegalArgumentException("method");
            }
            if ((label == null) || (label.length() == 0)) {
                throw new IllegalArgumentException("label");
            }
            this.uri = uri;
            this.method = method;
            this.label = label;
        }

        // --------------------------------------------------------------------
        // MenuEntry contract support
        // --------------------------------------------------------------------

        /** {@inheritDoc} */
        @Override
        public URI getUri() {
            return this.uri;
        }

        /** {@inheritDoc} */
        @Override
        public HttpMethod getMethod() {
            return this.method;
        }

        /** {@inheritDoc} */
        @Override
        public String getLabel() {
            return this.label;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Default value is 5000.</p>
         */
        @Override
        public int getPosition() {
            return this.position;
        }

        /** {@inheritDoc} */
        @Override
        public URI getIcon() {
            return this.icon;
        }

        /**
         * {@inheritDoc}
         * @return <code>true</code> always, as access control shall
         *         have been performed by
         *         {@link ProjectModule#canHandle(Project)}.
         */
        @Override
        public boolean isAccessible() {
            return true;
        }

        // --------------------------------------------------------------------
        // Specific implementation
        // --------------------------------------------------------------------

        /**
         * Sets the position where the module page shall appear in
         * the list of modules applicable to a project.
         * <p>
         * There's no hard-coded rule for the position value. Yet the
         * following informal rules are recommended:</p>
         * <ul>
         *  <li>From 1 to 999 for modules transforming non RDF data
         *   into RDF data</li>
         *  <li>From 1000 to 9999 for modules transforming RDF data
         *   (ontology alignment, interlinking...)</li>
         *  <li>Above 10000 for modules publishing or exporting RDF
         *   data</li>
         * </ul>
         * @param  position   the position as a positive integer.
         */
        public void setPosition(int position) {
            if (position < 0) {
                throw new IllegalArgumentException("position ("
                                    + position + ") shall not be negative");
            }
            this.position = position;
        }

        /**
         * <i>Reserved for future use</i>.
         * @param  icon   the module icon or <code>null</code>.
         */
        public void setIcon(URI icon) {
            this.icon = icon;
        }
    }
}
