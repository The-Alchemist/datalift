package org.datalift.fwk.util.web.json;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriter;


public class SparqlResultsJsonWriter extends AbstractJsonWriter
                                     implements TupleQueryResultWriter
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public SparqlResultsJsonWriter(OutputStream out) {
        this(out, null, null, null);
    }

    public SparqlResultsJsonWriter(OutputStream out, String urlPattern,
                                String defaultGraphUri, String jsonCallback) {
        super(out, urlPattern, defaultGraphUri, jsonCallback);
    }

    public SparqlResultsJsonWriter(Writer out) {
        this(out, null, null, null);
    }

    public SparqlResultsJsonWriter(Writer out, String urlPattern,
                                String defaultGraphUri, String jsonCallback) {
        super(out, urlPattern, defaultGraphUri, jsonCallback);
    }

    //-------------------------------------------------------------------------
    // TupleQueryResultWriter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final TupleQueryResultFormat getTupleQueryResultFormat() {
        return TupleQueryResultFormat.JSON;
    }

    /** {@inheritDoc} */
    @Override
    public void startQueryResult(List<String> columnHeaders)
                                    throws TupleQueryResultHandlerException {
        try {
            this.start(columnHeaders);
            this.openBraces();            
            // Write header
            this.writeKey("head");
            this.openBraces();
            this.writeKeyValue("vars", columnHeaders);
            this.closeBraces();
            this.writeComma();
            // Write results
            this.writeKey("results");
            this.openBraces();
            this.writeKey("bindings");
            this.openArray();
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
            this.startSolution();       // start of new solution

            for (Iterator<Binding> i=bindingSet.iterator(); i.hasNext(); ) {
                Binding b = i.next();
                this.writeKeyValue(b.getName(), b.getValue(),
                                                ResourceType.Unknown);
                if (i.hasNext()) {
                    this.writeComma();
                }
            }
            this.endSolution();         // end solution
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endQueryResult() throws TupleQueryResultHandlerException {
        try {
            this.closeArray();          // bindings array
            this.closeBraces();         // results braces
            this.closeBraces();         // root braces
            this.end();
        }
        catch (IOException e) {
            throw new TupleQueryResultHandlerException(e);
        }
    }

    @Override
    protected void writeValue(Value value, ResourceType type)
                                                            throws IOException {
        this.openBraces();

        if (value instanceof URI) {
                this.writeKeyValue("type", "uri");
                this.writeComma();
                this.writeKeyValue("value", ((URI)value).toString());
        }
        else if (value instanceof BNode) {
                this.writeKeyValue("type", "bnode");
                this.writeComma();
                this.writeKeyValue("value", ((BNode)value).getID());
        }
        else if (value instanceof Literal) {
                Literal l = (Literal)value;
                if (l.getDatatype() != null) {
                        this.writeKeyValue("type", "typed-literal");
                        this.writeComma();
                        this.writeKeyValue("datatype",
                                           l.getDatatype().toString());
                }
                else {
                        this.writeKeyValue("type", "literal");
                        if (l.getLanguage() != null) {
                                this.writeComma();
                                this.writeKeyValue("xml:lang", l.getLanguage());
                        }
                }
                this.writeComma();
                this.writeKeyValue("value", l.getLabel());
        }
        else {
            throw new IOException(
                            "Unknown Value object type: " + value.getClass());
        }
        this.closeBraces();
    }
}