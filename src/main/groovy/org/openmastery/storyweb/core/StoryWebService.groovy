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
package org.openmastery.storyweb.core

import com.fasterxml.jackson.databind.ObjectMapper
import org.openmastery.mapper.EntityMapper
import org.openmastery.publisher.core.annotation.AnnotationRespository
import org.openmastery.publisher.security.InvocationContext
import org.openmastery.storyweb.api.FaqSummary
import org.openmastery.publisher.api.journey.TagsUtil
import org.openmastery.time.TimeConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import java.sql.Timestamp

@Component
class StoryWebService {

	@Autowired
	AnnotationRespository annotationRepository


	@Autowired
	InvocationContext invocationContext

	private EntityMapper entityMapper = new EntityMapper();

	private ObjectMapper jsonMapper = new ObjectMapper()


	public List<FaqSummary> findAllFaqMatchingTags(List<String> tags) {
		String searchPattern = SearchUtils.createSearchPattern(tags)

		List<Object[]> results = annotationRepository.findFaqsBySearchCriteria(searchPattern)
		List<FaqSummary> faqSummaries = results.collect { Object[] row ->
			FaqSummary faqSummary = new FaqSummary()
			faqSummary.taskId = (Long) row[0]
			faqSummary.eventId = (Long) row[1]
			faqSummary.eventComment = row[2]
			faqSummary.faqComment = extractCommentFromJSON(row[3].toString())
			faqSummary.position = TimeConverter.toJodaLocalDateTime((Timestamp) row[4])
			faqSummary.tags = extractUniqueTags(faqSummary.eventComment, faqSummary.faqComment)
			return faqSummary
		}

		return faqSummaries;
	}



	private Set<String> extractUniqueTags(String eventComment, String faqComment) {
		Set<String> tagSet = []
		tagSet.addAll(TagsUtil.extractUniqueHashTags(eventComment))
		tagSet.addAll(TagsUtil.extractUniqueHashTags(faqComment))

		return tagSet
	}

	private String extractCommentFromJSON(String jsonMetadata) {
		CommentHolder commentHolder = jsonMapper.readValue(jsonMetadata, CommentHolder.class)
		return commentHolder.comment
	}


	private static class CommentHolder {
		String comment;

		CommentHolder() {}
	}

}
