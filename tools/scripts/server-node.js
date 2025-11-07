/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

const http = require('http');

const certificate = { kind: 'selfSigned', subject: 'localhost' }
const options = {
  elide: {
    https: { certificate },
    http3: { certificate },
  }
}

const server = http.createServer(options, (req, res) => {
  let content = '';
  req.on('data', (chunk) => {
    content += chunk.toString("utf8");
  })

  req.on('end', () => {
    res.writeHead(200, {'Content-Type': 'text/plain', 'Content-Length': content.length});
    res.write(content);
    res.end();
  })
});

server.listen(3000, () => {
  console.log('Server worker started on http://localhost:3000 🎉');
});
