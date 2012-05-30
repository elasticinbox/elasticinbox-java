package com.elasticinbox.rest.v2;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.elasticinbox.core.DAOFactory;
import com.elasticinbox.core.LabelDAO;
import com.elasticinbox.core.MessageDAO;
import com.elasticinbox.core.model.Labels;
import com.elasticinbox.core.model.Mailbox;

/**
 * This JAX-RS resource is responsible for maintaining mailbox consitency
 * through scrub operation.
 * 
 * @author Rustam Aliyev
 */
@Path("{domain}/{user}/mailbox/scrub")
public class ScrubResource
{
	private final MessageDAO messageDAO;
	private final LabelDAO labelDAO;

	@Context UriInfo uriInfo;

	public ScrubResource() {
		DAOFactory dao = DAOFactory.getDAOFactory();
		messageDAO = dao.getMessageDAO();
		labelDAO = dao.getLabelDAO();
	}

	/**
	 * Initialize new account
	 * 
	 * @param account
	 * @return
	 */
	@POST
	@Path("counters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response scrubIndex(
			@PathParam("user") final String user,
			@PathParam("domain") final String domain)
	{
		Mailbox mailbox = new Mailbox(user, domain);
		
		Labels calculatedCounters = messageDAO.calculateCounters(mailbox);
		labelDAO.setCounters(mailbox, calculatedCounters);
		
		return Response.noContent().build();
	}

}
