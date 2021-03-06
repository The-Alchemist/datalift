package org.datalift.owl.mapper;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.IF_MODIFIED_SINCE;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.datalift.fwk.MediaTypes.APPLICATION_JSON_UTF8;
import static org.datalift.fwk.MediaTypes.TEXT_HTML_UTF8;
import static org.datalift.fwk.project.Source.SourceType.TransformedRdfSource;
import static org.datalift.fwk.util.StringUtils.isSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.RdfFormat;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.io.FileUtils;
import org.datalift.fwk.util.web.Charsets;
import org.datalift.fwk.util.web.HttpDateFormat;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;
import org.datalift.owl.DatatypeProperty;
import org.datalift.owl.ObjectProperty;
import org.datalift.owl.Ontology;
import org.datalift.owl.OwlClass;
import org.datalift.owl.OwlObject;
import org.datalift.owl.OwlParser;
import org.datalift.owl.OwlProperty;
import org.datalift.owl.TechnicalException;
import org.openrdf.model.URI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


@Path(Data2Ontology.MODULE_NAME)
public class Data2Ontology extends BaseModule implements ProjectModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "data2ontology";
    /** The module position in the project view. */
    private final static int MODULE_POSITION = 1500;
    /** Base name of the resource bundle for converter GUI. */
    private final static String GUI_RESOURCES_BUNDLE = "resources";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The DataLift project manager. */
    protected ProjectManager projectManager = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public Data2Ontology() {
        super(MODULE_NAME);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        super.postInit(configuration);

        this.projectManager = configuration.getBean(ProjectManager.class);
        if (this.projectManager == null) {
            throw new TechnicalException("project.manager.not.available");
        }
    }

    //-------------------------------------------------------------------------
    // ProjectModule contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public UriDesc canHandle(Project p) {
        UriDesc projectPage = null;
        try {
            if ((this.findSource(p, false) != null)) {
                try {
                    String label = PreferredLocales.get()
                                .getBundle(GUI_RESOURCES_BUNDLE, this)
                                .getString("ontology.mapper.button");

                    projectPage = new UriDesc(
                            this.getName() + "?project=" + p.getUri(), label);
                    projectPage.setPosition(MODULE_POSITION);
                }
                catch (Exception e) {
                    throw new TechnicalException(e);
                }
            }
        }
        catch (Exception e) {
            log.fatal("Failed to check status of project {}: {}", e,
                                                p.getUri(), e.getMessage());
        }
        return projectPage;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam("project") java.net.URI projectId) {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Display conversion configuration page.
            TemplateModel view = this.newView("data2ontology.vm", p);
            view.put("srcType", TransformedRdfSource);
            response = Response.ok(view, TEXT_HTML_UTF8).build();
        }
        catch (IllegalArgumentException e) {
            TechnicalException error =
                            new TechnicalException("ws.invalid.param.error",
                                                   "project", projectId);
            this.sendError(BAD_REQUEST, error.getLocalizedMessage());
        }
        return response;
    }

    @GET
    @Path("ontology")
    @Produces(APPLICATION_JSON)
    public Response parseOntology(@QueryParam("src") String src) {
        if (! isSet(src)) {
            this.throwInvalidParamError("src", null);
        }
        Response response = null;
        try {
            URL u = new URL(src);
            String path = u.getPath();
            if (path.charAt(0) == '/') {
                path = path.substring(1);
            }
            File f = new File(Configuration.getDefault().getTempStorage(),
                              MODULE_NAME + '/' + path);
            long now = System.currentTimeMillis();

            if ((! f.exists()) || (f.lastModified() < now)) {
                // Compute HTTP Accept header.
                Map<String,String> headers = new HashMap<String,String>();
                headers.put(ACCEPT, this.getRdfAcceptHeader());
                if (f.exists()) {
                    headers.put(IF_MODIFIED_SINCE,
                                HttpDateFormat.formatDate(f.lastModified()));
                }
                // Make sure parent directories exist.
                f.getParentFile().mkdirs();
                // Retrieve file from source URL.
                log.trace("--- info url ---: {}, file: {}, headers: {}", u, f, headers);

                FileUtils.DownloadInfo info = FileUtils.save(u, null, headers, f);
                log.trace("--- info ---: {}", info);
                f.setLastModified((info.expires > 0L)? info.expires: now);
                // Extract RDF format from MIME type to force file suffix.
                RdfFormat fmt = RdfFormat.find(info.mimeType);
                if (fmt == null) {
                    // We probably got it from LOV (but application/octet-stream)
                    fmt = RdfUtils.guessRdfFormatFromExtension(src);
                }
                if (fmt == null) {
                    throw new TechnicalException("invalid.remote.mime.type",
                                                 info.mimeType);
                }
                // Ensure file extension is present to allow RDF syntax
                // detection in future cache accesses.
                String ext = "." + fmt.getFileExtension();
                if (! f.getName().endsWith(ext)) {
                    File newFile = new File(f.getCanonicalPath() + ext);
                    f.renameTo(newFile);
                    f = newFile;
                }
                // Mark file as to be deleted upon JVM termination.
                f.deleteOnExit();
            }
            // Parse ontology.
            log.info("-- parsing ontology {} with path : {}--", f.getName(), f.getPath());
            Ontology o = new OwlParser().parse(f, src);
            // Return JSON representation of OWL ontology.
            response = Response.ok(new OntologyJsonStreamingOutput(o),
                                   APPLICATION_JSON_UTF8).build();
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    @Path("execute")
    public Response executeConstruct(
                                @FormParam("project") java.net.URI project,
                                @FormParam("sourceGraph") java.net.URI sourceGraph,
                                @FormParam("targetName") String targetName,
                                @FormParam("targetGraph") String targetGraph,
                                @FormParam("query") String query)
                                                throws WebApplicationException {
        Response response = null;
        try {
            boolean createSource = true;

            // Retrieve project and input source (to check they exist!).
            String srcId = sourceGraph.toString();
            Project p = null;
            Source in = null;
            try {
                p = this.getProject(project);
                in = (TransformedRdfSource)(p.getSource(srcId));
            }
            catch (Exception e) {
                this.throwInvalidParamError("sourceGraph", srcId);
            }
            // Execute SPARQL Construct queries.
            java.net.URI ctx = java.net.URI.create(targetGraph.toString());
            Repository internal = Configuration.getDefault()
                                               .getInternalRepository();
            String construct = query;
            log.debug("Applying mapping from {} to {}, query:\n{}",
                                                sourceGraph, ctx, construct);
            RdfUtils.convert(internal, Arrays.asList(construct),
                             internal, ctx, createSource);
            if (createSource) {
                // Register new transformed RDF source.
                this.addResultSource(p, in, targetName, java.net.URI.create(targetGraph));
            }
            response = Response.ok("{"
                + "\"uri\": \"" + targetGraph + "\", "
                + "\"title\": \"" + targetName + "\""
                + "}", APPLICATION_JSON_UTF8).build();
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

    /**
     * Traps accesses to module static resources and redirect them
     * toward the default {@link ResourceResolver} for resolution.
     * @param  path        the relative path of the module static
     *                     resource being accessed.
     * @param  uriInfo     the request URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a {@link Response JAX-RS response} to download the
     *         content of the specified public resource.
     */
    @GET
    @Path("static/{path: .*$}")
    public Object getStaticResource(@PathParam("path") String path,
                                    @Context UriInfo uriInfo,
                                    @Context Request request,
                                    @HeaderParam(ACCEPT) String acceptHdr) {
        return Configuration.getDefault()
                            .getBean(ResourceResolver.class)
                            .resolveModuleResource(this.getName(),
                                                   uriInfo, request, acceptHdr);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Retrieves a {@link Project} using its URI.
     * @param  projectId   the project URI.
     *
     * @return the project.
     * @throws TechnicalException if the project does not exist.
     */
    private final Project getProject(java.net.URI projectId) {
        Project p = this.projectManager.findProject(projectId);
        if (p == null) {
            throw new IllegalArgumentException(projectId.toString());
        }
        return p;
    }

    /**
     * Searches the specified project for a source matching the expected
     * input source type of the module.
     * @param  p          the project.
     * @param  findLast   whether to return the last matching source or
     *                    the first.
     * @return a matching source of <code>null</code> if no matching
     *         source was found.
     */
    private Source findSource(Project p, boolean findLast) {
        if (p == null) {
            throw new IllegalArgumentException("p");
        }
        Source src = null;
        for (Source s : p.getSources()) {
            if (s.getType() == TransformedRdfSource) {
                src = s;
                if (! findLast) break;
                // Else: continue to get last source of type in project...
            }
        }
        return src;
    }

    private TransformedRdfSource addResultSource(Project p, Source parent,
                                                 String name, java.net.URI uri)
                                                                 throws IOException {
        java.net.URI id = java.net.URI.create(uri.toString());
        TransformedRdfSource newSrc =
                    this.projectManager.newTransformedRdfSource(p, id,
                                                        name, null, id, parent);
        this.projectManager.saveProject(p);
        return newSrc;
    }

    private String getRdfAcceptHeader() {
        StringBuilder buf = new StringBuilder();
        for (RdfFormat fmt : RdfFormat.values()) {
            boolean first = true;
            for (MediaType m : fmt.mimeTypes) {
                if (first) {
                    // Preferred MIME type (q=1.0).
                    if (buf.length() != 0) {
                        buf.append(", ");
                    }
                    buf.append(m);
                    first = false;
                }
                else {
                    // Secondary MIME types.
                    buf.append(", ").append(m).append("; q=0.5");
                }
            }
        }
        return buf.toString();
    }

    /**
     * Return a model for the specified template view, populated with
     * the specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated template model.
     */
    private TemplateModel newView(String templateName, Object it) {
        return ViewFactory.newView(
                                "/" + this.getName() + '/' + templateName, it);
    }

    /**
     * Throws a {@link WebApplicationException} with a HTTP status set
     * to 400 (Bad request) to signal an invalid or missing web service
     * parameter.
     * @param  name    the parameter name in the web service interface.
     * @param  value   the invalid parameter value or <code>null</code>
     *                 if the parameter was absent.
     *
     * @throws WebApplicationException always.
     */
    private void throwInvalidParamError(String name, Object value)
                                                throws WebApplicationException {
        TechnicalException error = (value != null)?
                new TechnicalException("ws.invalid.param.error", name, value):
                new TechnicalException("ws.missing.param", name);
        this.sendError(BAD_REQUEST, error.getLocalizedMessage());
    }

    /**
     * Logs and map an internal processing error onto HTTP status codes.
     * @param  e   the error to map.
     *
     * @throws WebApplicationException always.
     */
    private void handleInternalError(Exception e)
                                                throws WebApplicationException {
        TechnicalException error = null;
        if (e instanceof WebApplicationException) {
            throw (WebApplicationException)e;
        }
        else if (e instanceof FileNotFoundException) {
            this.sendError(NOT_FOUND, e.getLocalizedMessage());
        }
        else if (e instanceof TechnicalException) {
            error = (TechnicalException)e;
        }
        else {
            error = new TechnicalException(
                            "ws.internal.error", e, e.getLocalizedMessage());
        }
        log.fatal(e.getMessage(), e);
        this.sendError(INTERNAL_SERVER_ERROR, error.getLocalizedMessage());
    }

    //-------------------------------------------------------------------------
    // Java representations of JSON request data
    //-------------------------------------------------------------------------

    public final static class OntologyDesc
    {
        public String name;
        public String uri;
    }

    public final static class MappingDesc
    {
        public String name;
        public String predicate;
        public String value;
        public List<String> types = new LinkedList<String>();
        public Collection<MappingDesc> children = new LinkedList<MappingDesc>();
    }

    //-------------------------------------------------------------------------
    // JSON result serialization handling nested classes
    //-------------------------------------------------------------------------

    /**
     * A {@link StreamingOutput} implementation that serializes an
     * {@link Ontology} object into JSON.
     */
    private final static class OntologyJsonStreamingOutput
                                                    implements StreamingOutput
    {
        private final Ontology ontology;

        public OntologyJsonStreamingOutput(Ontology ontology) {
            this.ontology = ontology;
        }

        /** {@inheritDoc} */
        @Override
        public void write(OutputStream output)
                                throws IOException, WebApplicationException {
            OwlSerializer owlSerializer = new OwlSerializer();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(OwlClass.class, owlSerializer)
                    .registerTypeAdapter(OwlProperty.class, owlSerializer)
                    .registerTypeAdapter(ObjectProperty.class, owlSerializer)
                    .registerTypeAdapter(DatatypeProperty.class, owlSerializer)
                    .registerTypeAdapter(java.net.URI.class, new UriSerializer())
                    .setPrettyPrinting().create();
            Writer w = new OutputStreamWriter(output, Charsets.UTF_8);
            gson.toJson(this.ontology, w);
            w.flush();
        }
    }

    /**
     * A {@link JsonSerializer} to serialize {@link OwlObject}s.
     */
    private final static class OwlSerializer implements JsonSerializer<OwlObject>
    {
        private boolean inOwl = false;

        public OwlSerializer() {
            super();
        }

        public JsonElement serialize(OwlObject src, Type type,
                                     JsonSerializationContext context) {
            JsonElement e = null;
            if (inOwl) {
                e = new JsonPrimitive(src.uri());
            }
            else {
                inOwl = true;
                if (src instanceof OwlClass) {
                    e = this.serialize((OwlClass)src);
                }
                else if (src instanceof OwlProperty) {
                    e = this.serialize((OwlProperty)src);
                }
                else {
                    e = this.serialize(src);
                }
                inOwl = false;
            }
            return e;
        }

        private JsonObject serialize(OwlObject o) {
            JsonObject e = new JsonObject();
            // e.addProperty("uri", src.uri);
            e.addProperty("type", o.getClass().getSimpleName());
            e.addProperty("name", o.name);
            if (isSet(o.desc)) {
                e.addProperty("desc", o.desc);
            }
            return e;
        }

        private JsonObject serialize(OwlClass c) {
            JsonObject e = this.serialize((OwlObject)c);
            this.add(c.parents(), "parents", e);
            this.add(c.subclasses(), "subclasses", e);
            this.add(c.disjoints(), "disjoints", e);
            this.add(c.properties(true), "properties", e);
            if (c.union()) {
                e.addProperty("union", Boolean.TRUE);
            }
            return e;
        }

        private JsonObject serialize(OwlProperty p) {
            JsonObject e = this.serialize((OwlObject)p);
            Collection<OwlClass> ranges = p.ranges();
            if (! ranges.isEmpty()) {
                this.add(p.ranges(),  "ranges", e);
            }
            else {
                if (p.type() != null) {
                    JsonArray x = new JsonArray();
                    x.add(new JsonPrimitive(p.type().stringValue()));
                    e.add("ranges", x);
                }
            }
            this.add(p.domains(), "domains", e);
            return e;
        }

        private void add(Collection<? extends OwlObject> c,
                                                String name, JsonObject e) {
            if ((c != null) && (! c.isEmpty())) {
                JsonArray x = new JsonArray();
                for (OwlObject o : c) {
                    x.add(new JsonPrimitive(o.uri()));
                }
                e.add(name, x);
            }
        }
    }

    /**
     * A {@link JsonSerializer} to serialize {@link URI}s.
     */
    private final static class UriSerializer implements JsonSerializer<URI>
    {
        public UriSerializer() {
            super();
        }

        public JsonElement serialize(URI src, Type typeOfSrc,
                                     JsonSerializationContext context) {
            return new JsonPrimitive(src.stringValue());
        }
    }
}
