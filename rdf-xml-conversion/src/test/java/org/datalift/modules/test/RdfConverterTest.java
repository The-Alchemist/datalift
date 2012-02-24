package org.datalift.modules.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.datalift.fwk.log.LogService;
import org.datalift.fwk.log.Logger;
import org.datalift.modules.base.RdfConverter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;

@Ignore
public class RdfConverterTest {
	
	private RdfConverter converter;
	private String rdfFile;
	private File ruleSet;
	private Repository repository;
	private URI targetURI;
	private Repository output;
	private String basePath = "C:\\mondeca\\datalift\\sources\\datalift\\rdf-xml-conversion\\src\\test\\resources\\rdf-rdf\\";
	
	@Before
	public void initEnv() throws URISyntaxException{
	
//	LogService log = new Log4JLogService();
			
	converter = new RdfConverter();
	rdfFile = basePath + "\\ontocdt71_v0.5.rdf";
	ruleSet = new File(basePath + "\\OWLtoITM.xml");
	repository = new SailRepository(new MemoryStore());
	targetURI = new URI("http://test/target/uri#");
	}
	
	@Ignore
//	@Test
//	public void testRdfConversion() throws Exception{
//	
//	try{	output = converter.applyRules(new File(rdfFile), ruleSet);
//		assertTrue(output != null);
//		RepositoryResult<Statement> statements = output.getConnection().getStatements(null, null, null, true);
//		List<Statement> stmts = statements.asList();
//		System.out.println(stmts.size());
//
//		   for (Statement st : stmts) {
//			   System.out.println("inside...");
//		      System.out.println(st.getSubject() + " -- " + st.getPredicate() + " -- " + st.getObject());
//		    
//		   }
//
//		   assertTrue(stmts.size() > 0);
//	}
//	catch(Exception e){
//		e.printStackTrace();
//	}
//	finally{
//	
//	}
//		
//	}
	
	@After
	public void tearDown(){
		try {
			output.shutDown();
			repository.shutDown();
		} catch (RepositoryException e) {
			System.out.println("org.datalift.modules.test.RdfConverterTest - Error shutting down repository");
			e.printStackTrace();
		}
	}

}
