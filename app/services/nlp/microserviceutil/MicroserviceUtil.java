package services.nlp.microserviceutil;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;

public class MicroserviceUtil {

	public static JsonNode getJsonFromMessageBody(Response response) {

		String result = response.readEntity(String.class);
		JsonNode resultNode = Json.parse(result);
		return resultNode;
			
	}
}
