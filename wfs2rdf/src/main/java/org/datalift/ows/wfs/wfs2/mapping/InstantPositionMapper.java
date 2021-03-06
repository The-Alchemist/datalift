package org.datalift.ows.wfs.wfs2.mapping;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.utilities.Helper;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandlerException;

/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
public class InstantPositionMapper extends BaseMapper{

	@Override
	protected void mapFeatureSimpleValue(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		XMLGregorianCalendar d=Helper.getDate(cf.value);
		if(d!=null)
		{
			Value v5=ctx.vf.createLiteral(d);
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(),ctx.vf.createURI(Context.nsw3Time+"inXSDDateTime"), v5));
		}
	}

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsw3Time+"Instant")));
		if(cf.isReferencedObject())
		{
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsDatalift+Helper.capitalize(Context.referencedObjectType.getLocalPart()))));
		}
	}

	@Override
	protected void addParentSimpleLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {	
		Resource subjectURI;
		ComplexFeature parent=cf.getParent();
		if(parent!=null )
		{
			if(parent.getId()!=null)
			{
				subjectURI= parent.getId();
			}
			else
			{
				return;
			}
		}
		else 
		{
			subjectURI=Context.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
		String p=cf.name.getLocalPart();
		URI preperty=null;
		if(p.contains("begin"))
		{
			preperty=ctx.vf.createURI(Context.nsw3Time+"begin");
		}
		if(p.contains("end"))
		{
			preperty=ctx.vf.createURI(Context.nsw3Time+"end");	
		}
		if(preperty!=null)
		{
			ctx.model.handleStatement(ctx.vf.createStatement(subjectURI, preperty, cf.getId()));
		}
		mappedAsSimple=true;
	}

	@Override
	protected void setCfId(ComplexFeature cf, Context ctx) {
		/******give the feature an identifier****/
		Resource os;
		int count=0;		
		if(cf.getId()!=null)
		{
			alreadyLinked=true;
			return;
		}
		//check if there is any gml identifier. if yes, use it as id if not, create a generic id
		String id=cf.getAttributeValue(Const.identifier);
		if(id==null)
		{
			if(cf.isReferencedObject())
			{
				QName type=Context.referencedObjectType;//Const.ReferenceType;
				count =ctx.getInstanceOccurences(type);
				os=ctx.vf.createURI(Context.nsProject+type.getLocalPart()+"_"+count);
			}
			else
			{
				count=ctx.getInstanceOccurences(cf.name);
				os=ctx.vf.createBNode(cf.name.getLocalPart()+"_"+count);
			}		
			cf.setId(os);
		}
		else
		{
			try {
				cf.setId(ctx.vf.createURI(id));
			} catch (Exception e) {
				//if the id is not a valid URI, then use the standard base URI 
				cf.setId(ctx.vf.createURI(ctx.baseURI+id));
			}
		}	
		alreadyLinked=false;	
	}
}
