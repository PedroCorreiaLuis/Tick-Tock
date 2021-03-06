package database.mappings

import java.util.UUID

import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.PlaySpec
import database.utils.DatabaseUtils._
import slick.jdbc.MySQLProfile.api._
import database.mappings.TaskMappings._
import api.utils.DateUtils._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class TaskMappingsSuite extends PlaySpec with BeforeAndAfterAll{

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  val dtbase: Database = appBuilder.injector.instanceOf[Database]

  val taskUUID1 = UUID.randomUUID().toString
  val taskUUID2 = UUID.randomUUID().toString
  val taskUUID3 = UUID.randomUUID().toString
  val taskUUID4 = UUID.randomUUID().toString

  val fileUUID1 = UUID.randomUUID().toString
  val fileUUID2 = UUID.randomUUID().toString
  val fileUUID3 = UUID.randomUUID().toString

  override def beforeAll = {
    val result = for {
      _ <- dtbase.run(createTasksTableAction)
      _ <- dtbase.run(insertTask(TaskRow(taskUUID1, fileUUID1, 0))) //runOnce task
      _ <- dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))) //periodic task
      res <- dtbase.run(insertTask(TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some("PST")))) //personalized task
    } yield res
    Await.result(result, Duration.Inf)
    Await.result(dtbase.run(selectAllFromTasksTable.result), Duration.Inf).size mustBe 3

  }

  override def afterAll = {
    dtbase.run(dropTasksTableAction)
  }

  "TaskMappings#selectTaskByTaskId" should {
    "return the correct TaskRow when given an existing taskId." in {
      val result1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "TaskRow(" + taskUUID1 + "," + fileUUID1 + ",0,None,None,None,None,None,None)"
      val result2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#selectTaskByFileId" should {
    "return the correct TaskRow when given an existing fileId." in {
      val result1 = Await.result(dtbase.run(selectTaskByFileId(fileUUID1).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "TaskRow(" + taskUUID1 + "," + fileUUID1 + ",0,None,None,None,None,None,None)"
      val result2 = Await.result(dtbase.run(selectTaskByFileId(fileUUID2).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#selectTaskByPeriod" should {
    "return the correct TaskRow when given an existing period." in {
      val result1 = Await.result(dtbase.run(selectTaskByPeriod(0).result), Duration.Inf)
      result1.size mustBe 1
      result1.head.toString mustBe "TaskRow(" + taskUUID1 + "," + fileUUID1 + ",0,None,None,None,None,None,None)"
      val result2 = Await.result(dtbase.run(selectTaskByPeriod(4).result), Duration.Inf)
      result2.size mustBe 1
      result2.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#selectTaskByValue" should {
    "return the correct TaskRow when given an existing value." in {
      val result = Await.result(dtbase.run(selectTaskByValue(3).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#selectTaskByStartDateAndTime" should {
    "return the correct TaskRow when given an existing startDateAndTime." in {
      val result = Await.result(dtbase.run(selectTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      result.size mustBe 2
      result.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
      result.tail.head.toString mustBe "TaskRow(" + taskUUID3 + "," + fileUUID3 + ",7,None,Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,Some(PST))"
    }
  }

  "TaskMappings#selectTaskByEndDateAndTime" should {
    "return the correct TaskRow when given an existing endDateAndTime." in {
      val result = Await.result(dtbase.run(selectTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "TaskRow(" + taskUUID3 + "," + fileUUID3 + ",7,None,Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,Some(PST))"
    }
  }

  "TaskMappings#selectTaskByTotalOccurrences" should {
    "return the correct TaskRow when given an existing totalOccurrences." in {
      val result = Await.result(dtbase.run(selectTaskByTotalOccurrences(10).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#selectTaskByCurrentOccurrences" should {
    "return the correct TaskRow when given an existing currentOccurrences." in {
      val result = Await.result(dtbase.run(selectTaskByCurrentOccurrences(10).result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#selectTaskByTimezone" should {
    "return the correct TaskRow when given an existing timezone." in {
      val result = Await.result(dtbase.run(selectTaskByTimezone("PST").result), Duration.Inf)
      result.size mustBe 1
      result.head.toString mustBe "TaskRow(" + taskUUID3 + "," + fileUUID3 + ",7,None,Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,Some(PST))"
    }
  }

  "TaskMappings#insertTask" should {
    "insert the given TaskRow." in {
      val insertResult = Await.result(dtbase.run(insertTask(TaskRow(taskUUID4, fileUUID1, 0))), Duration.Inf)
      insertResult mustBe 1
      val selectResult = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID1 + ",0,None,None,None,None,None,None)"
      Await.result(dtbase.run(deleteTaskByTaskId(taskUUID4)), Duration.Inf)
    }
  }

  "TaskMappings#updateTaskByTaskId" should {
    "update a TaskRow by giving the corresponding taskId." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByTaskId(taskUUID1, TaskRow(taskUUID4, fileUUID2, 0))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID2 + ",0,None,None,None,None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByTaskId(taskUUID4, TaskRow(taskUUID1, fileUUID1, 0))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID1 + "," + fileUUID1 + ",0,None,None,None,None,None,None)"
    }
  }

  "TaskMappings#updateTaskByFileId" should {
    "update a TaskRow by giving the corresponding fileId." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByFileId(fileUUID1, TaskRow(taskUUID4, fileUUID2, 0))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID2 + ",0,None,None,None,None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByFileId(fileUUID2, TaskRow(taskUUID1, fileUUID1, 0))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID1 + "," + fileUUID1 + ",0,None,None,None,None,None,None)"
    }
  }

  "TaskMappings#updateTaskByPeriod" should {
    "update a TaskRow by giving the corresponding period." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByPeriod(0, TaskRow(taskUUID4, fileUUID2, 1, Some(5), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID2 + ",1,Some(5),Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByPeriod(1, TaskRow(taskUUID1, fileUUID1, 0))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID1 + "," + fileUUID1 + ",0,None,None,None,None,None,None)"
    }
  }

  "TaskMappings#updateTaskByValue" should {
    "update a TaskRow by giving the corresponding value." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByValue(3, TaskRow(taskUUID4, fileUUID1, 1, Some(5), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID1 + ",1,Some(5),Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByValue(5, TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#updateTaskByStartDateAndTime" should {
    "update a TaskRow by giving the corresponding startDateAndTime." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), TaskRow(taskUUID4, fileUUID1, 1, Some(5), Some(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID1 + ",1,Some(5),Some(Thu Jan 01 00:00:00 GMT 2035),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByStartDateAndTime(stringToDateFormat("2035-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#updateTaskByEndDateAndTime" should {
    "update a TaskRow by giving the corresponding endDateAndTime." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), TaskRow(taskUUID4, fileUUID1, 1, Some(5), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2045-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID1 + ",1,Some(5),Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2045),None,None,None)"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByEndDateAndTime(stringToDateFormat("2045-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some("PST")))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID3).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID3 + "," + fileUUID3 + ",7,None,Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,Some(PST))"
    }
  }

  "TaskMappings#updateTaskByTotalOccurrences" should {
    "update a TaskRow by giving the corresponding totalOccurrences." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByTotalOccurrences(10, TaskRow(taskUUID4, fileUUID1, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(5), Some(5)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID1 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(5),Some(5),None)"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByTotalOccurrences(5, TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#updateTaskByCurrentOccurrences" should {
    "update a TaskRow by giving the corresponding currentOccurrences." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByCurrentOccurrences(10, TaskRow(taskUUID4, fileUUID1, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(5), Some(5)))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID1 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(5),Some(5),None)"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByCurrentOccurrences(5, TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID2).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID2 + "," + fileUUID2 + ",4,Some(3),Some(Tue Jan 01 00:00:00 GMT 2030),None,Some(10),Some(10),None)"
    }
  }

  "TaskMappings#updateTaskByTimezone" should {
    "update a TaskRow by giving the corresponding timezone." in {
      val updateResult1 = Await.result(dtbase.run(updateTaskByTimezone("PST", TaskRow(taskUUID4, fileUUID2, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some("EST")))), Duration.Inf)
      updateResult1 mustBe 1
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult1.head.toString mustBe "TaskRow(" + taskUUID4 + "," + fileUUID2 + ",7,None,Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,Some(EST))"
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      val updateResult2 = Await.result(dtbase.run(updateTaskByTimezone("EST", TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some("PST")))), Duration.Inf)
      updateResult2 mustBe 1
      val selectResult3 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID4).result), Duration.Inf)
      selectResult3.isEmpty mustBe true
      val selectResult4 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID3).result), Duration.Inf)
      selectResult4.head.toString mustBe "TaskRow(" + taskUUID3 + "," + fileUUID3 + ",7,None,Some(Tue Jan 01 00:00:00 GMT 2030),Some(Sun Jan 01 00:00:00 GMT 2040),None,None,Some(PST))"
    }
  }

  "TaskMappings#deleteTaskByTaskId" should {
    "delete a TaskRow by giving the corresponding taskId." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByTaskId(taskUUID1)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectTaskByTaskId(taskUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID1, fileUUID1, 0))), Duration.Inf)
    }
  }

  "TaskMappings#deleteTaskByFileId" should {
    "delete a TaskRow by giving the corresponding fileId." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByFileId(fileUUID1).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByFileId(fileUUID1)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectTaskByFileId(fileUUID1).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID1, fileUUID1, 0))), Duration.Inf)
    }
  }

  "TaskMappings#deleteTaskByPeriod" should {
    "delete a TaskRow by giving the corresponding period." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByPeriod(0).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByPeriod(0)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectTaskByPeriod(0).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID1, fileUUID1, 0))), Duration.Inf)
    }
  }

  "TaskMappings#deleteTaskByValue" should {
    "delete a TaskRow by giving the corresponding value." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByValue(3).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByValue(3)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectTaskByValue(3).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))), Duration.Inf)
    }
  }

  "TaskMappings#deleteTaskByStartDateAndTime" should {
    "delete a TaskRow by giving the corresponding startDateAndTime." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))), Duration.Inf)
      deleteResult mustBe 2
      val selectResult2 = Await.result(dtbase.run(selectTaskByStartDateAndTime(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))), Duration.Inf)
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some("PST")))), Duration.Inf)
    }
  }

  "TaskMappings#deleteTaskByEndDateAndTime" should {
    "delete a TaskRow by giving the corresponding endDateAndTime." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"))), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectTaskByEndDateAndTime(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some("PST")))), Duration.Inf)
    }
  }

  "TaskMappings#deleteTaskByTotalOccurrences" should {
    "delete a TaskRow by giving the corresponding totalOccurrences." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByTotalOccurrences(10).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByTotalOccurrences(10)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectTaskByTotalOccurrences(10).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))), Duration.Inf)
    }
  }

  "TaskMappings#deleteTaskByCurrentOccurrences" should {
    "delete a TaskRow by giving the corresponding currentOccurrences." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByCurrentOccurrences(10).result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByCurrentOccurrences(10)), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectTaskByCurrentOccurrences(10).result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID2, fileUUID2, 4, Some(3), Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, Some(10), Some(10)))), Duration.Inf)
    }
  }

  "TaskMappings$deleteTaskByTimezone" should {
    "delete a TaskRow by giving the corresponding timezone." in {
      val selectResult1 = Await.result(dtbase.run(selectTaskByTimezone("PST").result), Duration.Inf)
      selectResult1.nonEmpty mustBe true
      val deleteResult = Await.result(dtbase.run(deleteTaskByTimezone("PST")), Duration.Inf)
      deleteResult mustBe 1
      val selectResult2 = Await.result(dtbase.run(selectTaskByTimezone("PST").result), Duration.Inf)
      selectResult2.isEmpty mustBe true
      Await.result(dtbase.run(insertTask(TaskRow(taskUUID3, fileUUID3, 7, None, Some(stringToDateFormat("2030-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), Some(stringToDateFormat("2040-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss")), None, None, Some("PST")))), Duration.Inf)

    }
  }
}
