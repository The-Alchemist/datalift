package org.datalift.s4ac.services;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.datalift.s4ac.utils.CRUDType;



public class AccessPolicy {
	private ACSType acstype;
	private String uri;
	private Set<String> graphs;
	private Set<CRUDType> privileges = new HashSet<CRUDType>();
	private Set<String> asks = new HashSet<String>();
	
	public AccessPolicy(String uri, String type, Set<String> graphs) {
		this.uri=uri;
		this.graphs=graphs;
		if(type.equals("http://ns.inria.fr/s4ac/v2#ConjunctiveAccessConditionSet")) 
			this.acstype = ACSType.CONJUNCTIVE;
		else if(type.equals("http://ns.inria.fr/s4ac/v2#DisjunctiveAccessConditionSet")) 
			this.acstype = ACSType.DISJUNCTIVE;
	}

	public AccessPolicy(String uri, String type) {
		this(uri, type, null);
	}
	
	public ACSType getAcstype() {
		return acstype;
	}

	public Set<String> getGraph() {
		return graphs;
	}

	public Set<CRUDType> getPrivileges() {
		return privileges;
	}

	public Set<String> getAsks() {
		return asks;
	}

	public String getUri() {
		return uri;
	}
	
	public void setAcstype(ACSType acstype) {
		if(this.acstype==null) this.acstype = acstype;
	}

	public void setGraph(Set<String> graphs) {
		if(this.graphs==null) this.graphs = graphs;
	}

	public void setPrivileges(Set<CRUDType> privileges) {
		this.privileges = privileges;
	}
	
	public void addPrivilege(CRUDType privilege) {
		this.privileges.add(privilege);
	}

	public void setAsks(Set<String> asks) {
		this.asks = asks;
	}
	
	public void addAsk(String ask) {
		this.asks.add(ask);
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}

	public void addGraph(String g) {
		this.graphs.add(g);
		
	}
}