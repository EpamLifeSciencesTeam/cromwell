package centaur.serialization

import java.io.{BufferedWriter, FileNotFoundException, FileWriter}

import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * This class contains information about last version in which corresponding integration test was successfully run
  *
  * @param testCasesRuns key is a test name; value is a version of Cromwell, in which this test passed last time
  */
case class TestsReports(testCasesRuns: TrieMap[String, String]) {
  def addSuccessfulTest(testName: String, version: String): Option[String] =
    testCasesRuns.put(testName, version)

  def testsToSkip(version: String): Set[String] =
    testCasesRuns.filter(_._2 == version).keys.toSet
}

/**
  * Serializer for {@link centaur.serialization.TestsReports} class. Based on Circe, serializes to JSON
  */
object TestsReportsSerializer {
  private val logger = LoggerFactory.getLogger(TestsReportsSerializer.getClass)

  def read(fileName: String): TestsReports = {
    import io.circe.parser._

    val obj = readFile(fileName) {
      case Success(res) => decode[TrieMap[String, String]](res.getLines.mkString) match {
        case Left(ex) => logger.error(s"Failed to decode string: ${res.getLines.mkString}", ex); None
        case Right(v) => Some(v)
      }
      case Failure(_: FileNotFoundException) => logger.warn("File {} was not found", fileName); None
      case Failure(ex) => logger.error("Failed to read file", ex); None
    }

    obj match {
      case None => TestsReports(TrieMap.empty[String, String])
      case Some(m) => logger.info("Successfully deserialized object {}", m); TestsReports(m)
    }
  }

  def write(fileName: String, testsReports: TestsReports): Unit = {
    import io.circe.syntax._
    val jsonString = testsReports.testCasesRuns.asJson.spaces2
    writeFile(fileName, jsonString) {
      case Success(_) => logger.info("Successfully saved object {}", testsReports)
      case Failure(ex) => logger.error(s"Failed to serialize object: $testsReports", ex)
    }
  }

  private def readFile[T](filePath: String)(func: Try[Source] => T): T = {
    val input = Try(Source.fromFile(filePath))
    try {
      func(input)
    } finally {
      input.foreach(x => x.close())
    }
  }

  private def writeFile[T](filePath: String, json: String)(func: Try[Unit] => T): T = {
    val fileWriter = Try(new FileWriter(filePath))
    val output = fileWriter.map(x => new BufferedWriter(x))
    lazy val writeFunc = output.map(x => x.write(json))
    try {
      func(writeFunc)
    } finally {
      output.foreach(x => x.close())
    }
  }
}