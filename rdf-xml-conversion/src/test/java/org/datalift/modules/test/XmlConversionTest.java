package org.datalift.modules.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.datalift.modules.base.XmlConverter;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;

/*
 * Do not run these tests unless all the required services are started. Namley, Log4JLogService.
 * Easiest hack is to add the project datalift-core to the class path. Log4JLogService() could
 * be mocked, but since it's an implementation and not a reference, it wasn't done.
 */
@Ignore
public class XmlConversionTest {
	
	XmlConverter module;
	String xmlSource;
	String xslSource;
	Repository outputRepo;
	String basePath = "C:\\mondeca\\datalift\\sources\\datalift\\rdf-xml-conversion\\src\\test\\resources\\xml-rdf\\";
	
	@Before
	public void initTest() throws URISyntaxException{
//		LogService logService = new Log4JLogService();
		module = new XmlConverter();
		xmlSource = basePath + "test\\12388548.xml";
		xslSource = basePath + "bnf2rdf.xsl";
		
	}
	
	@Test
//	public void testXmlConversion() throws Exception{
//		BasicConfigurator.configure();
//		
//		outputRepo = module.applyXslTransformation(xmlSource, xslSource);
//		assertNotNull(outputRepo);
//		
//		RepositoryResult<Statement> statements = outputRepo.getConnection().getStatements(null, null, null, true);
//		List<Statement> stmts = statements.asList();
//		System.out.println(stmts.size());
//
//		   for (Statement st : stmts) {
//			   System.out.println("inside...");
//		      System.out.println(st.getSubject() + " -- " + st.getPredicate() + " -- " + st.getObject());
//		    
//		   }
//
//		assertTrue(stmts.size() > 0);
//		
//	}
	
	@After
	public void tearDown() throws RepositoryException{
		outputRepo.shutDown();
	}

}
