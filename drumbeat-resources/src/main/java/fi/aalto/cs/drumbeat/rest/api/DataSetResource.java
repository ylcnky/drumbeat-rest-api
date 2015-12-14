package fi.aalto.cs.drumbeat.rest.api;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.AlreadyExistsException;
import com.hp.hpl.jena.shared.NotFoundException;

import fi.aalto.cs.drumbeat.rest.common.DrumbeatApplication;
import fi.aalto.cs.drumbeat.rest.common.DrumbeatResponseBuilder;
import fi.aalto.cs.drumbeat.rest.common.DrumbeatWebException;
import fi.aalto.cs.drumbeat.rest.managers.DataSetContentManager;
import fi.aalto.cs.drumbeat.rest.managers.DataSetManager;
import fi.aalto.cs.drumbeat.rest.managers.ErrorFactory;
import fi.aalto.cs.drumbeat.rest.ontology.LinkedBuildingDataOntology;
import fi.hut.cs.drumbeat.common.DrumbeatException;
import fi.hut.cs.drumbeat.common.file.FileManager;
import fi.hut.cs.drumbeat.ifc.convert.stff2ifc.IfcParserException;


@Path("/datasets")
public class DataSetResource {

	public static final String DATA_TYPE_IFC = "IFC";
	public static final String DATA_TYPE_RDF = "RDF";
	public static final String DATA_TYPE_CSV = "CSV";
	
	private static final Logger logger = Logger.getLogger(DataSetResource.class);
	
	
	@GET
	@Path("/{collectionId}/{dataSourceId}")
	public Response getAll(			
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers)
	{
		DrumbeatApplication.getInstance().notifyRequest(uriInfo);
		
		try {		
			Model model = getDataSetManager().getAll(collectionId, dataSourceId);
			return DrumbeatResponseBuilder.build(
					Status.OK,
					model,
					headers.getAcceptableMediaTypes());			
		} catch (NotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		}
	}
	
