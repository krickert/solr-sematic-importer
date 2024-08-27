package com.krickert.search.indexer.controller;

import com.krickert.search.indexer.config.IndexerConfiguration;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller("/api/config")
public class ConfigurationAdminController {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationAdminController.class);

    private final IndexerConfiguration indexerConfiguration;

    public ConfigurationAdminController(IndexerConfiguration indexerConfiguration) {
        this.indexerConfiguration = indexerConfiguration;
        log.info("Created ConfigurationAdminController");
    }

    @Get("/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "retrieve the configuration")
    public IndexerConfiguration getConfig(@QueryValue @Parameter(description = "Name of the configuration we looking up. Will show default config if none is shown.", required = false) @Nullable String configName) {
        if (StringUtils.isNotEmpty(configName)) {
            log.info("returning config for collection {}", configName);
        } else {
            log.info("returning default config");
        }

        return indexerConfiguration;
    }



}
