#!/usr/bin/env sh
java -D$HOSTNAME $JAVA_OPTS \
    -Djava.security.egd=file:/dev/./urandom -cp application:application/BOOT-INF/classes:application/BOOT-INF/lib/*:dependencies/BOOT-INF/lib/*:snapshot-dependencies/BOOT-INF/lib/* \
    org.iromu.trino.graphql.GraphQLTrinoApplication
