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
package org.talend.sdk.component.proxy.api.persistence;

import java.util.Collection;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.servlet.http.HttpServletRequest;

import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

import lombok.Getter;

public class OnEdit extends PersistenceEvent {

    @Getter
    private final String id;

    public OnEdit(final String id, final HttpServletRequest request, final Jsonb jsonb, final JsonObject enrichment,
            final Collection<SimplePropertyDefinition> definitions, final Map<String, String> properties) {
        super(request, jsonb, enrichment, definitions, properties);
        this.id = id;
    }
}
