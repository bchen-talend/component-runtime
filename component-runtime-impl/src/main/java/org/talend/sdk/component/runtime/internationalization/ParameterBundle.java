/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.internationalization;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class ParameterBundle extends InternalBundle {

    private final String[] simpleNames;

    public ParameterBundle(final ResourceBundle[] bundles, final String prefix, final String... simpleNames) {
        super(bundles, prefix);
        this.simpleNames = simpleNames;
    }

    public Optional<String> displayName(final ParameterBundle parent) {
        return get(parent, "_displayName");
    }

    public Optional<String> placeholder(final ParameterBundle parent) {
        return get(parent, "_placeholder");
    }

    private Optional<String> get(final ParameterBundle parentBundle, final String suffix) {
        Optional<String> v = readValue(suffix);
        if (!v.isPresent()) {
            v = Stream.of(simpleNames).map(s -> {
                final String k = s + "." + suffix;
                return readRawValue(k).orElse(parentBundle == null ? null : parentBundle.readRawValue(k).orElse(null));
            }).filter(Objects::nonNull).findFirst();
        }

        return v;
    }

}
