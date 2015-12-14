package fi.aalto.cs.drumbeat.rest.common;

import java.util.List;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.hp.hpl.jena.rdf.model.Model;

public class DrumbeatResponseBuilder {
	
	public static Response build(
			Status status,
			Model model,
			List<MediaType> acceptableMediaTypes) {
		
		for (MediaType mediaType : acceptableMediaTypes) {
			
			try {
			
				String entity = ModelToMediaTypeConverter.convert(model, mediaType);
				return Response
						.status(status)
						.entity(entity)
						.type(mediaType)
						.build();
				
			}catch (NotSupportedException e) {				
			}
			
		}
		
		throw new DrumbeatWebException(
				Response.Status.UNSUPPORTED_MEDIA_TYPE,
				String.format(
						"Use supported media types: %s",
						ModelToMediaTypeConverter.getSupportedMediaTypes().toString()),
				null);
	}
	
}