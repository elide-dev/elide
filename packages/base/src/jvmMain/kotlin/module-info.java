/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

module elide.base {
    requires java.base;
    requires kotlin.stdlib;
    requires io.micronaut.inject;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires org.slf4j;

    exports elide.annotations;
    exports elide.annotations.base;
    exports elide.annotations.data;
    exports elide.util;
    exports elide.runtime;
    exports elide.runtime.jvm;
}
