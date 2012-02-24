package org.datalift.modules.base;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import com.mondeca.rdf.transform.engine.RuleEngineManager;
import com.mondeca.rdf.transform.rule.RuleParserIfc;
import com.mondeca.rdf.transform.rule.RuleSet;

public class RdfConverter extends BaseConverter {
	
	private static Logger log = Logger.getLogger(RdfConverter.class);
	

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "rdf-converter";
	
    protected Configuration configuration   = null;
    protected ProjectManager projectManager = null;
    protected SparqlEndpoint sparqlEndpoint = null;
    protected Repository internalRepository = null;

	public RdfConverter() {
		super(MODULE_NAME, true, SourceType.TransformedRdfSource);
	}
	
	@Override
	public void postInit(Configuration configuration) {
		super.postInit(configuration);

		this.configuration = configuration;
		this.internalRepository = configuration.getInternalRepository();
		this.projectManager = configuration.getBean(ProjectManager.class);
		this.sparqlEndpoint = configuration.getBean(SparqlEndpoint.class);
	}

	@Override
	public UriDesc canHandle(Project p) {

        UriDesc uriDesc = null;
        if (this.findSource(p, false) != null) {
            try {
                String label = "Rules based RDF to RDF Converter";

                uriDesc = new UriDesc(
                                    this.getName() + "?project=" + p.getUri(),
                                    HttpMethod.GET, label);
            }
            catch (Exception e) {
            	e.printStackTrace();
            }
        }
        return uriDesc;
    }

	@GET
	public Response getIndex(@QueryParam("project") URI projectId,
							@Context UriInfo uriInfo, 
							@Context Request request,
							@HeaderParam(ACCEPT) String acceptHdr) throws WebApplicationException{

		Response response = null;
		
     try{
    	 log.info("[INFO] creating the index page for rdf converter module");
        Project p = this.getProject(projectId);
        Map<String, Object> args = new TreeMap<String, Object>();
        try {
            args.put("it", this.canHandle(p).getUrl(
                                            uriInfo.getBaseUri().toString()));
        }	
        catch (MalformedURLException e) { /* Ignore... */ }

        args.put("project", p);
        response = Response.ok(this.newViewable("/rdf_ui_base.vm", args))
                           .build();
        log.info("[INFO] returning the index page");
     } catch(Exception e){
    	 throw new WebApplicationException(Response
					.status(Status.BAD_REQUEST)
					.entity("Couldn't display the index page of the module" + e.getMessage()
							+ ")").build());
     }
     
        
        return response;
	}

	@POST
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response loadSourceData(@QueryParam("project") URI projectId,
			@QueryParam("source") URI sourceId,
			@FormParam("rules_file_path") String ruleSetPath, 
			@FormParam("dest_title") String destTitle,
			@FormParam("dest_graph_uri") URI targetURI,
			@Context UriInfo uriInfo, @Context Request request,
			@HeaderParam(ACCEPT) String acceptHdr)
			throws WebApplicationException {

		Response response = null;
		try {
			
			   // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source. It should be transformed rdf source
            TransformedRdfSource src = (TransformedRdfSource) p.getSource(sourceId);
            // Convert data and load generated RDF triples.
            this.transformData(src, new File(ruleSetPath), targetURI);
            // Register new transformed RDF source.
            log.info("Adding transformed rdf as a new source to the project");
            this.addResultSource(p, src, destTitle, targetURI);
            // Display generated triples.
            String uri = projectId.toString() + "#source";
            response = Response.created(projectId)
                    .entity(this.newViewable("/redirect.vm", uri))
                    .type(TEXT_HTML)
                    .build();
            
            return response;

		} catch (Exception e) {
			log.fatal("Failed to do the transformation --- ", e);
			throw new WebApplicationException(Response
					.status(Status.BAD_REQUEST)
					.entity("Couldn't process the request" + e.getMessage()
							+ ")").build());
		}
		

	}
	
	//public - for testing
	public org.openrdf.repository.Repository applyRules(
			org.openrdf.repository.Repository inputRepository, File ruleSet)
			throws Exception {
		
		log.info("[INFO] starting the rdf-rdf(rules based) transformation...");
		
		org.openrdf.repository.Repository outputRepository = new SailRepository(new MemoryStore());
		outputRepository.initialize();
		
		List<File> rulesStreams = new ArrayList<File>();
		
		try {

			if (ruleSet.isDirectory()) {
				File[] files = ruleSet.listFiles();
				for (File f : files) {
					rulesStreams.add(f);
				}
			} else
				rulesStreams.add(ruleSet);

			RuleEngineManager manager = new RuleEngineManager();
			//TODO give this as an option to user?
			manager.setUseRDFSInferencing(false);
			log.debug("Initializing rule engine manager");
			manager.init();

			RuleParserIfc parser = manager.getRuleReader();
			List<RuleSet> rules = new ArrayList<RuleSet>();
			try {
				for (File f : rulesStreams) {
					FileInputStream fis = new FileInputStream(f);
					rules.add(parser.readRules(fis));
					fis.close();
				}
			} catch (Exception e) {
				throw new Exception(e);
			}

			manager.transform(inputRepository, rules, outputRepository, false);
		} catch (Exception rdfe) {
			rdfe.printStackTrace();
		}

		log.info("[INFO] Done applying rules ...");
		
		return outputRepository;
		
	}
	
	private void addOutputToRepository( org.openrdf.repository.Repository outputRepository, URI targetURI)
			throws Exception {

		log.info("[INFO] Adding transformed rdf to the internal store ...");

		RepositoryConnection conn = this.configuration.getInternalRepository().newConnection();
		// for some reason, if auto commit is false, connection is being
		// shutdown - will create problems if a large file is bring processed
//		conn.setAutoCommit(false);
		
		try {
			org.openrdf.model.URI ctx = null;
			if (targetURI != null) {
				ctx = conn.getValueFactory().createURI(targetURI.toString());
				conn.clear(ctx);
			}

			conn.add(
					outputRepository.getConnection().getStatements(null, null,
							null, false), ctx);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			conn.close();
		}

		conn.commit();

		log.info("[INFO] Done.");

	}
	

	/*
	 * The transformation is applied in a seperate repository created specially
	 * for this purpose. This is to ensure that the data inside internal store
	 * doesn't get corrupted during the transformation process and also a
	 * limitation on the part of the Mondeca's rdf-transform library to not
	 * being able to specify named graphs inside repositories as input
	 */
	private void transformData(TransformedRdfSource src, File ruleSet,
			URI targetURI) throws Exception {

		org.openrdf.repository.Repository repository = new SailRepository(new MemoryStore());
		repository.initialize();

		ValueFactory f = internalRepository.getNativeRepository()
				.getValueFactory();
		org.openrdf.model.URI context = f.createURI(src.getTargetGraph());

		RepositoryResult<Statement> stmts = internalRepository
				.getNativeRepository()
				.getConnection()
				.getStatements((Resource) null, (URIImpl) null,
						(Resource) null, false, (Resource) context);

		repository.getConnection().add(stmts, (Resource) null);

		org.openrdf.repository.Repository output = this.applyRules(repository, ruleSet);
		this.addOutputToRepository(output, targetURI);

	}
	
//	private void addResultSource(Project p, Source parent, String name,
//			URI namedGraph) throws IOException {
//		log.info("[INFO] Adding transformed sources to the list of sources...");
//		p.addSource(this.projectManager.newTransformedRdfSource(namedGraph,
//				name, "", namedGraph, parent));
//		this.projectManager.saveProject(p);
//	}

}
