/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

const app = express()
const port = 3000

app.get("/hello", (req, res) => {
  res.send("Hello!")
})

app.get("/hello/:name", (req, res) => {
  const name = req.params.name
  console.log(`said hello to name: ${req.params.name}`)
})

app.listen(port, () => {
  console.info(`Listening on port ${port}`)
})
