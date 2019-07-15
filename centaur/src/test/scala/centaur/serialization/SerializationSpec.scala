package centaur.serialization

import java.io.File

import org.apache.commons.io.FileUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.concurrent.TrieMap

class SerializationSpec extends FlatSpec with Matchers {
  private val filePath: String = "centaur/src/test/resources/testSerialize.json"

  behavior of "TestsReports"
  it should "correctly serialize and deserialize data" in {
    val testsReports = TestsReports(TrieMap("cool_test_1" -> "43-snapshot", "cool_test_2" -> "42-snapshot",
      "cool_test_3" -> "43-fq33qw4"))
    TestsReportsSerializer.write(filePath, testsReports)

    val result = TestsReportsSerializer.read(filePath)

    assert(testsReports == result)

    //clean up
    FileUtils.deleteQuietly(new File(filePath))
  }

  it should "read empty object when file is not exists and be able to add a new value" in {
    val testsReports = TestsReportsSerializer.read(filePath)

    assert(testsReports != null)
    assert(testsReports.testCasesRuns.isEmpty)

    val testName = "cool_test_4"
    val cromwellVer = "42-qwem4343w"
    testsReports.addSuccessfulTest(testName, cromwellVer)

    assert(testsReports.testCasesRuns.nonEmpty)
    assert(testsReports.testsToSkip(cromwellVer).contains(testName))
  }
}
