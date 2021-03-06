package api.controllers

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import api.dtos.FileDTO
import api.utils.DateUtils.{getCurrentDateTimestamp, stringToDateFormat}
import com.google.inject.Guice
import database.repositories.{FileRepository, TaskRepository}
import database.utils.DatabaseUtils.TEST_DB
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._
import api.validators.Error._
import slick.jdbc.meta.MTable
import slick.jdbc.MySQLProfile.api._
import database.mappings.FileMappings._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

class FileFunctionalSuite extends PlaySpec with GuiceOneAppPerSuite with BeforeAndAfterAll with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  lazy val appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
  Guice.createInjector(appBuilder.applicationModule).injectMembers(this)
  implicit val fileRepo: FileRepository = appBuilder.injector.instanceOf[FileRepository]
  implicit val taskRepo: TaskRepository = appBuilder.injector.instanceOf[TaskRepository]
  val dtbase: Database = appBuilder.injector.instanceOf[Database]
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()

  val LOCALHOST = "localhost:9000"

  val fileUUID1: String = UUID.randomUUID().toString
  val fileUUID2: String = UUID.randomUUID().toString
  val fileUUID3: String = UUID.randomUUID().toString
  val fileUUID4: String = UUID.randomUUID().toString

  override def beforeAll = {
    val result = for {
      _ <- dtbase.run(createFilesTableAction)
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID1, "test1", stringToDateFormat("01-01-2018 12:00:00", "dd-MM-yyyy HH:mm:ss")))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID2, "test2", stringToDateFormat("01-02-2018 12:00:00", "dd-MM-yyyy HH:mm:ss")))
      _ <- fileRepo.insertInFilesTable(FileDTO(fileUUID3, "test3", stringToDateFormat("01-03-2018 12:00:00", "dd-MM-yyyy HH:mm:ss")))
      res <- fileRepo.insertInFilesTable(FileDTO(fileUUID4, "test4", stringToDateFormat("01-04-2018 12:00:00", "dd-MM-yyyy HH:mm:ss")))
    } yield res
    Await.result(result, Duration.Inf)

  }

  override def afterAll = {
    Await.result(dtbase.run(dropFilesTableAction), Duration.Inf)
  }

  "FileController#GETfile" should {
    "receive a GET request" in {
      val fakeRequest = FakeRequest(GET, s"/file")
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      val bodyText = contentAsString(result.get)
      status(result.get) mustBe OK
      bodyText mustBe """[{"fileId":""" + "\"" + fileUUID1 + "\"" +
        ""","fileName":"test1","uploadDate":1514808000000},{"fileId":""" + "\"" + fileUUID2 + "\"" +
        ""","fileName":"test2","uploadDate":1517486400000},{"fileId":""" + "\"" + fileUUID3 + "\"" +
        ""","fileName":"test3","uploadDate":1519905600000},{"fileId":""" + "\"" + fileUUID4 + "\"" + ""","fileName":"test4","uploadDate":1522580400000}]"""
    }
  }

  "FileController#GETfileWithId" should {
    "receive a GET request with a valid id" in {
      val toGet = fileUUID2
      val fakeRequest = FakeRequest(GET, s"/file/" + toGet)
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      val bodyText = contentAsString(result.get)
      status(result.get) mustBe OK
      bodyText mustBe """{"fileId":""" + "\"" + toGet + "\"" + ""","fileName":"test2","uploadDate":1517486400000}"""
    }

    "receive a GET request with an invalid id" in {
      val toGet = "asd"
      val fakeRequest = FakeRequest(GET, s"/file/" + toGet)
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      val bodyText = contentAsString(result.get)
      status(result.get) mustBe BAD_REQUEST
      bodyText mustBe Json.toJsObject(invalidFileName).toString
    }
  }

  "FileController#DELETEfileWithId" should {
    "receive a DELETE request with a valid id" in {
      val toDelete = fileUUID4
      val fakeRequest = FakeRequest(DELETE, s"/file/" + toDelete)
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      val bodyText = contentAsString(result.get)
      status(result.get) mustBe NO_CONTENT
      bodyText mustBe ""
      Await.result(fileRepo.insertInFilesTable(FileDTO(fileUUID4, "test4", stringToDateFormat("01-04-2018 12:00:00", "dd-MM-yyyy HH:mm:ss"))), Duration.Inf)
    }

    "receive a DELETE request with an invalid id" in {
      val toDelete = "asd"
      val fakeRequest = FakeRequest(DELETE, s"/file/" + toDelete)
        .withHeaders(HOST -> "localhost:9000")
      val result = route(app, fakeRequest)
      val bodyText = contentAsString(result.get)
      status(result.get) mustBe BAD_REQUEST
      bodyText mustBe Json.toJsObject(invalidEndpointId).toString
    }
  }
}
