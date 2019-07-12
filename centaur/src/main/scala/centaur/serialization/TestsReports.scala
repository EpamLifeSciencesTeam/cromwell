package centaur.serialization

import java.io.{FileInputStream, FileOutputStream}

import com.esotericsoftware.kryo.io.{Input, Output}
import com.twitter.chill.ScalaKryoInstantiator
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

/**
  * This class contains information about last version in which corresponding integration test was successfully run
  * @param testCasesRuns  key is a test name; value is a version of Cromwell, in which this test passed last time
  */
case class TestsReports(testCasesRuns: TrieMap[String, String]) {
  def addSuccessfulTest(testName: String, version: String): Option[String] =
    testCasesRuns.put(testName, version)

  def testsToSkip(version: String): Set[String] =
    testCasesRuns.filter(_._2 == version).keys.toSet
}

/**
  * Serializer for {@link centaur.serialization.TestsReports} class. Based on Kryo, uses twitter's chill for Scala collections serialization
  * @see <a href="https://github.com/twitter/chill">chill</a>
  */
object TestsReportsSerializer {
  private val logger = LoggerFactory.getLogger(TestsReportsSerializer.getClass)
  private val instantiator = new ScalaKryoInstantiator
  instantiator.setRegistrationRequired(false)
  private val kryo = instantiator.newKryo()

  def read(fileName: String): TestsReports = {
    val obj = readFile(fileName) {
      case Success(i) => Option(kryo.readObject(i, classOf[mutable.HashMap[String, String]]))
      case Failure(ex) => logger.error("An error occurred while trying to deserialize object", ex); None
    }

    obj match {
      case None => TestsReports(TrieMap.empty[String, String])
      //Unfortunately, chill does not supports TrieMap serialization/deserialization out of box
      //Therefore, this 'hack' to convert HashMap to right type is used
      case Some(map) => TestsReports(TrieMap.empty[String, String] ++ map)
    }
  }

  def write(fileName: String, testsReports: TestsReports): Unit = {
    writeFile(fileName) {
      case Success(o) => kryo.writeObject(o, testsReports.testCasesRuns)
      case Failure(ex) => logger.error("An error occurred while trying to serialize object", ex)
    }
  }

  def readFile[T](filePath: String)(func: Try[Input] => T): T = {
    val fileInputStream = Try(new FileInputStream(filePath))
    val input = fileInputStream.map(x => new Input(x))
    try {
      func(input)
    } finally {
      input.foreach(x => x.close())
    }
  }

  def writeFile[T](filePath: String)(func: Try[Output] => T): T = {
    val fileOutputStream = Try(new FileOutputStream(filePath))
    val output = fileOutputStream.map(x => new Output(x))
    try {
      func(output)
    } finally {
      output.foreach(x => x.close())
    }
  }
}