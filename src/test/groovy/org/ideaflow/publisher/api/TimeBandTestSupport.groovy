package org.ideaflow.publisher.api

import org.ideaflow.publisher.core.activity.IdleTimeBand

import java.time.Duration
import java.time.LocalDateTime


trait TimeBandTestSupport {

	IdeaFlowBand createBand(LocalDateTime start, LocalDateTime end) {
		IdeaFlowBand.builder()
				.start(start)
				.end(end)
				.idle(Duration.ZERO)
				.nestedBands([])
				.build()
	}

	IdleTimeBand createIdle(LocalDateTime start, LocalDateTime end) {
		IdleTimeBand.builder()
				.start(start)
				.end(end)
				.build()
	}


	TimeBandGroup createGroup(LocalDateTime start, LocalDateTime end) {
		createGroup(createBand(start, end))
	}

	TimeBandGroup createGroup(IdeaFlowBand... bands) {
		TimeBandGroup.builder()
				.linkedTimeBands(bands as List)
				.build()
	}

	void assertStartAndEnd(TimeBand band, LocalDateTime expectedStart, LocalDateTime expectedEnd) {
		assert band.start == expectedStart
		assert band.end == expectedEnd
	}

}
