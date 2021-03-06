/**
 *  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import clonedeep from 'lodash/cloneDeep';
import get from 'lodash/get';
import { removeError, addError, getError } from '@talend/react-forms/lib/UIForm/utils/errors';

/**
 * Change errors on the target input
 * Add the error if trigger results in an error
 * Remove error if trigger has no error
 * @param errors The form errors map
 * @param schema The input schema
 * @param errorMessage The trigger error message
 * @returns {object} The new errors map
 */
function getNewErrors(errors, schema, errorMessage) {
  if (errorMessage) {
    return addError(errors, schema, errorMessage);
  } else if (getError(errors, schema) !== undefined) {
    return removeError(errors, schema);
  }
  return errors;
}

/**
 * Add or Remove the input error depending on the trigger result
 * @param schema The input schema
 * @param body The trigger response body
 * @param errors The form errors map
 * @returns {{errors: *}} The new errors map
 */
function validation({ schema, body, errors }) {
  const newError = body.status === 'KO' ? body.comment : undefined;
  return { errors: getNewErrors(errors, schema, newError) };
}

/**
 * Insert new form data
 * @param schema The input schema
 * @param body The trigger response body
 * @param properties The form data
 * @param trigger The trigger configuration
 * @param errors The form errors map
 * @returns {{properties: *, errors: Object}} The properties and errors map
 */
function schema({ schema, body, properties, trigger, errors }) {
  const newErrors = getNewErrors(errors, schema, body.error);
  let newProperties = properties;

  if (body.entries && trigger.options && trigger.options.length !== 0) {
    newProperties = clonedeep(properties);
    for (const { path, type } of trigger.options) {
      let parentPath = path;
      let directChildPath = path;
      const lastDot = path.lastIndexOf('.');
      if (lastDot > 0) {
        parentPath = path.substring(0, lastDot);
        directChildPath = path.substring(lastDot + 1);
      }

      let mutable = (parentPath === path) ? newProperties : get(newProperties, parentPath);
      if (!mutable) {
        continue;
      }
      mutable[directChildPath] = (type === 'array') ?
        body.entries.map(e => e.name) :
        body.entries.reduce({}, (a, e) => {
          a[e.name] = e.type;
          return a;
        });
    }
  }
  return {
    properties: newProperties,
    errors: newErrors,
  };
}

function dynamic_values({ properties }) {
  // for now it is set on the server side so no-op is ok
  return { properties };
}

function suggestions({ body }) {
  return { titleMap: (body.items || []).map(item => ({ name: item.label, value: item.id })) };
}

export default {
  dynamic_values,
  healthcheck: validation,
  schema,
  validation,
  suggestions,
};
