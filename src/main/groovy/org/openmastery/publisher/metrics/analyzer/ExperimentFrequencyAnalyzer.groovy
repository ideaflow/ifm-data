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
package org.openmastery.publisher.metrics.analyzer

import org.openmastery.publisher.api.ideaflow.IdeaFlowTimeline
import org.openmastery.publisher.api.journey.DiscoveryCycle
import org.openmastery.publisher.api.journey.TroubleshootingJourney
import org.openmastery.publisher.api.metrics.GraphPoint
import org.openmastery.publisher.api.metrics.MetricType
import org.openmastery.storyweb.api.MetricThreshold

/**
 * Look for spikes in experiments/journey as a clue that there might be problems with experiment output
 */
class ExperimentFrequencyAnalyzer extends AbstractTimelineAnalyzer<Double> {

	ExperimentFrequencyAnalyzer() {
		super(MetricType.MAX_EXPERIMENT_CYCLES)
	}
	@Override
	List<GraphPoint<Double>> analyzeTimelineAndJourneys(IdeaFlowTimeline timeline, List<TroubleshootingJourney> journeys) {

		List<GraphPoint<Double>> allPoints = journeys.collect { TroubleshootingJourney journey ->
			GraphPoint<Double> journeyPoint = createPointFromMeasurableContext("/journey", journey)

			journeyPoint.childPoints = generatePointsForDiscoveryCycles(journey.discoveryCycles)
			journeyPoint.frequency = getSumOfFrequency(journeyPoint.childPoints)
			journeyPoint.value = getSumOfValues(journeyPoint.childPoints)
			journeyPoint.danger = isOverThreshold(journeyPoint.value)
			return journeyPoint
		}

		GraphPoint<Double> timelinePoint = createTimelinePoint(timeline, journeys)
		timelinePoint.value = getMaximumValue(allPoints)
		timelinePoint.danger = isOverThreshold(timelinePoint.value)
		allPoints.add(timelinePoint)
		return allPoints
	}

	List<GraphPoint<Double>> generatePointsForDiscoveryCycles(List<DiscoveryCycle> discoveryCycles) {
		discoveryCycles.collect { DiscoveryCycle discoveryCycle ->
			GraphPoint<Double> discoveryPoint = createPointFromMeasurableContext("/discovery", discoveryCycle)
			discoveryPoint.value = Double.valueOf(discoveryCycle.getFrequency())
			discoveryPoint.danger = isOverThreshold(discoveryPoint.value)
			return discoveryPoint
		}
	}


	@Override
	MetricThreshold<Double> getDangerThreshold() {
		return createMetricThreshold(15D)
	}

}
