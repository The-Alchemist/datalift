package org.datalift.modules.base;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
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
import org.datalift.fwk.project.XmlSource;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.modules.exception.MondecaModuleException;
import org.openrdf.repository.RepositoryConnection;

import com.mondeca.sesame.toolkit.repository.LocalMemoryRepositoryProvider;
import com.mondeca.sesame.toolkit.repository.RepositoryProviderException;
import com.mondeca.sesame.toolkit.repository.XMLtoRDFDataInjector;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

//@MetaInfServices(org.datalift.fwk.Module.class)
public class XmlConverter extends BaseConverter{
	
private static Logger log = Logger.getLogger(XmlConverter.class);
	
	private static final String MODULE_NAME = "xml-converter";
	
    protected Configuration configuration   = null;
    protected ProjectManager projectManager = null;
    protected SparqlEndpoint sparqlEndpoint = null;
    protected Repository internalRepository = null;
    
    
	public XmlConverter(){
		super(MODULE_NAME, true, SourceType.XmlSource);
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
	                String label = "XSL based XML Converter";

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
	
	/**
	 * 
	 * @param projectId
	 * @param uriInfo
	 * @param request
	 * @param acceptHdr
	 * @return returns the index page for this module
	 * @throws WebApplicationException
	 */

	@GET
	public Response getIndex(@QueryParam("project") URI projectId,
							@Context UriInfo uriInfo, 
							@Context Request request,
							@HeaderParam(ACCEPT) String acceptHdr) throws WebApplicationException{

		Response response = null;
		
     try{
    	 log.info("[INFO] creating the index page...");
        Project p = this.getProject(projectId);
        Map<String, Object> args = new TreeMap<String, Object>();
        try {
            args.put("it", this.canHandle(p).getUrl(
                                            uriInfo.getBaseUri().toString()));
        }
        catch (MalformedURLException e) { /* Ignore... */ }

        args.put("project", p);
        
        response = Response.ok(this.newViewable("/xml_ui_base.vm", args))
                           .build();
        log.info("[INFO] returning the index page");
     } catch(Exception e){
    	 log.fatal("Failed to display the index page of the module ", e);
    	 response = Response.status(Status.BAD_REQUEST)
					.entity("Couldn't display the index page of the module" + e.getMessage()
							+ ")").build();
     }
     
        
        return response;
	}

	@POST
//	@Consumes(MediaType.)
	public Response loadSourceData(@QueryParam("project") URI projectId,
			@QueryParam("source") URI sourceId,
//			@FormDataParam("xsl_file") InputStream xslData,
			@FormParam("xsl_file") String xslFile,
			@FormParam("dest_title") String destTitle,
			@FormParam("dest_graph_uri") URI targetGraph,
			@Context UriInfo uriInfo, 
			@Context Request request,
			@HeaderParam(ACCEPT) String acceptHdr)
			throws WebApplicationException{
		
//		if (xslData == null) {
//			throw new WebApplicationException(new MondecaModuleException(
//					"XSL file source in null. Please select a file!"));
//		}

		Response response = null;
		try {
			
			Project p = this.getProject(projectId);

			XmlSource src = (XmlSource) p.getSource(sourceId);
			log.info("Transforming xml now...");
			
			System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
			
			this.transformData(src, xslFile, this.internalRepository, targetGraph);

			this.addResultSource(p, src, destTitle, targetGraph);

			//display converted triples
			String uri = projectId.toString() + "#source";
			response = Response.created(projectId)
					.entity(this.newViewable("/redirect.vm", uri))
					.type(TEXT_HTML).build();
			log.info("Finished transformation....returning response");
		
		} catch (Exception e) {
			log.fatal("Failed to do the transformation --- ", e);
			response = Response
					.status(Status.BAD_REQUEST)
					.entity("Couldn't process the request" + e.getMessage()
							+ ")").build();
		}
		return response;

	}
	
	public void transformData(XmlSource xmlSource, String xslSource,
			Repository internalRepository, URI targetURI) throws Exception {

		this.transformData(xmlSource.getFilePath(), xslSource,
				internalRepository, targetURI);
	}
	
	//public for tests
	public org.openrdf.repository.Repository applyXslTransformation(String xmlSource, String xslSource) throws RepositoryProviderException{
		
		org.openrdf.repository.Repository outputRepository = null;
		try {
			getClass().getClassLoader().loadClass(
					"net.sf.saxon.TransformerFactoryImpl");
			if (log.isDebugEnabled())
				log.debug(
						"Setting XML Transformer Factory to this implementation - {}",
						"net.sf.saxon.TransformerFactoryImpl");
			System.setProperty("javax.xml.transform.TransformerFactory",
					"net.sf.saxon.TransformerFactoryImpl");
		} catch (java.lang.ClassNotFoundException e) {
			log.error("Saxon class not found. If you want to use XSLT 2.0 features, include saxon jar in the classpath.");
		}
		
		LocalMemoryRepositoryProvider repositoryProvider = new LocalMemoryRepositoryProvider();
		File xml = new File(Configuration.getDefault().getPublicStorage(), xmlSource);
		repositoryProvider.setDataInjector(new XMLtoRDFDataInjector(xml.getPath(), xslSource));
		repositoryProvider.init();
		outputRepository = repositoryProvider.getRepository();
		
		return outputRepository;
	}

	public void transformData(String xmlSource, String xslSource,
			Repository internalRepository, URI targetURI) throws Exception {
		RepositoryConnection conn = this.configuration.getInternalRepository().newConnection();
		org.openrdf.repository.Repository outputRepository = null;
		
		try {
			outputRepository = this.applyXslTransformation(xmlSource, xslSource);
		} catch (Exception e) {
			log.fatal("[FAIL] Failed to apply xsl transformation on the xml source ", e);
			throw new MondecaModuleException("Failed to apply xsl transformation on the xml source");
		}
		
		
		try {
			org.openrdf.model.URI ctx = null;
			if (targetURI != null) {
				ctx = conn.getValueFactory().createURI(targetURI.toString());
				conn.clear(ctx);
			}

			//TODO if no output is generated, a new source shouldn't be created!
			if (outputRepository != null) {
				conn.add(
						outputRepository.getConnection().getStatements(null,
								null, null, false), ctx);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			conn.close();
		}

	}
	
//	private void addResultSource(Project p, Source parent, String name,
//			URI namedGraph) throws IOException {
//		log.info("[INFO] Adding transformed sources to the list of sources...");
//		p.addSource(this.projectManager.newTransformedRdfSource(p, namedGraph,
//				name, "", namedGraph, parent));
//		this.projectManager.saveProject(p);
//	}

}
