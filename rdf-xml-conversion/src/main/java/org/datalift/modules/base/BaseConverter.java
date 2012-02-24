package org.datalift.modules.base;

import java.io.IOException;
import java.net.URI;

import javax.ws.rs.HttpMethod;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;

import com.sun.jersey.api.view.Viewable;

public abstract class BaseConverter extends BaseModule implements ProjectModule{
	
	 private final static Logger log = Logger.getLogger();

	    protected final SourceType inputSource;
	    protected final HttpMethod accessMethod;

	    protected Configuration configuration   = null;
	    protected ProjectManager projectManager = null;
	    protected SparqlEndpoint sparqlEndpoint = null;
	    protected Repository internalRepository = null;


	protected BaseConverter(String name, SourceType inputSource) {
		super(name, true);
		this.inputSource = inputSource;
		accessMethod = HttpMethod.GET;
	}
	
	protected BaseConverter(String name, boolean isResource, SourceType inputSource) {
		super(name, isResource);
		this.inputSource = inputSource;
		accessMethod = HttpMethod.GET;
		
	}
	
	@Override
	public void postInit(Configuration configuration) {
		super.postInit(configuration);

		this.configuration = configuration;
		this.internalRepository = configuration.getInternalRepository();
		this.projectManager = configuration.getBean(ProjectManager.class);
		this.sparqlEndpoint = configuration.getBean(SparqlEndpoint.class);
	}


	protected final Project getProject(URI projectId) throws Exception {
		Project p = this.projectManager.findProject(projectId);
		if (p == null) {
			throw new Exception("Project is null!");
		}

		return p;
	}
	
    protected final Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }

 protected final Source findSource(Project p, boolean findLast) {
     if (p == null) {
         throw new IllegalArgumentException("p");
     }
     Source src = null;
     for (Source s : p.getSources()) {
         if (s.getType() == this.inputSource) {
             src = s;
             if (! findLast) break;
         }
     }
     return src;
 }
 
 /**
  * Creates a new transformed RDF source and attach it to the
  * specified project.
  * @param  p        the owning project.
  * @param  parent   the parent source object.
  * @param  name     the new source name.
  * @param  uri      the new source URI.
  *
  * @return the newly created transformed RDF source.
  * @throws IOException if any error occurred creating the source.
  */
	protected TransformedRdfSource addResultSource(Project p, Source parent,
			String name, URI uri) throws IOException {
		TransformedRdfSource newSrc = this.projectManager
				.newTransformedRdfSource(p, uri, name, null, uri, parent);
		this.projectManager.saveProject(p);
		return newSrc;
	}

}
