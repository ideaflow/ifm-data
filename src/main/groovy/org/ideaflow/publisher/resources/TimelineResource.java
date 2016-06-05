package org.ideaflow.publisher.resources;

import org.ideaflow.publisher.api.ResourcePaths;
import org.ideaflow.publisher.api.timeline.BandTimeline;
import org.ideaflow.publisher.core.timeline.BandTimelineFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Component
@Path(ResourcePaths.TIMELINE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class TimelineResource {

	@Autowired
	private BandTimelineFactory timelineGenerator;

	@GET
	@Path(ResourcePaths.TASK_PATH + "/{taskId}")
	public BandTimeline getTimelineForTask(@PathParam("taskId") Long taskId) {
		return timelineGenerator.createTaskTimeline(taskId);
	}


//	private IdeaFlowInMemoryPersistenceService persistenceService;
//
//	@GET
//	@Path(ResourcePaths.TASK_PATH + "/{taskId}")
//	public Timeline getTimelineForTask(@PathParam("taskId") String taskId, @QueryParam("userId") String userId) {
//		TimelineGenerator generator = new TimelineGenerator();
//		return generator.createTimeline(persistenceService.getStateList(),
//		                                persistenceService.getIdleTimeBandList(),
//		                                persistenceService.getEventList());
//	}


//
//	@GET
//	@Path(ResourcePaths.DAY_PATH)
//	public Timeline getTimelineForDay(@QueryParam("day") LocalDate day, @QueryParam("userId") String userId) {
//		return new Timeline();
//	}
//
//	@GET
//	@Path(ResourcePaths.DAY_PATH + ResourcePaths.RECENT_PATH)
//	public List<Timeline> getRecentTimelinesForDays(@QueryParam("days") int days, @QueryParam("userId") String userId) {
//		return Collections.emptyList();
//	}
//
//	@GET
//	@Path(ResourcePaths.USER_PATH + ResourcePaths.RECENT_PATH)
//	public List<Timeline> getRecentTimelinesForUser(@QueryParam("userId") String userId) {
//		return Collections.emptyList();
//	}
//
//	@GET
//	@Path(ResourcePaths.PROJECT_PATH + ResourcePaths.RECENT_PATH)
//	public List<Timeline> getRecentTimelinesForProject(@QueryParam("projectId") String projectId) {
//		return Collections.emptyList();
//	}

}
