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
package org.openmastery.publisher.core.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface TaskRepository extends PagingAndSortingRepository<TaskEntity, Long> {

	TaskEntity findByOwnerIdAndName(Long ownerId, String name);

	Page<TaskEntity> findByOwnerId(@Param("ownerId") Long userId, Pageable pageable);

	@Query(nativeQuery = true, value = "select * from task where owner_id=(?1) " +
			"and creation_date <= (?3) and modify_date >= (?2) order by modify_date desc")
	List<TaskEntity> findTasksWithinRange(Long userId, Timestamp startTime, Timestamp endTime);

}
