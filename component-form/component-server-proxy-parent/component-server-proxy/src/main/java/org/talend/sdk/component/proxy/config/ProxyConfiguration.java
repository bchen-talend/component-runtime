/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.proxy.config;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.NONE;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Invocation;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.Getter;

@Getter
@ApplicationScoped
public class ProxyConfiguration {

    private static final String PREFIX = "talend.component.proxy.";

    @Inject
    @Documentation("The base to contact the remote server "
            + "(NOTE: it is recommanded to put a load balancer if you have multiple instances.)")
    @ConfigProperty(name = PREFIX + "server.base")
    private String targetServerBase;

    @Inject
    @Documentation("The connect timeout for the communication with the server.base in ms.")
    @ConfigProperty(name = PREFIX + "client.timeouts.connect", defaultValue = "60000")
    private Long connectTimeout;

    @Inject
    @Documentation("The read timeout for the communication with the server.base in ms.")
    @ConfigProperty(name = PREFIX + "client.timeouts.read", defaultValue = "600000")
    private Long readTimeout;

    @Inject
    @Documentation("List of JAX-RS providers to register on the client, at least a JSON-B one should be here.")
    @ConfigProperty(name = PREFIX + "client.providers")
    private List<Class> clientProviders;

    @Inject
    @Getter(NONE)
    @Documentation("The headers to append to the request when contacting the server. Format is a properties one. "
            + "You can put a hardcoded value or a placeholder (`${key}`)."
            + "In this case it will be read from the request attributes and headers.")
    @ConfigProperty(name = PREFIX + "processing.headers")
    private Optional<String> headers;

    @Inject
    @Documentation("An optional location (absolute or resolved from `APP_HOME` environment variable). "
            + "It can take an optional query parameter `force` which specifies if the startup should fail if the  "
            + "file is not resolved. The resolution is done per configuration type (`datastore`, `dataset`, ...) "
            + "but fallbacks on `default` type if the file is not found.\n\nThe values can be keys in the resource bundle "
            + "`org.talend.sdk.component.proxy.enrichment.i18n.Messages`. Use that for display names, placeholders etc..."
            + "The content ")
    @ConfigProperty(name = PREFIX + "processing.uiSpec.patch",
            defaultValue = "component-uispec-metadata.%s.json?force=false")
    private String uiSpecPatchLocation;

    @Inject
    @Documentation("A home location for relative path resolution (optional).")
    @ConfigProperty(name = PREFIX + "application.home", defaultValue = "${playx.application.home}")
    private String home;

    @Inject
    @Documentation("If true the proposable (suggestion lists only depending on the server state) will be cached, otherwise they will be "
            + "requested for each form rendering.")
    @ConfigProperty(name = PREFIX + "actions.proposable.cached", defaultValue = "true")
    private Boolean cacheProposables;

    @Inject
    @Documentation("Should the server use jcache to store catalog information and refresh it with some polling. "
            + "If so the keys `" + PREFIX + "jcache.caches.$cacheName.expiry.duration`, `" + PREFIX
            + "jcache.caches.$cacheName.management.active` and " + "`" + PREFIX
            + "jcache.caches.$cacheName.statistics.active` will be read to create a JCache `MutableConfiguration`. Also note that if all the caches"
            + "share the same configuration you can ignore the `$cacheName` layer.")
    @ConfigProperty(name = PREFIX + "jcache.active", defaultValue = "true")
    private Boolean jcacheActive;

    @Inject
    @Documentation("Caching provider implementation to use (only set it if ambiguous).")
    @ConfigProperty(name = PREFIX + "jcache.provider")
    private Optional<String> jcacheProvider;

    @Inject
    @Documentation("Number of seconds used to check if the server must be refreshed.")
    @ConfigProperty(name = PREFIX + "jcache.refresh.period", defaultValue = "60")
    private Long jcacheRefreshPeriod;

    @Getter
    private BiFunction<Invocation.Builder, Function<String, String>, Invocation.Builder> headerAppender;

    private Collection<String> dynamicHeaders;

    @PostConstruct
    private void init() {
        processHeaders();
    }

    private void processHeaders() {
        if (!headers.isPresent()) {
            headerAppender = (a, b) -> a;
            dynamicHeaders = emptySet();
        } else {
            final Properties properties = new Properties();
            try (final Reader reader = new StringReader(headers.get().trim())) {
                properties.load(reader);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
            final Set<String> dynamicHeaderKeys = new HashSet<>();
            final Map<String, Function<Function<String, String>, String>> providers =
                    properties.stringPropertyNames().stream().collect(toMap(identity(), e -> {
                        final String value = properties.getProperty(e);
                        if (value.contains("${") && value.contains("}")) {
                            final Map<String, String> toReplace = new HashMap<>();
                            int lastEnd = -1;
                            do {
                                final int start = value.indexOf("${", lastEnd);
                                if (start < 0) {
                                    break;
                                }
                                final int end = value.indexOf('}', start);
                                if (end < start) {
                                    break;
                                }

                                final String rawKey = value.substring(start + "${".length(), end);
                                dynamicHeaderKeys.add(rawKey);
                                toReplace.put(value.substring(start, end + 1), rawKey);
                                lastEnd = end;
                            } while (lastEnd > 0);
                            if (!toReplace.isEmpty()) {
                                return placeholders -> {
                                    String output = value;
                                    for (final Map.Entry<String, String> placeholder : toReplace.entrySet()) {
                                        output = output.replace(placeholder.getKey(),
                                                ofNullable(placeholders.apply(placeholder.getValue())).orElse(""));
                                    }
                                    return output;
                                };
                            }
                        }
                        return ignored -> value;
                    }));
            dynamicHeaders = unmodifiableSet(dynamicHeaderKeys);
            headerAppender = (builder, placeholders) -> {
                Invocation.Builder out = builder;
                for (final Map.Entry<String, Function<Function<String, String>, String>> header : providers
                        .entrySet()) {
                    out = out.header(header.getKey(), header.getValue().apply(placeholders));
                }
                return out;
            };
        }
    }

    public String getPrefix() {
        return PREFIX;
    }
}
