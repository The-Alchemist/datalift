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

package org.datalift.core.rdf.sesame;


import java.io.File;
import java.net.URI;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

import org.datalift.core.TechnicalException;
import org.datalift.core.rdf.BaseRepository;
import org.datalift.core.rdf.RepositoryFactory;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.rdf.Repository;


/**
 * A repository factory implementation for instantiating
 * <a href="http://www.openrdf.org/">Open RDF Sesame 2</a> repositories.
 * <p>
 * This implementation support two types on Sesame repositories:</p>
 * <dl>
 *  <dt>HTTP</dt>
 *  <dd>Remote Sesame repositories accessible over HTTP</dd>
 *  <dt>SAIL</dt>
 *  <dd>Local, JVM-embedded, in-memory or file-based Sesame
 *   repositories</dd>
 * </dl>
 *
 * @author lbihanic
 */
public final class SesameRepositoryFactory extends RepositoryFactory
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The URL scheme for Sesame HTTP repositories. */
    public final static String HTTP_URL_SCHEME  = "http:";
    /**
     * The URL scheme for Sesame SAIL repositories (both in-memory and
     * file-based).
     */
    public final static String SAIL_URL_SCHEME  = "sail:";
    /** The URL scheme for Sesame file-based SAIL repositories. */
    public final static String FILE_URL_SCHEME  = "file:";

    //-------------------------------------------------------------------------
    // RepositoryFactory contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Repository newRepository(String name, String url,
                                                 Configuration configuration) {
        Repository r = null;

        if (url.startsWith(HTTP_URL_SCHEME)) {
            r = new SesameHttpRepository(name, url, configuration);
        }
        else if ((url.startsWith(SAIL_URL_SCHEME)) ||
                 (url.startsWith(FILE_URL_SCHEME))) {
            r = new SesameSailRepository(name, url, configuration);
        }
        // Else: Not a Sesame repository.

        return r;
    }

    //-------------------------------------------------------------------------
    // SesameHttpRepository nested class
    //-------------------------------------------------------------------------

    /**
     * A Repository implementation to access remote Sesame repositories
     * repositories over HTTP.
     */
    public final static class SesameHttpRepository extends BaseRepository
    {
        //---------------------------------------------------------------------
        // Constructors
        //---------------------------------------------------------------------
    
        /**
         * Build a new DataLift repository accessing a remote Sesame
         * repository over HTTP.
         * @param  name            the repository name in DataLift
         *                         configuration.
         * @param  url             the repository URL.
         * @param  configuration   the DataLift configuration.
         *
         * @throws IllegalArgumentException if either <code>name</code>
         *         or <code>configuration</code> is null.
         * @throws RuntimeException if any error occurred connecting the
         *         repository.
         */
        public SesameHttpRepository(String name, String url,
                                                 Configuration configuration) {
            super(name, url, configuration);
        }
    
        //---------------------------------------------------------------------
        // BaseRepository contract support
        //---------------------------------------------------------------------
    
        /** {@inheritDoc} */
        @Override
        protected org.openrdf.repository.Repository
                            newNativeRepository(Configuration configuration) {
            org.openrdf.repository.Repository repository = null;
            try {
                repository = new HTTPRepository(this.url);
                repository.initialize();
            }
            catch (OpenRDFException e) {
                throw new TechnicalException("repository.connect.error", e,
                                        this.name, this.url, e.getMessage());
            }
            return repository;
        }
    }

    //-------------------------------------------------------------------------
    // SesameSailRepository nested class
    //-------------------------------------------------------------------------

    /**
     * A Repository implementation to access local (JVM-embedded)
     * Sesame repositories.
     */
    private final static class SesameSailRepository extends BaseRepository
    {
        //---------------------------------------------------------------------
        // Constructors
        //---------------------------------------------------------------------
    
        /**
         * Build a new DataLift repository accessing a local,
         * JVM-embedded Sesame repository.
         * @param  name            the repository name in DataLift
         *                         configuration.
         * @param  url             the repository URL.
         * @param  configuration   the DataLift configuration.
         *
         * @throws IllegalArgumentException if either <code>name</code>
         *         or <code>configuration</code> is null.
         * @throws RuntimeException if any error occurred creating the
         *         repository.
         */
        public SesameSailRepository(String name, String url,
                                                 Configuration configuration) {
            super(name, url, configuration);
        }
    
        //---------------------------------------------------------------------
        // BaseRepository contract support
        //---------------------------------------------------------------------
    
        /** {@inheritDoc} */
        @Override
        protected org.openrdf.repository.Repository
                            newNativeRepository(Configuration configuration) {
            org.openrdf.repository.Repository repository = null;
            try {
                String path = this.url;
                if (path.startsWith(SAIL_URL_SCHEME)) {
                    path = path.substring(SAIL_URL_SCHEME.length());
                }
                MemoryStore store = (path.startsWith(FILE_URL_SCHEME))?
                                    new MemoryStore(new File(new URI(path))):
                                    new MemoryStore();
                repository = new SailRepository(store);
                repository.initialize();
            }
            catch (IllegalArgumentException e) {
                // File path URI rejected by File constructor.
                throw new TechnicalException("repository.invalid.url", e,
                                             this.name, this.url);
            }
            catch (Exception e) {
                throw new TechnicalException("repository.config.error", e,
                                             this.name, e.getMessage());
            }
            return repository;
        }
    }
}