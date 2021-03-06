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


import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import com.clarkparsia.empire.annotation.RdfsClass;
import com.clarkparsia.empire.impl.RdfQuery;

import org.datalift.core.util.web.RequestLifecycleFilter.RequestLifecycleListener;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.ObjectNotFoundException;
import org.datalift.fwk.project.PersistenceException;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * A generic RDF JPA DAO.
 *
 * @param  <T>   the type of objects this DAO persists.
 *
 * @author lbihanic
 */
public class GenericRdfJpaDao<T> implements GenericRdfDao,
                                            RequestLifecycleListener
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /**
     * The JPA {@link EntityManager} associated to the current thread.
     * <p>
     * Per the JPA specifications, EntityManager instances are not
     * thread-safe. Moreover as an implicit persistence context is
     * associated to each EntityManager instance, creating a new
     * instance for each user request guarantees that requests
     * isolation.</p>
     */
    private final static ThreadLocal<EntityManager> entityManager =
                                            new ThreadLocal<EntityManager>();

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The class the instances of which are persisted through this DAO. */
    protected final Class<? extends T> persistentClass;
    /** The RDF type of the persistent class. */
    protected final String rdfType;
    /**
     * The JPA entity manager factory this DAO relies on to access the
     * persistent object store.
     */
    protected final EntityManagerFactory entityMgrFactory;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new RDF JPA DAO.
     * @param  persistentClass    the default classes for the objects
     *                            persisted by this DAO.
     * @param  entityMgrFactory   the JPA EntityManagerFactory to use to
     *                            access the RDF store.
     *
     * @throws IllegalArgumentException if <code>persistentClass</code>
     *         is <code>null</code> or can not be mapped to a known
     *         persisted RDF type.
     */
    public GenericRdfJpaDao(final Class<? extends T> persistentClass,
                            final EntityManagerFactory entityMgrFactory) {
        if (persistentClass == null) {
            throw new IllegalArgumentException("persistentClass");
        }
        RdfsClass rdfsClass = persistentClass.getAnnotation(RdfsClass.class);
        if (rdfsClass == null) {
            throw new IllegalArgumentException(persistentClass.getName());
        }
        this.persistentClass = persistentClass;
        this.rdfType = rdfsClass.value();
        this.entityMgrFactory = entityMgrFactory;
    }

    //-------------------------------------------------------------------------
    // GenericRdfJpaDao contract definition
    //-------------------------------------------------------------------------

    /**
     * Retrieves all instances of T present in the RDF store.
     * @return a collection (possibly empty) containing all persistent
     *         instances found in the underlying storage.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public Collection<? extends T> getAll() {
        return this.getAll(this.persistentClass, this.rdfType);
    }

    /**
     * Retrieves an instance of T in the RDF store. The instance may
     * not exists.
     * @param  id   the RDF object identifier, as a URI.
     *
     * @return the object read from the RDF store or <code>null</code>
     *         if no object with the specified identifier was found.
     * @throws IllegalArgumentException if <code>id</code> is
     *         <code>null</code>.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public T find(URI id) {
        return this.find(this.persistentClass, id);
    }

    /**
     * Retrieves an instance of T from the RDF store. The instance
     * shall exist otherwise an error is returned.
     * @param  id   the RDF object identifier, as a URI.
     *
     * @return the object read from the RDF store.
     * @throws ObjectNotFoundException if no object with the specified
     *         identifier was found.
     * @throws IllegalArgumentException if <code>id</code> is
     *         <code>null</code>.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public T get(URI id) {
        return this.get(this.persistentClass, id);
    }

    /**
     * Deletes the specified RDF object.
     * @param  id   the RDF object identifier, as a URI.
     *
     * @throws IllegalArgumentException if <code>id</code> is
     *         <code>null</code>.
     * @throws ObjectNotFoundException if no object with the specified
     *         identifier was found.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public void delete(URI id) {
        this.delete(this.get(id));
    }

    /**
     * Executes the specified JPA-modified SPARQL query on the RDF
     * store and returns the collection of matched objects.
     * @param  query   the JPA-modified SPARQL query.
     *
     * @return the collection of matched objects, possibly empty.
     * @throws IllegalArgumentException if <code>query</code> is
     *         <code>null</code> or empty.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public List<? extends T> executeQuery(String query) {
        return this.executeQuery(query, this.persistentClass);
    }

    //-------------------------------------------------------------------------
    // GenericRdfDao contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public <C> Collection<? extends C> getAll(Class<C> entityClass) {
        return this.getAll(entityClass, null);
    }

    /** {@inheritDoc} */
    @Override
    public <C> C find(Class<C> entityClass, URI id) {
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass");
        }
        if (id == null) {
            throw new IllegalArgumentException("id");
        }
        C o = null;
        try {
            o = this.getEntityManager().find(entityClass, id);
        }
        catch (EntityNotFoundException e) {
            // Should never happen. => Ignore...
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
        return o;
    }

    /** {@inheritDoc} */
    @Override
    public <C> C get(Class<C> entityClass, URI id) {
        C entity = this.find(entityClass, id);
        if (entity == null) {
            throw new ObjectNotFoundException(String.valueOf(id));
        }
        return entity;
    }

    /** {@inheritDoc} */
    @Override
    public void persist(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity");
        }
        try {
            EntityManager em = this.getEntityManager();
            em.persist(entity);
            em.flush();
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <C> C save(C entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity");
        }
        try {
            EntityManager em = this.getEntityManager();
            entity = em.merge(entity);
            em.flush();
            return entity;
        }
        catch (EntityNotFoundException e) {
            throw new ObjectNotFoundException(e);
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <C> void delete(Class<C> entityClass, URI id) {
        this.delete(this.get(entityClass, id));
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Object entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity");
        }
        try {
            this.getEntityManager().remove(entity);
        }
        catch (EntityNotFoundException e) {
            throw new ObjectNotFoundException(e);
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <C> List<C> executeQuery(String query, Class<C> entityClass) {
        if (isBlank(query)) {
            throw new IllegalArgumentException("query");
        }
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass");
        }
        List<C> results = Collections.emptyList();
        try {
            Query q = this.getEntityManager().createQuery(query);
            q.setHint(RdfQuery.HINT_ENTITY_CLASS, entityClass);
            results = (List<C>)(q.getResultList());
        }
        catch (EntityNotFoundException e) {
            // Should never happen. => Ignore...
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
        return results;
    }

    //-------------------------------------------------------------------------
    // RequestLifecycleListener contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void requestReceived() {
        // Discard any pending transaction but there should be any.
        this.flushTransaction(true);
    }

    /** {@inheritDoc} */
    @Override
    public void responseSent() {
        // Flush and commit current transaction, if any.
        this.flushTransaction(false);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private <C> Collection<C> getAll(Class<C> entityClass, String rdfType) {
        if (entityClass == null) {
            throw new IllegalArgumentException("entityClass");
        }
        if (rdfType == null) {
            RdfsClass rdfsClass = entityClass.getAnnotation(RdfsClass.class);
            if (rdfsClass == null) {
                throw new IllegalArgumentException(entityClass.getName());
            }
            rdfType = rdfsClass.value();
        }
        List<C> results = new LinkedList<C>();

        try {
            Query query = this.getEntityManager().createQuery(
                                "where { ?result rdf:type " + rdfType + " . }");
            query.setHint(RdfQuery.HINT_ENTITY_CLASS, entityClass);
            for (Object p : query.getResultList()) {
                results.add((C)p);
            }
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
        return results;
    }

    /**
     * Returns the {@link EntityManager} for the current thread,
     * initializing one if none is available.
     * @return the {@link EntityManager} for the current thread.
     */
    private EntityManager getEntityManager() {
        EntityManager em = entityManager.get();
        if (em == null) {
            // Create and install new transaction manager.
            em = this.entityMgrFactory.createEntityManager();
            log.trace("Installing EntityManager: {}", em);
            entityManager.set(em);
        }
        EntityTransaction tx = em.getTransaction();
        if (! tx.isActive()) {
            // Initiate new transaction.
            log.trace("Initiating new JPA transaction");
            tx.begin();
        }
        return em;
    }

    private void flushTransaction(boolean expectNone) {
        // Close any pending transaction for the current thread.
        EntityManager em = entityManager.get();
        if (em != null) {
            EntityTransaction tx = em.getTransaction();
            if (tx.isActive()) {
                if (expectNone) {
                    log.warn("Unexpected active transaction found for thread {}",
                                            Thread.currentThread().getName());
                }
                try {
                    if (! tx.getRollbackOnly()) {
                        log.trace("Committing pending JPA transaction");
                        em.flush();
                        tx.commit();
                    }
                    else {
                        log.trace("Rolling back pending JPA transaction");
                        tx.rollback();
                    }
                    em.clear();
                    // Do not close the entity manager: Empire would shut down
                    // the whole EntityManagerFactory!
                    // em.close();
                }
                catch (Exception e) {
                    log.fatal("Failed to clear persistence context", e);
                }
            }
        }
    }
}
