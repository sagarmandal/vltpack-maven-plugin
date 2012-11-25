package net.adamcin.maven.vltpack

import org.apache.maven.plugin.AbstractMojo
import java.util.Collections
import org.apache.maven.plugins.annotations._
import org.apache.maven.project.MavenProject
import collection.JavaConversions
import org.apache.maven.artifact.Artifact
import java.io.File

/**
 *
 * @version $Id: PackageMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "package",
  defaultPhase = LifecyclePhase.PACKAGE,
  requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
class PackageMojo extends AbstractMojo {

  @Component
  var project: MavenProject = null

  @Parameter
  var embedBundles = Collections.emptyList[String]

  @Parameter
  var embedPackages = Collections.emptyList[String]

  def execute() {
    val deps = JavaConversions.collectionAsScalaIterable(project.getDependencyArtifacts).
      filter { art: Artifact => art.getType == null || art.getType == "jar" }.
      map { art: Artifact => (art.getArtifactId, art) }.toMap


    val outputDirectory = new File(project.getBuild.getOutputDirectory)

  }


}