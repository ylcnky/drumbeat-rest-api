package fi.aalto.cs.drumbeat.rest.api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.NotFoundException;

import fi.aalto.cs.drumbeat.rest.common.DrumbeatResponseBuilder;
import fi.aalto.cs.drumbeat.rest.common.DrumbeatWebException;
import fi.aalto.cs.drumbeat.rest.common.NameFormatter;
import fi.aalto.cs.drumbeat.rest.managers.DataSetObjectManager;
import fi.aalto.cs.drumbeat.common.DrumbeatException;
import fi.aalto.cs.drumbeat.common.params.BooleanParam;

@Path("/dsobjects")
public class DataSetObjectResource extends DrumbeatApiBase {

	private static final Logger logger = Logger.getLogger(DataSetObjectResource.class);

	private DataSetObjectManager getDataSetObjectManager() {
		try {
			return new DataSetObjectManager();
		} catch (DrumbeatException e) {
			logger.error(e);
			throw new DrumbeatWebException(
					Status.INTERNAL_SERVER_ERROR,
					"Error getting DataSetObjectManager instance: " + e.getMessage(),
					e);
		}
	}
	
	@GET
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}")
	public Response getAll(			
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers,
			@Context HttpServletRequest request)
	{
		notifyRequest(uriInfo, headers, request);
		
		try {		
			Model model = getDataSetObjectManager().getAll(collectionId, dataSourceId, dataSetId);
			String modelBaseUri = NameFormatter.formatObjectResourceBaseUri(collectionId, dataSourceId);
			return DrumbeatResponseBuilder.build(
					Status.OK,
					model,
					modelBaseUri,					
					headers.getAcceptableMediaTypes());			
		} catch (NotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		} catch (DrumbeatException e) {
			throw new DrumbeatWebException(Status.INTERNAL_SERVER_ERROR, e);			
		}
	}		

	@GET
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}/{objectId}")
	public Response getById(			
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@PathParam("objectId") String objectId,
			@QueryParam("excludeProperties") String excludeProperties,
			@QueryParam("expandBlankObjects") String expandBlankObjects,
			@QueryParam("filterProperties") String filterProperties,
			@QueryParam("filterObjectTypes") String filterObjectTypes,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers,
			@Context HttpServletRequest request)
	{
		notifyRequest(uriInfo, headers, request);
		
		BooleanParam excludePropertiesParam = new BooleanParam();
		excludePropertiesParam.setStringValue(excludeProperties);
		
		BooleanParam expandBlankObjectsParam = new BooleanParam();
		expandBlankObjectsParam.setStringValue(expandBlankObjects);

		try {		
			Model model = getDataSetObjectManager().getById(
					collectionId,
					dataSourceId,
					dataSetId,
					objectId,
					excludePropertiesParam.getValue(),
					expandBlankObjectsParam.getValue(),
					filterProperties,
					filterObjectTypes);
			String modelBaseUri = NameFormatter.formatObjectResourceBaseUri(collectionId, dataSourceId);
			return DrumbeatResponseBuilder.build(
					Status.OK,
					model,
					modelBaseUri,
					headers.getAcceptableMediaTypes());			
		} catch (NotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		} catch (DrumbeatException e) {
			throw new DrumbeatWebException(Status.INTERNAL_SERVER_ERROR, e);			
		}
	}		

	@GET
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}/{objectId}/type")
	public Response getType(			
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@PathParam("objectId") String objectId,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers,
			@Context HttpServletRequest request)
	{
		notifyRequest(uriInfo, headers, request);
		
		try {		
			Model model = getDataSetObjectManager().getObjectType(collectionId, dataSourceId, dataSetId, objectId);
			String modelBaseUri = NameFormatter.formatObjectResourceBaseUri(collectionId, dataSourceId);
			return DrumbeatResponseBuilder.build(
					Status.OK,
					model,
					modelBaseUri,
					headers.getAcceptableMediaTypes());			
		} catch (NotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		} catch (DrumbeatException e) {
			throw new DrumbeatWebException(Status.INTERNAL_SERVER_ERROR, e);			
		}
	}
	
	

}
