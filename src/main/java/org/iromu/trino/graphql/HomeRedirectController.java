package org.iromu.trino.graphql;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class HomeRedirectController {

    @Bean
    public RouterFunction<ServerResponse> redirectToGraphiql() {
        return route(GET("/"),
                req -> ServerResponse.temporaryRedirect(URI.create(req.uri().getPath() + "graphiql")).build());
    }

}
