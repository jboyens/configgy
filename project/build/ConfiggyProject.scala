import sbt._
import com.twitter.sbt._


class ConfiggyProject(info: ProjectInfo) extends StandardProject(info) {
  val specs = "org.scala-tools.testing" %% "specs" % "1.6.5-SNAPSHOT"
  val vscaladoc = "org.scala-tools" % "vscaladoc" % "1.1-md-3"

  val scalaToolsSnapshots = ScalaToolsSnapshots

  override def pomExtra =
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>

  Credentials(Path.userHome / ".ivy2" / "credentials", log)
  val publishTo = "nexus" at "http://nexus.scala-tools.org/content/repositories/releases/"
}
