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

package org.datalift.fwk.rdf.json;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;
import org.openrdf.rio.RioSetting;
import org.openrdf.rio.WriterConfig;

import static org.openrdf.query.resultio.BasicQueryWriterSettings.JSONP_CALLBACK;


/**
 * An implementation of {@link TupleQueryResultWriter} that serializes
 * SPARQL query results into a compact JSON syntax, suitable for
 * directly filling HTML tables with minimum client-side processing.
 *
 * @author hdevos
 */
public class SparqlResultsGridJsonWriter extends AbstractGridJsonWriter
                                         implements TupleQueryResultWriter
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private WriterConfig writerConfig = new WriterConfig();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out   the byte stream to write JSON text to.
     */
    public SparqlResultsGridJsonWriter(OutputStream out) {
        this(out, null);
    }

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out            the byte stream to write JSON text to.
     * @param  urlPattern     a message format to replace RDF resource
     *                        URIs with HTML links
     *                        (<code>&lt;a href=.../&gt;</code>).
     */
    public SparqlResultsGridJsonWriter(OutputStream out,
                                       MessageFormat urlPattern) {
        super(out, urlPattern, null);
    }

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out   the character stream to write JSON text to.
     */
    public SparqlResultsGridJsonWriter(Writer out) {
        this(out, null);
    }

    /**
     * Create a new compact grid-oriented RDF JSON serializer.
     * @param  out            the character stream to write JSON text to.
     * @param  urlPattern     a message format to replace RDF resource
     *                        URIs with HTML links
     *                        (<code>&lt;a href=.../&gt;</code>).
     */
    public SparqlResultsGridJsonWriter(Writer out, MessageFormat urlPattern) {
        super(out, urlPattern, null);
    }

    //-------------------------------------------------------------------------
    // TupleQueryResultWriter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final TupleQueryResultFormat getTupleQueryResultFormat() {
        return TupleQueryResultFormat.JSON;
    }

    //-------------------------------------------------------------------------
    // QueryResultHandler contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startQueryResult(List<String> bindingNames)
                                    throws TupleQueryResultHandlerException {
        if (this.getWriterConfig().isSet(JSONP_CALLBACK)) {
            this.setJsonCallback(this.getWriterConfig().get(JSONP_CALLBACK));
        }
        try {
            this.startDocument(bindingNames);
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleSolution(BindingSet bindingSet)
                                    throws TupleQueryResultHandlerException {
        try {
            this.write(bindingSet);
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleBoolean(boolean value)
                                        throws QueryResultHandlerException {
        throw new QueryResultHandlerException(
                                        new UnsupportedOperationException());
    }

    /** {@inheritDoc} */
    @Override
    public void handleLinks(List<String> linkUrls)
                    throws QueryResultHandlerException {
        // TODO: Add support for JSON links.
    }

    /** {@inheritDoc} */
    @Override
    public void endQueryResult() throws TupleQueryResultHandlerException {
        try {
            this.endDocument();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    //-------------------------------------------------------------------------
    // QueryResultWriter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public TupleQueryResultFormat getQueryResultFormat() {
        return getTupleQueryResultFormat();
    }

    /** {@inheritDoc} */
    @Override
    public void handleNamespace(String prefix, String uri)
                                        throws QueryResultHandlerException {
        this.setPrefix(prefix, uri);
    }

    @Override
    public void startDocument() throws QueryResultHandlerException {
        // NOP. See #startQueryResult(List<String>)
    }

    /** {@inheritDoc} */
    @Override
    public void handleStylesheet(String stylesheetUrl)
                                        throws QueryResultHandlerException {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void startHeader() throws QueryResultHandlerException {
        // NOP. See #startQueryResult(List<String>)
    }

    /** {@inheritDoc} */
    @Override
    public void endHeader() throws QueryResultHandlerException {
        // NOP. See #startQueryResult(List<String>)
    }

    @Override
    public Collection<RioSetting<?>> getSupportedSettings() {
        Set<RioSetting<?>> settings = new HashSet<RioSetting<?>>();
        settings.add(JSONP_CALLBACK);
        return settings;
    }

    @Override
    public WriterConfig getWriterConfig() {
        return this.writerConfig;
    }

    @Override
    public void setWriterConfig(WriterConfig config) {
        this.writerConfig = config;
    }
}