	@GET
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}")
	public Response getById(			
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers)
	{
		DrumbeatApplication.getInstance().notifyRequest(uriInfo);
		
		try {		
			Model model = getDataSetManager().getById(collectionId, dataSourceId, dataSetId);
			return DrumbeatResponseBuilder.build(
					Status.OK,
					model,
					headers.getAcceptableMediaTypes());			
		} catch (NotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		}
	}
	
	@DELETE
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}")
	public void delete(			
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers)
	{
		DrumbeatApplication.getInstance().notifyRequest(uriInfo);
		
		try {		
			getDataSetManager().delete(collectionId, dataSourceId, dataSetId);
		} catch (NotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		}
	}
	
	@POST
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response create(			
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@FormParam("name") String name,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers)
	{
		DrumbeatApplication.getInstance().notifyRequest(uriInfo);
		
		try {
			Model model = getDataSetManager().create(collectionId, dataSourceId, dataSetId, name);
			return DrumbeatResponseBuilder.build(
					Status.CREATED,
					model,
					headers.getAcceptableMediaTypes());			
		} catch (NotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		} catch (AlreadyExistsException e) {
			throw new DrumbeatWebException(Status.CONFLICT, e);			
		}
	}	
	
	
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}/uploadServerFile")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> uploadServerFile(
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@FormParam("dataType") String dataType,
			@FormParam("dataFormat") String dataFormat,
			@FormParam("filePath") String filePath,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers)
	{
		DrumbeatApplication.getInstance().notifyRequest(uriInfo);

		String dataSetName = LinkedBuildingDataOntology.formatDataSetName(collectionId, dataSourceId, dataSetId);
		logger.info(String.format("UploadServerFile: DataSet=%s, ServerFilePath=%s", dataSetName, filePath));
		
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		}
		
		return internalUploadDataSet(collectionId, dataSourceId, dataSetId, dataType, dataFormat, inputStream);
	}
	
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}/uploadUrl")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> uploadUrl(
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@FormParam("dataType") String dataType,
			@FormParam("dataFormat") String dataFormat,
			@FormParam("url") String url,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers)
	{
		DrumbeatApplication.getInstance().notifyRequest(uriInfo);

		String dataSetName = LinkedBuildingDataOntology.formatDataSetName(collectionId, dataSourceId, dataSetId);			
		logger.info(String.format("UploadUrl: DataSet=%s, Url=%s", dataSetName, url));
		
		InputStream inputStream;
		try {
			inputStream = new URL(url).openStream();
		} catch (IOException e) {			
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		}
		
		return internalUploadDataSet(collectionId, dataSourceId, dataSetId, dataType, dataFormat, inputStream);
	}


	@Path("/{collectionId}/{dataSourceId}/{dataSetId}/uploadContent")
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> uploadContent(
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@FormParam("dataType") String dataType,
			@FormParam("dataFormat") String dataFormat,			
			@FormParam("content") String content,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers)
	{
		DrumbeatApplication.getInstance().notifyRequest(uriInfo);

		String dataSetName = LinkedBuildingDataOntology.formatDataSetName(collectionId, dataSourceId, dataSetId);			
		logger.info(String.format("UploadContent: DataSet=%s, Content=%s", dataSetName, content));
		
		InputStream inputStream = new ByteArrayInputStream(content.getBytes());
		return internalUploadDataSet(collectionId, dataSourceId, dataSetId, dataType, dataFormat, inputStream);
	}

	
	@Path("/{collectionId}/{dataSourceId}/{dataSetId}/uploadClientFile")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> uploadClientFile(
			@PathParam("collectionId") String collectionId,
			@PathParam("dataSourceId") String dataSourceId,
			@PathParam("dataSetId") String dataSetId,
			@FormDataParam("dataType") String dataType,
			@FormDataParam("dataFormat") String dataFormat,
			@FormDataParam("file") InputStream inputStream,
	        @FormDataParam("file") FormDataContentDisposition fileDetail,
			@Context UriInfo uriInfo,
			@Context HttpHeaders headers)
	{
		DrumbeatApplication.getInstance().notifyRequest(uriInfo);
	        
		String dataSetName = LinkedBuildingDataOntology.formatDataSetName(collectionId, dataSourceId, dataSetId);
		logger.info(String.format("UploadContent: DataSet=%s, FileName=%s", dataSetName, fileDetail.getFileName()));		
		return internalUploadDataSet(collectionId, dataSourceId, dataSetId, dataType, dataFormat, inputStream);
	}
	
	private String getUploadFilePath(String dataSetName, String dataType, String dataFormat) {
		return String.format("%s/%s/%s.%s",
				DrumbeatApplication.getInstance().getRealPath(DrumbeatApplication.Paths.UPLOADS_FOLDER_PATH),
				dataType,
				dataSetName,
				dataFormat);
	}
	
	
	private Map<String, Object> internalUploadDataSet(
			String collectionId,
			String dataSourceId,
			String dataSetId,
			String dataType,
			String dataFormat,
			InputStream inputStream)
	{	
		
		try {
			DataSetManager dataSetManager = new DataSetManager();			

			String dataSetName = LinkedBuildingDataOntology.formatDataSetName(collectionId, dataSourceId, dataSetId);
			if (!dataSetManager.checkExists(collectionId, dataSourceId, dataSetId)) {
				throw ErrorFactory.createDataSetNotFoundException(collectionId, dataSourceId, dataSetId);
			}
			
			if (DrumbeatApplication.getInstance().getSaveUploads()) {
				String outputFilePath = getUploadFilePath(dataSetName, dataType, dataFormat); 
				OutputStream outputStream = FileManager.createFileOutputStream(outputFilePath);
				IOUtils.copy(inputStream, outputStream);
				inputStream.close();
				outputStream.close();
				
				inputStream = new FileInputStream(outputFilePath);
			}

			Model targetDataSetModel = DrumbeatApplication.getInstance().getDataModel(dataSetName);
			
			Map<String, Object> responseEntity = new HashMap<>();
			responseEntity.put("dataSetName", dataSetName);
			responseEntity.put("oldSize", targetDataSetModel.size());
			
			if (dataType.equalsIgnoreCase(DATA_TYPE_IFC)) {
				try {
					targetDataSetModel = new DataSetContentManager().uploadIfcData(inputStream, targetDataSetModel);
				} catch (IfcParserException ifcException) {
					throw new DrumbeatWebException(
							Status.BAD_REQUEST,
							String.format("Invalid IFC data: " + ifcException.getMessage()),
							ifcException);
				}
			} else if (dataType.equalsIgnoreCase(DATA_TYPE_RDF)) {
				try {
					// TODO: convert dataFormat string to Lang
					targetDataSetModel = new DataSetContentManager().uploadRdfData(inputStream, Lang.TURTLE, targetDataSetModel);
				} catch (RiotException riotException) {
					throw new DrumbeatWebException(
							Status.BAD_REQUEST,
							String.format("Invalid RDF data: " + riotException.getMessage()),
							riotException);					
				}
			} else {
				throw new DrumbeatWebException(
						Status.BAD_REQUEST,
						String.format("Unknown data type=%s", dataType),
						null);
			}
			
			responseEntity.put("newSize", targetDataSetModel.size());
			
			return responseEntity;

		} catch (DrumbeatWebException drumbeatWebException) {
			throw drumbeatWebException;
		} catch (NotFoundException e) {
			throw new DrumbeatWebException(Status.NOT_FOUND, e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			
			throw new DrumbeatWebException(
					Status.INTERNAL_SERVER_ERROR,
					String.format("Unexpected error: %s", e.getMessage()),
					e);

		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
		
		
	}
	
	
	private DataSetManager getDataSetManager() {
		try {
			return new DataSetManager();
		} catch (DrumbeatException e) {
			logger.error(e);
			throw new DrumbeatWebException(
					Status.INTERNAL_SERVER_ERROR,
					"Error getting DataSetManager instance: " + e.getMessage(),
					e);
		}
	}
	
	
}