/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.vltpack.mojo

import java.io.File

import net.adamcin.vltpack.PutsBundles
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

/**
 * PUT a bundle identified by the file parameter to the configured CQ instance
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(name = "put-bundle-file",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  requiresProject = false,
  threadSafe = true)
class PutBundleFileMojo
  extends BaseMojo
  with PutsBundles {


  /**
   * Specify a bundle file to be PUT
   */
  @Parameter(property = "file", required = true)
  val file: File = null

  override def execute() {
    putBundle(file) match {
      case Left(t) => throw t
      case Right(messages) => messages.foreach { getLog.info(_) }
    }
  }
}


