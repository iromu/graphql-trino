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

import java.util.List;

/**
 * Application-level configuration properties for customizing GraphQL schema generation
 * and behavior when integrating with Trino.
 * <p>
 * These properties are loaded from external configuration sources such as
 * {@code application.yml} or {@code application.properties} and are bound via the
 * {@link ConfigurationProperties} annotation using the prefix {@code app}.
 * </p>
 *
 * <p>
 * This configuration class supports customization of:
 * <ul>
 * <li>Filesystem location for storing schemas fetched from Trino</li>
 * <li>Schema sanitization options for invalid GraphQL object names</li>
 * <li>Caching behavior</li>
 * <li>Catalog and schema inclusion/exclusion filtering</li>
 * </ul>
 * </p>
 *
 * <p>
 * Example configuration in {@code application.yml}: <pre>
 * app:
 *   schema-folder: /tmp/schema
 *   replace-objects-name-characters: true
 *   ignore-objects-with-wrong-characters: false
 *   ignore-cache: true
 *   include-catalogs:
 *     - my_catalog
 *   exclude-catalogs:
 *     - system
 *   exclude-schemas:
 *     - information_schema
 * </pre>
 * </p>
 *
 * @author Ivan Rodriguez
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @see org.springframework.context.annotation.Configuration
 */
@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = AppProperties.APP_PREFIX)
public class AppProperties {

	/**
	 * Prefix used to bind properties from external configuration sources.
	 */
	public static final String APP_PREFIX = "app";

	/**
	 * Filesystem directory where the Trino schema definitions will be stored.
	 * <p>
	 * Defaults to {@code /etc/schema}.
	 * </p>
	 */
	private String schemaFolder = "/etc/schema";

	/**
	 * Whether to replace characters that are invalid in GraphQL object names.
	 * <p>
	 * If enabled, invalid characters will be automatically sanitized (e.g., replaced with
	 * underscores) to conform to GraphQL naming rules.
	 * </p>
	 */
	private boolean replaceObjectsNameCharacters = false;

	/**
	 * Whether to skip GraphQL object names that contain invalid characters.
	 * <p>
	 * If enabled, objects with names that do not conform to GraphQL naming standards will
	 * be ignored and excluded from the schema generation process.
	 * </p>
	 */
	private boolean ignoreObjectsWithWrongCharacters = true;

	/**
	 * Whether to bypass caching mechanisms.
	 * <p>
	 * When set to {@code true}, previously stored schema metadata will not be reused and
	 * fresh data will be fetched on each operation. Useful for development or real-time
	 * schema changes.
	 * </p>
	 */
	private boolean ignoreCache = false;

	/**
	 * A list of catalog names to include in scanning and processing.
	 * <p>
	 * If specified, only these catalogs will be considered for schema analysis.
	 * </p>
	 */
	private List<String> includeCatalogs;

	/**
	 * A list of catalog names to exclude from processing.
	 * <p>
	 * Defaults to {@code system} to avoid scanning internal system catalogs.
	 * </p>
	 */
	private List<String> excludeCatalogs = List.of("system");

	/**
	 * A list of schema names to exclude within any catalog.
	 * <p>
	 * Defaults to {@code information_schema}, which is typically excluded as it contains
	 * metadata tables rather than user data.
	 * </p>
	 */
	private List<String> excludeSchemas = List.of("information_schema");

}
