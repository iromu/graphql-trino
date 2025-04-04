# Trino GraphQL

A Spring Boot application that dynamically generates a GraphQL schema from Trino catalogs, schemas, and tables. It
allows users to explore and query Trino data sources using GraphQL without manually writing schema definitions.

## 🚀 Features

- **Dynamic GraphQL Schema**: Automatically scans Trino catalogs, schemas, and tables and exposes them as GraphQL
  queries.
- **Auto-detect Columns**: Generates GraphQL object types from Trino table column metadata.
- **Filter Support**: Query data with dynamic filters using GraphQL input arguments.
- **Streamed Results**: Efficient streaming of query results from Trino using JDBC.
- **GraphiQL Interface**: Visual GraphQL playground is exposed at the root URL `/` for easy testing and exploration.
- **Export Schema**: Exposes a REST endpoint to download the auto-generated GraphQL schema in SDL format.

## 🧠 How It Works

1. On startup, the app connects to Trino via JDBC.
2. It fetches available catalogs, schemas, tables, and columns.
3. GraphQL schema is dynamically generated using this metadata.
4. Each table is exposed as a top-level query.
5. Query fields support filters using a generic `FilterInput` object.
6. Queries return streamed `Map<String, Object>` rows to minimize memory usage.

## 🔧 Configuration

Set up your connection to Trino in `application.yml` or `application.properties`:

```yaml
spring:
  datasource:
    url: jdbc:trino://localhost:8080
    username: your-trino-user
    driver-class-name: io.trino.jdbc.TrinoDriver
```

## 🧪 Example GraphQL Query

```graphql
query {
    my_table(limit: 10, filters: [
        { field: "age", operator: "gt", intValue: 30 },
        { field: "name", operator: "like", stringValue: "Ali" }
    ]) {
        id
        name
        age
    }
}
```
