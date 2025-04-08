/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.iromu.trino.graphql;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivan Rodriguez
 */
@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = AppProperties.APP_PREFIX)
public class AppProperties {

	/**
	 * Prefix for Spring properties.
	 */
	public static final String APP_PREFIX = "app";

	/**
	 * Filesystem location to store the schema from Trino.
	 */
	private String schemaFolder = "/etc/schema";

	/**
	 * Replaces characters that are not allowed on GraphLQ Schema.
	 */
	private boolean replaceObjectsNameCharacters = false;

	/**
	 * Ignores object names with characters that are not allowed on GraphLQ Schema.
	 */
	private boolean ignoreObjectsWithWrongCharacters = true;

}
