package org.iromu.trino.graphql;

import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class GraphQLTrinoApplication {

    @Generated
    public static void main(String[] args) {
        SpringApplication.run(GraphQLTrinoApplication.class, args);
    }

}
