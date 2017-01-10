package org.openmastery.publisher.api.journey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.joda.time.LocalDateTime;
import org.openmastery.publisher.api.AbstractRelativeInterval;
import org.openmastery.publisher.api.event.Event;
import org.openmastery.publisher.api.event.EventType;
import org.openmastery.publisher.api.metrics.CapacityDistribution;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ProgressMilestone extends AbstractRelativeInterval implements StoryContextElement {

	@JsonIgnore
	Event event;

	@JsonIgnore
	String parentPath;

	CapacityDistribution capacityDistribution;

	Set<String> painTags;
	Set<String> contextTags;


	public ProgressMilestone(String parentPath, Event progressEvent) {
		this.event = progressEvent;
		this.parentPath = parentPath;
		setRelativeStart(progressEvent.getRelativePositionInSeconds());

		contextTags = TagsUtil.extractUniqueHashTags(progressEvent.getComment());
		painTags = new HashSet<String>();

		//TODO fix this hack properly in IdeaFlowStoryGenerator... overwriting subtask for default milestone
		if (event.getType() == EventType.NOTE) {
			event.setFullPath(getFullPath());
		}
	}

	@JsonIgnore
	public Long getId() {
		return event.getId();
	}

	public String getRelativePath() {
		return "/milestone/" + event.getId();
	}

	@JsonIgnore
	public String getFullPath() {
		return parentPath + getRelativePath();
	}

	public LocalDateTime getPosition() {
		return event.getPosition();
	}

	public String getDescription() {
		return event.getComment();
	}

	@JsonIgnore
	public LocalDateTime getStart() {
		return event.getPosition();
	}



	@Override
	public int getFrequency() {
		return 1;
	}

	@JsonIgnore
	@Override
	public List<? extends StoryElement> getChildStoryElements() {
		return Collections.emptyList();
	}


}
