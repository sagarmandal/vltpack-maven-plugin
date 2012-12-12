/*
 * Copyright 2012 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.maven.vltpack

import mojo.BaseMojo
import org.apache.maven.repository.RepositorySystem
import org.apache.maven.artifact.repository.{DefaultRepositoryRequest, RepositoryRequest}
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.logging.Log
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.slf4j.LoggerFactory
import collection.JavaConversions
import java.io.File
import scalax.io.Resource
import org.codehaus.plexus.util.SelectorUtils
import org.apache.maven.project.MavenProject
import org.apache.maven.plugins.annotations.Component

/**
 * Trait defining common mojo parameters and methods useful for arbitrarily resolving artifacts from
 * local and remote repositories
 * @since 1.0
 * @author Mark Adamcin
 */
trait ResolvesArtifacts extends BaseMojo {
  private val log = LoggerFactory.getLogger(getClass)

  @Component
  var repositorySystem: RepositorySystem = null

  def proj: MavenProject = Option(session) match {
    case Some(s) => s.getCurrentProject
    case None => null
  }

  lazy val repositoryRequest: RepositoryRequest = DefaultRepositoryRequest.getRepositoryRequest(session, proj)

  lazy val dependencies: List[Artifact] = Option(proj) match {
    case Some(project) => {
      JavaConversions.collectionAsScalaIterable(project.getDependencyArtifacts).toList
    }
    case None => Nil
  }

  def resolveByArtifactIds(artifactIds: Set[String]): Stream[Artifact] = {

    val deps = dependencies.filter { (artifact: Artifact) => artifactIds contains artifact.getArtifactId}

    resolveArtifacts(deps.toStream)
  }

  def resolveArtifacts(artifacts: Stream[Artifact]): Stream[Artifact] = {
    val request = new ArtifactResolutionRequest(repositoryRequest)
    artifacts.toStream.foldLeft(Stream.empty[Artifact]) {
      (stream, artifact: Artifact) => {
        request.setArtifact(artifact)
        val result = repositorySystem.resolve(request)

        stream #::: JavaConversions.collectionAsScalaIterable(result.getArtifacts).toStream
      }
    }
  }

  def resolveByFilter(filter: String): Stream[Artifact] = {
    val deps = dependencies.filter {
      (artifact: Artifact) => SelectorUtils.`match`(filter, artifact.getDependencyConflictId)
    }

    resolveArtifacts(deps.toStream)
  }

  def resolveByGATCV(groupId: String, artifactId: String, artifactType: String, classifier: String, version: String): Option[Artifact] = {
    val artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, artifactType, classifier)
    Option(artifact) match {
      case Some(a) => resolveArtifacts(Stream(a)).headOption
      case None => None
    }
  }

  def copyToDir(dir: File, log: Log)(artifact: Artifact): File = {
    val target = new File(dir, artifact.getFile.getName)
    if (target.lastModified().compareTo(artifact.getFile.lastModified()) < 0) {
      log.info("Copying " + artifact.getId)
      log.info("\t to " + VltpackUtil.toRelative(new File("."), target.getAbsolutePath))
      Resource.fromFile(artifact.getFile).copyDataTo(Resource.fromFile(target))
    }
    target
  }

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("repositorySystem is empty? " + Option(repositorySystem).isEmpty)
    log.info("repositoryRequest is empty? " + Option(repositoryRequest).isEmpty)
  }
}