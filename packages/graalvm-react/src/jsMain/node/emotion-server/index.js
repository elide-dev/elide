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

function createExtractCriticalToChunks(cache) {
  return function (html) {
    const RGX = new RegExp(`${cache.key}-([a-zA-Z0-9-_]+)`, "gm");

    const o = { html, styles: [] };
    let match;
    const ids = {};
    while ((match = RGX.exec(html)) !== null) {
      if (ids[match[1]] === undefined) {
        ids[match[1]] = true;
      }
    }

    const regularCssIds = [];
    let regularCss = "";

    Object.keys(cache.inserted).forEach((id) => {
      if (
        (ids[id] !== undefined ||
          cache.registered[`${cache.key}-${id}`] === undefined) &&
        cache.inserted[id] !== true
      ) {
        if (cache.registered[`${cache.key}-${id}`]) {
          regularCssIds.push(id);
          regularCss += cache.inserted[id];
        } else {
          o.styles.push({
            key: `${cache.key}-global`,
            ids: [id],
            css: cache.inserted[id],
          });
        }
      }
    });

    o.styles.push({ key: cache.key, ids: regularCssIds, css: regularCss });

    return o;
  };
}

function generateStyleTag(cssKey, ids, styles, nonceString) {
  return `<style data-emotion="${cssKey} ${ids}"${nonceString}>${styles}</style>`;
}

function createConstructStyleTagsFromChunks(cache, nonceString) {
  return function (criticalData) {
    let styleTagsString = "";

    criticalData.styles.forEach((item) => {
      styleTagsString += generateStyleTag(
        item.key,
        item.ids.join(" "),
        item.css,
        nonceString
      );
    });

    return styleTagsString;
  };
}

export function createEmotionServer(cache) {
  if (cache.compat !== true) {
    cache.compat = true;
  }
  const nonceString =
    cache.nonce !== undefined ? ` nonce="${cache.nonce}"` : "";
  return {
    extractCriticalToChunks: createExtractCriticalToChunks(cache),
    constructStyleTagsFromChunks: createConstructStyleTagsFromChunks(
      cache,
      nonceString
    ),
  };
}
