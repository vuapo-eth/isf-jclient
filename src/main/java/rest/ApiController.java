package rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;    

@Path("resource/{id}")
public class ApiController {

	/**
	 * Call: <code>/service/rest/resource/23</code>
	 * @return HTTP Response
	 */
	@GET
	public Response getResource(@PathParam("id") String anId) {
		return Response.status(Status.OK).entity("Hello" + anId + " World").build();
	}
}