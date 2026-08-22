/*
 * Morgan Stanley makes this available to you under the Apache License, Version 2.0 (the "License").
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package optimus.graph

import optimus.breadcrumbs.ChainedID
import optimus.breadcrumbs.crumbs.CrumbNodeType
import optimus.breadcrumbs.crumbs.EdgeType

/**
 * Stand-in for the internal edge tracker, which records parent/child relationships between chained IDs so that a
 * request graph can be reassembled from the crumb stream. Nothing here consumes that graph, so tracking is a no-op.
 */
object Edges {
  def ensureTracked(
      parent: ChainedID,
      child: ChainedID,
      name: String,
      nodeType: CrumbNodeType.CrumbNodeType,
      edgeType: EdgeType.EdgeType): Unit = ()
}
