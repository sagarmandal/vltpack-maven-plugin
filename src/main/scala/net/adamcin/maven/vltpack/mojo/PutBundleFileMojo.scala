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

package net.adamcin.maven.vltpack.mojo

import java.io.File
import org.apache.maven.plugin.logging.Log
import net.adamcin.maven.vltpack.PutsBundle
import scala.Left
import scala.Right
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}

/**
 * PUT a bundle identified by the file parameter to the configured CQ instance
 * @since 1.0
 * @author Mark Adamcin
 */
@Mojo(name = "put-bundle-file",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  requiresProject = false,
  threadSafe = true)
class PutBundleFileMojo
  extends BaseMojo
  with PutsBundle {

  /**
   * Specify a bundle file to be PUT
   * @since 1.0
   */
  @Parameter(property = "file", required = true)
  val file: File = null

  override def execute() {
    putBundle(file) match {
      case Right(t) => throw t
      case Left(messages) => messages.foreach { getLog.info(_) }
    }
  }

  override def printParams(log: Log) {
    super.printParams(log)
    getLog.info("file = " + file)
  }
}


