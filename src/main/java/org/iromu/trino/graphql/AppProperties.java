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
 * Configuration properties for the application.
 *
 * <p>
 * This class holds configuration properties used by the application. It is annotated with
 * {@link ConfigurationProperties} to bind properties prefixed with "app" from the
 * application's configuration (e.g., `application.properties` or `application.yml`).
 * </p>
 *
 * <p>
 * The properties in this class are mainly related to the configuration of the schema
 * storage location, character sanitization for GraphQL schema objects, and caching
 * behavior. These settings help configure the system's behavior with respect to schema
 * handling and the handling of object names in GraphQL schemas.
 * </p>
 *
 * @author Ivan Rodriguez
 */
@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = AppProperties.APP_PREFIX)
public class AppProperties {

	/**
	 * Prefix used in application properties for binding.
	 */
	public static final String APP_PREFIX = "app";

	/**
	 * Filesystem location to store the schema from Trino.
	 */
	private String schemaFolder = "/etc/schema";

	/**
	 * Indicates whether characters that are not allowed in GraphQL schema object names
	 * should be replaced.
	 *
	 * <p>
	 * If this flag is set to {@code true}, characters in schema object names that are not
	 * allowed by GraphQL standards will be replaced with valid characters. This may be
	 * useful when dynamically generating GraphQL schemas based on external data.
	 * </p>
	 */
	private boolean replaceObjectsNameCharacters = false;

	/**
	 * Indicates whether to ignore object names with characters that are not allowed in
	 * GraphQL schema.
	 *
	 * <p>
	 * If set to {@code true}, object names with invalid characters will be ignored (i.e.,
	 * skipped during processing). This can help avoid issues with invalid names in
	 * dynamically generated schemas.
	 * </p>
	 */
	private boolean ignoreObjectsWithWrongCharacters = true;

	/**
	 * Indicates whether caching should be ignored.
	 *
	 * <p>
	 * If set to {@code true}, caching mechanisms will be disabled. The default is
	 * {@code false}, meaning caching is enabled.
	 * </p>
	 */
	private boolean ignoreCache = false;

}
