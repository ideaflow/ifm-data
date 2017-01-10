/*
 * Copyright 2017 New Iron Group, Inc.
 *
 * Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/gpl-3.0.en.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openmastery.storyweb.core.metrics.analyzer

import groovy.util.logging.Slf4j
import org.openmastery.publisher.api.event.ExecutionEvent
import org.openmastery.publisher.api.ideaflow.IdeaFlowBand
import org.openmastery.publisher.api.ideaflow.IdeaFlowStateType
import org.openmastery.publisher.api.ideaflow.IdeaFlowTimeline
import org.openmastery.publisher.api.journey.IdeaFlowStory
import org.openmastery.publisher.api.journey.SubtaskStory
import org.openmastery.publisher.api.journey.TroubleshootingJourney
import org.openmastery.publisher.api.metrics.DurationInSeconds
import org.openmastery.storyweb.api.metrics.GraphPoint
import org.openmastery.publisher.api.metrics.MetricType
import org.openmastery.storyweb.api.metrics.MetricThreshold

@Slf4j
class HaystackAnalyzer extends AbstractTimelineAnalyzer<DurationInSeconds> {

	HaystackAnalyzer() {
		super(MetricType.MAX_HAYSTACK_SIZE, true)
	}

	@Override
	GraphPoint<DurationInSeconds> analyzeIdeaFlowStory(IdeaFlowTimeline timeline, List<TroubleshootingJourney> journeys) {

		//exclude learning time, generally trailing troubleshooting time, but could be a whole string of haystacks...
		//maybe we get the haystack size

		Map<String, Long> maxHaystacksFound = [:]

		List<IdeaFlowBand> consecutiveBandPeriods = collapseConsecutiveBandPeriods(timeline.ideaFlowBands)
		log.debug("Collapsed bands:" + consecutiveBandPeriods )

		consecutiveBandPeriods.each { IdeaFlowBand band ->
			Long relativeStart = band.relativePositionInSeconds
			Long relativeEnd = relativeStart + band.durationInSeconds

			List<Long> relativeEventTimes = findRelativePositionsWithinRange(timeline.executionEvents, relativeStart, relativeEnd)

			Long previousTime = null

			relativeEventTimes.each { Long currentTime ->
				if (previousTime == null) {
					previousTime = currentTime
				}

				Long haystackSizeInSeconds = currentTime - previousTime
				log.debug("Haystack calculation [$previousTime to $currentTime] = haystack: $haystackSizeInSeconds")
				assignBlame(maxHaystacksFound, journeys, currentTime, haystackSizeInSeconds)
				previousTime = currentTime
			}
		}

		return translateToGraphPoints(timeline, journeys, maxHaystacksFound)

	}

	@Override
	GraphPoint<DurationInSeconds> assignBlameToSubtasks(IdeaFlowStory ideaFlowStory, GraphPoint<DurationInSeconds> summaryPoint) {
		List<GraphPoint<DurationInSeconds>> subtaskPoints = []

		ideaFlowStory.subtasks.each { SubtaskStory subtaskStory ->
			List<String> journeyPaths = subtaskStory.collect { it.relativePath }
			List<GraphPoint<DurationInSeconds>> journeyPoints = []
			summaryPoint.childPoints.each { GraphPoint<DurationInSeconds> point ->
				if (journeyPaths.contains(point.relativePath)) {
					journeyPoints.add(point)
				}
			}

			GraphPoint graphPoint = createAggregatePoint(subtaskStory.timeline, journeyPoints)
			if (graphPoint) {
				graphPoint.relativePath = subtaskStory.relativePath
				graphPoint.contextTags = subtaskStory.contextTags
				subtaskPoints.add(graphPoint)
			}
		}

		summaryPoint.childPoints = subtaskPoints

		return summaryPoint
	}



	GraphPoint<DurationInSeconds> translateToGraphPoints(IdeaFlowTimeline timeline, List<TroubleshootingJourney> journeys, Map<String, Long> maxHaystacksFound) {

		List<GraphPoint<DurationInSeconds>> allPoints = []

		maxHaystacksFound.each { String relativePath, Long haystackSizeInSeconds ->
			TroubleshootingJourney journey = findJourney(journeys, relativePath)

			if (journey) {
				GraphPoint<DurationInSeconds> graphPoint = createPointFromStoryElement(journey)
				graphPoint.value = new DurationInSeconds(haystackSizeInSeconds)
				graphPoint.danger = isOverThreshold(graphPoint.value)
				allPoints.add(graphPoint)
			}
		}

		allPoints = allPoints.sort { GraphPoint<DurationInSeconds> point ->
			point.relativePositionInSeconds
		}

		GraphPoint<DurationInSeconds> timelinePoint = createTimelinePoint(timeline, allPoints)
		Long biggestHaystack = maxHaystacksFound.get("/timeline")
		if (biggestHaystack) {
			timelinePoint.value = new DurationInSeconds(biggestHaystack)
		} else {
			timelinePoint.value = new DurationInSeconds(0)
		}
		timelinePoint.danger = isOverThreshold(timelinePoint.value)
		timelinePoint.childPoints = allPoints

		return timelinePoint
	}

	@Override
	GraphPoint<DurationInSeconds> createAggregatePoint(IdeaFlowTimeline timeline, List<GraphPoint<DurationInSeconds>> allPoints) {
		GraphPoint timelinePoint = null
		if (allPoints.size() > 0) {
			timelinePoint = createTimelinePoint(timeline, allPoints)
			timelinePoint.value = getMaximumValue(allPoints)
			timelinePoint.danger = isOverThreshold(timelinePoint.value)
		}
		return timelinePoint
	}

	TroubleshootingJourney findJourney(List<TroubleshootingJourney> journeys, String relativePath) {
		journeys.find { TroubleshootingJourney journey ->
			journey.relativePath == relativePath
		}
	}

	Map<String, Long> assignBlame(Map<String, Long> maxHaystacksFound, List<TroubleshootingJourney> journeys, long haystackEnd, long haystackSizeInSeconds) {

		for (TroubleshootingJourney journey : journeys) {
			if (haystackEnd <= journey.relativeEnd) {
				Long oldHaystack = maxHaystacksFound.get(journey.getRelativePath())
				if (oldHaystack == null || haystackSizeInSeconds > oldHaystack) {
					maxHaystacksFound.put(journey.relativePath, haystackSizeInSeconds)
				}
				break;
			}

			long haystackStart = haystackEnd - haystackSizeInSeconds
			if (haystackStart < journey.relativeEnd) {
				long partialHaystackSize = journey.relativeEnd - haystackStart
				adjustMaximumHaystack(maxHaystacksFound, journey, partialHaystackSize)
			}
		}
		Long oldHaystack = maxHaystacksFound.get("/timeline")
		if (oldHaystack == null || haystackSizeInSeconds > oldHaystack) {
			log.debug("Recording new maximum haystack size: "+haystackSizeInSeconds)
			maxHaystacksFound.put("/timeline", haystackSizeInSeconds)
		}

		return maxHaystacksFound;
	}

	private void adjustMaximumHaystack(Map<String, Long> maxHaystacksFound, TroubleshootingJourney journey, long newHaystackSize) {
		Long oldHaystack = maxHaystacksFound.get(journey.getRelativePath())
		if (oldHaystack == null || newHaystackSize > oldHaystack) {
			maxHaystacksFound.put(journey.relativePath, newHaystackSize)
		}
	}

	@Override
	MetricThreshold<DurationInSeconds> getDangerThreshold() {
		return createMetricThreshold(new DurationInSeconds(60 * 60))
	}


	private List<IdeaFlowBand> collapseConsecutiveBandPeriods(List<IdeaFlowBand> bands) {
		log.debug("Before collapsed bands : "+bands)
		List<IdeaFlowBand> filteredBands = bands.findAll() { IdeaFlowBand band ->
			(band.type == IdeaFlowStateType.PROGRESS || band.type == IdeaFlowStateType.TROUBLESHOOTING)
		}

		filteredBands = filteredBands.sort { IdeaFlowBand band ->
			band.relativePositionInSeconds
		}

		log.debug("after filtered bands : "+filteredBands)

		List<IdeaFlowBand> consecutiveBandPeriods = []
		IdeaFlowBand lastBand = null
		filteredBands.each { IdeaFlowBand band ->
			if (lastBand == null) {
				log.debug("lastBand is null")
				lastBand = band
			} else {
				if (bandsAreAdjacent(lastBand, band)) {
					log.debug("Bands are adjacent: "+lastBand.relativeEnd + ", "+band.relativeStart)
					IdeaFlowBand newBand = IdeaFlowBand.builder()
							.relativePositionInSeconds(lastBand.relativePositionInSeconds)
							.durationInSeconds(lastBand.durationInSeconds + band.durationInSeconds)
							.build()
					lastBand = newBand
				} else {
					log.debug("Bands have a gap: "+lastBand.relativeEnd + ", "+band.relativeStart)
					consecutiveBandPeriods.add(lastBand)
					lastBand = band
				}
			}
		}
		if (lastBand) {
			consecutiveBandPeriods.add(lastBand)
		}

		return consecutiveBandPeriods
	}

	private boolean bandsAreAdjacent(IdeaFlowBand prevBand, IdeaFlowBand nextBand) {
		Long prevBandEnd = prevBand.relativePositionInSeconds + prevBand.durationInSeconds
		return (prevBandEnd == nextBand.relativePositionInSeconds)
	}


	private List<Long> findRelativePositionsWithinRange(List<ExecutionEvent> allEvents, Long relativeStart, Long relativeEnd) {
		List<ExecutionEvent> eventsWithinBand = findEventsWithinRange(allEvents, relativeStart, relativeEnd)
		List<Long> relativeTimes = createRelativePositionsList(eventsWithinBand)
		relativeTimes.add(relativeStart)
		relativeTimes.add(relativeEnd)

		Collections.sort(relativeTimes)
		return relativeTimes
	}

	private List<Long> createRelativePositionsList(List<ExecutionEvent> events) {
		List<Long> relativeTimes = events.collect { ExecutionEvent event ->
			event.relativePositionInSeconds
		}
		Collections.sort(relativeTimes)
		return relativeTimes
	}


	private List<ExecutionEvent> findEventsWithinRange(List<ExecutionEvent> events, Long relativeStart, Long relativeEnd) {
		return events.findAll() { ExecutionEvent event ->
			event.relativePositionInSeconds > relativeStart && event.relativePositionInSeconds < relativeEnd
		}
	}

}
