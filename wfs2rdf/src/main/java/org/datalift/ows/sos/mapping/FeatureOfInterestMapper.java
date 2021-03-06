package org.datalift.ows.sos.mapping;

import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.utilities.Helper;
import org.datalift.ows.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.Resource;
import org.openrdf.rio.RDFHandlerException;
/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
public class FeatureOfInterestMapper extends BaseMapper  {

	private boolean mapped=false;
	@Override
	protected void mapWithParent(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!alreadyLinked)
		{
			if(cf.isSimple())
			{
				this.addParentSimpleLinkStatements(cf, ctx);
				return;
			}
		}
	}
	/**
	 * FOR SHORTCUT MAPPING : the feature's content is directly linked with its parent 
	 * links the unique value of the  cf with the subject cf.getParent.id using the name of cf as a predicat
	 * it generates ONE triple
	 * use case of this is <om:procedure xlink:href="urn:xxx"/>
	 * @param cf
	 * @param ctx
	 * @throws RDFHandlerException 
	 */
	@Override
	protected void addParentSimpleLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		// first of all, look at the value of the feature. if any then try to create the triple with the value of one attribute 
		if(Helper.isSet(cf.value))
		{
			mapTypedValue(cf.getParent().getId(), cf.value, cf.getTypeName(), cf.name, Context.nsOml+"featureOfInterest", ctx);
			return;
		}
		this.mapFeatureSimpleAttributes(cf, ctx, cf.getParent().getId());	
		mappedAsSimple=true;
	}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		return;
	}
	@Override
	protected void mapGeometryIfAny(ComplexFeature cf, Context ctx) {
		return;
	}
	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) {
		return;
	}
	@Override
	protected void mapFeatureSimpleValue(ComplexFeature cf, Context ctx) {
		return;
	}
	@Override
	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource toLinkWith) throws RDFHandlerException {
		if(!mapped && !mappedAsSimple)
			{
			Resource id;
			String predicate=null;
			if (toLinkWith == null) {
				id = cf.getId();
			} else {
				id = toLinkWith;
				predicate=Context.nsDatalift+cf.name.getLocalPart();
			}
			for (Attribute a : cf.itsAttr) {
				if (!(a instanceof ComplexFeature) && isUsefulAttribute(a)) {
					mapTypedValue(id, a.value, a.getTypeName(), a.name, predicate, ctx);
				}
				mapped=true;
			}
			}
		
		
	}

}
