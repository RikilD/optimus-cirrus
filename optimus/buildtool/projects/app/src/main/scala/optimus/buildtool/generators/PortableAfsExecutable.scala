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
package optimus.buildtool.generators

import optimus.buildtool.config.AfsNamingConventions
import optimus.buildtool.config.DependencyDefinition
import optimus.buildtool.files.FileAsset
import optimus.buildtool.scope.CompilationScope
import optimus.buildtool.utils.Utils

import java.nio.file.Paths

final case class PortableAfsExecutable(
    windows: AfsExecutable,
    linux: AfsExecutable
) {

  /**
   * Returns the AFS executable corresponding to the current platform, modified by whatever is in the config file.
   */
  def configured(config: Map[String, String]): AfsExecutable = {
    // could conceivably be extended to other platforms...
    val (default, platform) = if (Utils.isWindows) (windows, "windows") else (linux, "linux")

    def get(key: String, default: String): String = config.getOrElse(s"$key.$platform", default)

    default.copy(
      metaDir = get("execMetaDir", default.metaDir),
      projectDir = get("execProjectDir", default.projectDir),
      executablePath = get("execPath", default.executablePath),
      variant = config.get("variant")
    )
  }
}

final case class AfsExecutable(
    metaDir: String,
    projectDir: String,
    executablePath: String,
    variant: Option[String]
) {
  def file(): FileAsset =
    FileAsset(Paths.get(s"protoc-3.21.1-${AfsExecutable.hostClassifier}.exe").toAbsolutePath)

  def dependencyDefinition(scope: CompilationScope): DependencyDefinition =
    scope.externalDependencyResolver.dependencyDefinitions
      .find(d => d.group == metaDir && d.name == projectDir && d.variant.map(_.name) == variant)
      .getOrElse {
        val suffix = variant.map(v => s".$v").getOrElse("")
        throw new IllegalArgumentException(s"No central dependency found for ${metaDir}.${projectDir}$suffix")
      }
}

object AfsExecutable {

  /**
   * Maven Central classifier for the host, e.g. "osx-aarch_64". `file` resolves the protoc binary by this name because
   * this workspace has no AFS to look it up in; bootstrap.sh derives the same name and links the matching binary into
   * the source root, so the two must agree.
   */
  def hostClassifier: String = {
    val os = sys.props("os.name").toLowerCase match {
      case n if n.contains("mac") || n.contains("darwin") => "osx"
      case n if n.contains("win")                         => "windows"
      case _                                              => "linux"
    }
    val arch = sys.props("os.arch").toLowerCase match {
      case "aarch64" | "arm64" => "aarch_64"
      case _                   => "x86_64"
    }
    s"$os-$arch"
  }
}
