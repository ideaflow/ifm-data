package org.openmastery.publisher.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.LocalDateTime;
import org.openmastery.publisher.api.Positionable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractPositionable implements Positionable {

	private Long taskId;
	private LocalDateTime position;
	private Long relativePositionInSeconds;


// simplify dozer mapping

	@JsonIgnore
	public LocalDateTime getStart() {
		return position;
	}

	@JsonIgnore
	public void setStart(LocalDateTime position) {
		this.position = position;
	}
}
