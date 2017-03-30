/**
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
package org.openmastery.storyweb.resources;


import org.openmastery.publisher.api.PagedResult;
import org.openmastery.storyweb.api.FaqSummary;
import org.openmastery.storyweb.api.PainPoint;
import org.openmastery.storyweb.api.ResourcePaths;
import org.openmastery.storyweb.core.MetricsService;
import org.openmastery.storyweb.core.StoryWebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Component
@Path(ResourcePaths.STORY_WEB_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class StoryWebResource {


	@Autowired
	MetricsService metricsService;

	/**
	 * Returns the most painful points in the StoryWeb, optionally filterable by tag
	 * @param tags Can be contextTags, painTags or a mix of both
	 * @return List<PainPoint>
	 */

	@GET
	public PagedResult<PainPoint> findPainfulStoryPoints(@QueryParam("project") String project,
												  @DefaultValue("0") @QueryParam("page_number") Integer pageNumber,
												  @DefaultValue("10") @QueryParam("per_page") Integer elementsPerPage,
												  @QueryParam("tag") List<String> tags) {
		List<PainPoint> painPoints = metricsService.findAndFilterBiggestPainPoints(tags);

		PagedResult<PainPoint> pagedResult = new PagedResult<PainPoint>();
		int firstIndex = pageNumber * elementsPerPage;
		int lastIndex = Math.min(painPoints.size() - 1, ((pageNumber + 1) * elementsPerPage));
		int numPages = painPoints.size() / elementsPerPage + ((painPoints.size() % elementsPerPage) > 0? 1 : 0);

		pagedResult.setContents(painPoints.subList(firstIndex, lastIndex));
		pagedResult.setPageNumber(pageNumber);
		pagedResult.setElementsPerPage(elementsPerPage);
		pagedResult.setTotalPages(numPages);
		pagedResult.setHasNext(pageNumber < numPages - 1);
		pagedResult.setHasPrevious(pageNumber > 0);

		return pagedResult;
	}




}
