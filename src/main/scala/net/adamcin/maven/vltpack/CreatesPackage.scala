package net.adamcin.maven.vltpack

import java.io.{FileOutputStream, File}
import org.apache.maven.plugin.MojoExecutionException
import com.day.jcr.vault.vlt.meta.xml.zip.ZipMetaDir
import com.day.jcr.vault.vlt.meta.xml.file.FileMetaDir
import com.day.jcr.vault.vlt.meta.MetaDirectory
import org.apache.maven.plugins.annotations.Parameter
import scalax.io.Resource
import java.util.TimeZone
import org.apache.maven.plugin.logging.Log
import java.util.jar.{JarEntry, JarOutputStream}


/**
 *
 * @version $Id: CreatesPackage.java$
 * @author madamcin
 */
trait CreatesPackage extends LogsParameters {

  final val defaultJcrPath = "/"
  val vaultPrefix = "META-INF/vault/"
  final val JCR_ROOT = "jcr_root"
  final val META_INF = "META-INF"

  @Parameter(property = "jcrPath", defaultValue = defaultJcrPath)
  val jcrPath: String = defaultJcrPath

  lazy val jcrPathNoSlashEnd = noLeadingSlash(noTrailingSlash(jcrPath))

  @Parameter(property = "vlt.tz")
  val serverTimezone: String = null

  val localTz = TimeZone.getDefault

  lazy val serverTz = Option(serverTimezone) match {
    case Some(tz) => TimeZone.getTimeZone(tz)
    case None => TimeZone.getDefault
  }

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("jcrPath = " + jcrPath)
    log.info("vlt.tz = " + serverTimezone)
  }

  def adjustToServerTimeZone(localTime: Long): Long =
    (localTime - localTz.getOffset(localTime)) + serverTz.getOffset(localTime)

  def noLeadingSlash(path: String) = Option(path) match {
    case Some(p) => if (p.startsWith("/")) p.substring(1, p.length) else p
    case None => ""
  }

  def noTrailingSlash(path: String) = Option(path) match {
    case Some(p) => if (p.endsWith("/")) p.substring(0, p.length - 1) else p
    case None => ""
  }

  def leadingSlashIfNotEmpty(path: String) = Option(path) match {
    case Some(p) => if (p.length > 0 && !p.startsWith("/")) "/" + p else p
    case None => ""
  }


  def verifyJcrPath(vltRoot: File, vaultSource: File) {
    val vltFile = new File(vltRoot, "jcr_root/.vlt")
    if (!vltFile.exists || !vltFile.canRead) {
      Right(new MojoExecutionException("Failed to read .vlt file"))
    } else {
      val metaDir: MetaDirectory = if (vltFile.isDirectory) {
        new ZipMetaDir(vltFile)
      } else {
        new FileMetaDir(vltFile)
      }

      val vltEntries = metaDir.getEntries
    }
  }

  /**
   * Creates a package from a standard vlt working copy, such that the vltRoot directory has two children, jcr_root and
   * META-INF.
   * @param vltRoot parent of both jcr_root and META-INF
   * @param zipFile zip file to write to
   * @param prepareZipFile function that prepares the stream by, say, injecting generated versions of entries and
   *                      returning a Set of those entry names that should be skipped during normal package generation,
   *                      thus allowing one to merge multiple source trees
   */
  def createPackage(
                     vltRoot: File,
                     zipFile: File,
                     prepareZipFile: (JarOutputStream) => Set[String]) {

    if (zipFile.exists) {
      throw new MojoExecutionException("zipFile already exists")
    }

    val zipResource = Resource.fromOutputStream(new JarOutputStream(new FileOutputStream(zipFile)))

    zipResource.acquireFor {
      (zip) => {
        val skip = Option(prepareZipFile) match {
          case Some(f) => f(zip)
          case None => Set.empty[String]
        }

        addEntryToZipFile(
          addToSkip = false,
          skipEntries = skip,
          entryFile = new File(vltRoot, JCR_ROOT),
          entryName = JCR_ROOT + leadingSlashIfNotEmpty(jcrPathNoSlashEnd),
          zip = zip)

        addEntryToZipFile(
          addToSkip = false,
          skipEntries = skip,
          entryFile = new File(vltRoot, META_INF),
          entryName = META_INF,
          zip = zip)
      }
    }
  }

  /**
   * Recursively adds files to the provided zip output stream
   * @param addToSkip set to true to add the current entry to the returned set of entries to skip.
   * @param skipEntries entry names that should not be added to the zip file
   * @param entryFile file that will be copied to the stream if it is not a directory. if it is a directory, it's children
   *             will be listed and this method will be called on each as appropriate
   * @param entryName name of current entry should it be successfully added to the zip stream
   * @param zip zip output stream
   * @return set of zip entries to skip during subsequent calls to this method
   */
  def addEntryToZipFile(
                       addToSkip: Boolean,
                       skipEntries: Set[String],
                       entryFile: File,
                       entryName: String,
                       zip: JarOutputStream): Set[String] = {

    if (entryFile.isDirectory) {
      entryFile.listFiles().foldLeft(skipEntries) {
        (skip, f) => addEntryToZipFile(addToSkip, skipEntries, f, entryName + "/" + f.getName, zip)
      }
    } else {
      if (!entryFile.exists() || (skipEntries contains entryName)) {
        skipEntries
      } else {
        val entry = new JarEntry(entryName)

        if (entryFile.lastModified > 0) {
          entry.setTime(adjustToServerTimeZone(entryFile.lastModified))
        }

        zip.putNextEntry(entry)

        Resource.fromFile(entryFile).copyDataTo(Resource.fromOutputStream(IOUtil.blockClose(zip)))
        if (addToSkip) {
          skipEntries + entryName
        } else {
          skipEntries
        }
      }
    }
  }

}