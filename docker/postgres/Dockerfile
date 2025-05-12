FROM postgres:alpine
USER root

# database credentials
ARG POSTGRES_USER=postgres POSTGRES_PASSWORD=postgres POSTGRES_DB=example
ENV POSTGRES_USER=${POSTGRES_USER} POSTGRES_PASSWORD=${POSTGRES_PASSWORD} POSTGRES_DB=${POSTGRES_DB}

# which schema should be used
ARG SCHEMA_FOLDER="stocks"

# copy the necessary files
ENV IMPORT_SCRIPT="import.sh" SCHEMA_SCRIPT="schema.sql" DATA_FOLDER="data"

RUN mkdir $DATA_FOLDER

COPY "stocks"/$SCHEMA_SCRIPT /docker-entrypoint-initdb.d/001_stocks_$SCHEMA_SCRIPT
COPY $IMPORT_SCRIPT /docker-entrypoint-initdb.d/002_stocks_$IMPORT_SCRIPT
COPY "stocks"/$DATA_FOLDER/* $DATA_FOLDER/

COPY "movies"/$SCHEMA_SCRIPT /docker-entrypoint-initdb.d/001_movies_$SCHEMA_SCRIPT
COPY $IMPORT_SCRIPT /docker-entrypoint-initdb.d/002_movies_$IMPORT_SCRIPT
COPY "movies"/$DATA_FOLDER/* $DATA_FOLDER/

USER postgres
