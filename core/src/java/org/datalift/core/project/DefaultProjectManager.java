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

package org.datalift.core.project;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.config.ConfigKeys;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;
import com.clarkparsia.empire.sesametwo.RepositoryFactoryKeys;
import com.clarkparsia.utils.NamespaceUtils;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.DbSource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.security.SecurityContext;

import static org.datalift.fwk.util.StringUtils.*;


public class DefaultProjectManager implements ProjectManager, LifeCycle
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private Configuration configuration;

    private EntityManagerFactory emf = null;
    private EntityManager entityMgr = null;
    private ProjectJpaDao projectDao = null;

    private final Collection<Class<?>> classes = new LinkedList<Class<?>>();

    //-------------------------------------------------------------------------
    // LifeCycle contract support
    //-------------------------------------------------------------------------

    @Override
    public void init(Configuration configuration) {
        this.configuration = configuration;
        this.classes.addAll(this.getPersistentClasses());
        this.registerRdfNamespaces();
    }

    @Override
    public void postInit(Configuration configuration) {
        this.emf = this.createEntityManagerFactory(
                                        configuration.getInternalRepository());
        this.entityMgr = this.emf.createEntityManager();
        // Create Data Access Object for Projects.
        this.projectDao = new ProjectJpaDao(this.entityMgr);
    }

    @Override
    public void shutdown(Configuration configuration) {
        // Shutdown JPA persistence provider.
        if (this.emf != null) {
            if (this.entityMgr != null) {
                this.entityMgr.close();
            }
            this.emf.close();
        }
    }

    //-------------------------------------------------------------------------
    // ProkectManager contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Project findProject(URI uri) {
        return this.projectDao.find(uri);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Project> listProjects() {
        return this.projectDao.getAll();
    }

    /** {@inheritDoc} */
    @Override
    public CsvSource newCsvSource(URI uri, String title, String filePath,
                                  char separator, boolean hasTitleRow)
                                                            throws IOException {
        CsvSourceImpl src = new CsvSourceImpl(uri.toString());
        src.setTitle(title);
        File f = this.getFileStorage(filePath);
        if (!f.isFile()) {
            throw new FileNotFoundException(filePath);
        }
        src.setTitleRow(hasTitleRow);
        src.setFilePath(filePath);
        src.setMimeType("text/csv");
        src.setSeparator(String.valueOf(separator));
        for (Separator s : Separator.values()) {
            if (s.getValue() == separator) {
                src.setSeparator(s.name());
                break;
            }
        }
        src.init(this.configuration.getPublicStorage(), uri);
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public RdfSource newRdfSource(URI uri, String title, String filePath,
                                  String mimeType) throws IOException {
        RdfSourceImpl src = new RdfSourceImpl(uri.toString());
        src.setTitle(title);
        File f = this.getFileStorage(filePath);
        if (!f.isFile()) {
            throw new FileNotFoundException(filePath);
        }
        src.setFilePath(filePath);
        src.setMimeType(mimeType);
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public DbSource newDbSource(URI uri, String title, String database,
                                String srcUrl, String user, String password,
                                String request, int cacheDuration) {
        DbSourceImpl src = new DbSourceImpl(uri.toString());
        src.setTitle(title);
        src.setDatabase(database);
        src.setConnectionUrl(srcUrl);
        src.setUser(user);
        src.setPassword(password);
        src.setRequest(request);
        src.setCacheDuration(cacheDuration);
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public TransformedRdfSource newTransformedRdfSource(URI uri, String title, 
                                            URI targetGraph, Source parent) {
        TransformedRdfSourceImpl src =
                                new TransformedRdfSourceImpl(uri.toString());
        src.setTitle(title);
        src.setTargetGraph(targetGraph.toString());
        src.setParent(parent);
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public Ontology newOntology(URI srcUrl, String title) {
         OntologyImpl ontology = new OntologyImpl();
         ontology.setTitle(title);
         ontology.setSource(srcUrl);
         ontology.setDateSubmitted(new Date());
         ontology.setOperator(SecurityContext.getUserPrincipal());
         return ontology;
    }

    /** {@inheritDoc} */
    @Override
    public Project newProject(URI projectId, String title,
                              String description, URI license) {
        // Create new project.
        Project p = new ProjectImpl(projectId.toString());
        p.setTitle(title);
        p.setOwner(SecurityContext.getUserPrincipal());
        p.setDescription(description);
        p.setLicense(license);
        Date date = new Date();
        p.setDateCreation(date);
        p.setDateModification(date);
        return p;
    }
    
    /** {@inheritDoc} */
    @Override
    public String getProjectFilePath(String projectId, String fileName) {
        String path = "project/" + projectId;
        if (isSet(fileName)) {
            path += "/" + fileName;
        }
        return path;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteProject(Project p) {
        this.projectDao.delete(p);
    }

    /** {@inheritDoc} */
    @Override
    public void saveProject(Project p) {
        try {
            if (this.findProject(new URL(p.getUri()).toURI()) == null) {
                this.projectDao.persist(p);
                String id = p.getUri().substring(p.getUri().lastIndexOf("/") + 1);
                File projectStorage = this.getFileStorage(
                                            this.getProjectFilePath(id, null));
                projectStorage.mkdirs();
            }
            else {
                this.projectDao.save(p);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Invalid project URI: " + p.getUri(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addPersistentClasses(Collection<Class<?>> classes) {
        this.classes.addAll(classes);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public void registerRdfNamespace(RdfNamespace ns) {
        NamespaceUtils.addNamespace(ns.prefix, ns.uri);
    }

    private void registerRdfNamespaces() {
        for (RdfNamespace ns : RdfNamespace.values()) {
            NamespaceUtils.addNamespace(ns.prefix, ns.uri);
        }
    }

    /**
     * Returns the list of persistent classes to be handled by Empire JPA
     * provider.
     *
     * @return the list of persistent classes.
     */
    @SuppressWarnings("unchecked")
    private Collection<Class<?>> getPersistentClasses() {
        Collection<Class<?>> classes = new LinkedList<Class<?>>();
        classes.addAll(Arrays.asList(ProjectImpl.class, CsvSourceImpl.class,
                RdfSourceImpl.class, DbSourceImpl.class, OntologyImpl.class, 
                TransformedRdfSourceImpl.class));
        return classes;
    }

    private File getFileStorage(String path) {
        return new File(this.configuration.getPublicStorage(), path);
    }

    /**
     * Creates and configures a new Empire JPA EntityManagerFactory to
     * persist objects into the specified RDF repository.
     *
     * @param  repository   the RDF repository to persist objects into.
     *
     * @return a configured Empire EntityManagerFactory.
     */
    private EntityManagerFactory createEntityManagerFactory(
                                                        Repository repository) {
        // Build Empire configuration.
        EmpireConfiguration empireCfg = new EmpireConfiguration();
        // Set persistent classes and associated (custom) annotation provider.
        empireCfg.setAnnotationProvider(CustomAnnotationProvider.class);
        Map<String,String> props = empireCfg.getGlobalConfig();
        props.put(CustomAnnotationProvider.ANNOTATED_CLASSES_PROP,
                  join(this.classes, ",").replace("class ", ""));
        // Initialize Empire.
        Empire.init(empireCfg, new OpenRdfEmpireModule());
        // Configure target repository.
        Map<Object, Object> map = new HashMap<Object,Object>();
        map.put(ConfigKeys.FACTORY, "sesame");
        map.put(RepositoryFactoryKeys.REPO_HANDLE,
                                            repository.getNativeRepository());
        // Create Empire JPA persistence provider.
        PersistenceProvider provider = Empire.get().persistenceProvider();
        return provider.createEntityManagerFactory("", map);
    }

    //-------------------------------------------------------------------------
    // ProjectJpaDao nested class
    //-------------------------------------------------------------------------

    /**
     * A JPA DAO implementation for persisting ProjectImpl objects.
     */
    public final static class ProjectJpaDao extends GenericRdfJpaDao<Project> {
        public ProjectJpaDao(EntityManager em) {
            super(ProjectImpl.class, em);
        }

        @Override
        public Project save(Project entity) {
            entity.setDateModification(new Date());
            return super.save(entity);
        }
    }
}