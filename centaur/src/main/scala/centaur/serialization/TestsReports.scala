package centaur.serialization

import java.io.{BufferedWriter, FileNotFoundException, FileWriter}

import centaur.serialization.TestsReports.{CromwellVersion, TestName, TestReports}
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.io.Source
import scala.util.Try

object TestsReports {
  type TestName = String
  type CromwellVersion = String
  type TestReports = TrieMap[TestName, CromwellVersion]

  def apply(): TestsReports = new TestsReports(TrieMap.empty[TestName, CromwellVersion])
}

/**
  * This class contains information about last version in which corresponding integration test was successfully run
  *
  * @param testCasesRuns key is a test name; value is a version of Cromwell, in which this test passed last time
  */
case class TestsReports(testCasesRuns: TestReports) {
  def addSuccessfulTest(testName: TestName, version: CromwellVersion): Option[String] =
    testCasesRuns.put(testName, version)

  def testsToSkip(version: CromwellVersion): Set[TestName] =
    testCasesRuns.filter(_._2 == version).keys.toSet
}

/**
  * Serializer for {@link centaur.serialization.TestsReports} class. Based on Circe, serializes to JSON
  */
object TestsReportsSerializer {
  import io.circe.generic.auto._

  private val logger = LoggerFactory.getLogger(TestsReportsSerializer.getClass)

  def read(fileName: String): TestsReports = {
    import io.circe.parser._

    using(Try(Source.fromFile(fileName))) { src =>
      src.map(x => decode[TestsReports](x.getLines.mkString)).fold(
        x => { log(x, "read"); TestsReports() },
        y => y.getOrElse(TestsReports()))
    }
  }

  def write(fileName: String, testsReports: TestsReports): Unit = {
    import io.circe.syntax._

    val jsonString = testsReports.asJson.spaces2
    using(Try(new BufferedWriter(new FileWriter(fileName)))) { wrt =>
      wrt.fold(x => log(x, "write"), _.write(jsonString))
    }
  }

  private def using[R <: AutoCloseable, A](r: Try[R])(f: Try[R] => A): A = try f(r) finally r.foreach(_.close)

  private def log[E <: Throwable](ex: E, str: String): Unit = {
    ex match {
      case e: FileNotFoundException if str == "read" => logger.warn(s"Failed to $str object, it will be created", e)
      case _ => logger.error(s"Failed to $str object", ex)
    }
  }
}

