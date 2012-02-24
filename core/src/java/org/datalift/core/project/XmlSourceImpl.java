package org.datalift.core.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.XmlSource;
import org.datalift.fwk.util.CloseableIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.clarkparsia.empire.annotation.RdfsClass;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

@Entity
@RdfsClass("datalift:xmlSource")
public class XmlSourceImpl extends BaseFileSource<Node> implements XmlSource {
	
	private final static Logger log = Logger.getLogger();

	protected XmlSourceImpl() {
		super(SourceType.XmlSource);
	}

	public XmlSourceImpl(String uri, Project p) {
		super(SourceType.XmlSource, uri, p);
	}

	@Override
	public CloseableIterator<Node> iterator() {
		
		log.info("creating a node iterator...");

		List<Node> nodeList = new ArrayList<Node>();

		DOMParser parser = new DOMParser();
		InputSource inputSource;
		try {
			inputSource = new InputSource(this.getInputStream());
			parser.parse(inputSource);

			Document doc = parser.getDocument();
			visit(doc, 0, nodeList);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return new NodeIterator(nodeList);

	}

	//TODO for efficiency, use linked or double linked list instead of for loop
	private void visit(Node node, int level, List<Node> list) {
		log.debug("Visiting all the nodes");
		
		NodeList nodeList = node.getChildNodes();
		if (nodeList.getLength() > 0) {
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node n = nodeList.item(i);
				if (!list.contains(n)) {
					if (log.isTraceEnabled())
						log.trace("Adding node with name - {}", n.getNodeName());
					list.add(n);
				}
				visit(n, level + 1, list);
			}
		}
	}

	// TODO this is a basic iterator - improve this iterator to provide more
	// features(siblings iterator/children iterator?)

	private class NodeIterator implements CloseableIterator<Node> {

		private Iterator<Node> iterator;

		public NodeIterator(List<Node> nodeList) {
			this.iterator = nodeList.iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Node next() {
			return iterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();

		}

		@Override
		public void close() {
			// nothing to do here?
		}
	}
}
