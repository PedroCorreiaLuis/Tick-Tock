package api.services

import api.services.TaskService._
import database.repositories.FileRepository._
import database.repositories.TaskRepository._

object TickTock{

  def retrieveDataFromDB = {
    println("retrieving data from DB")
    selectAllTasks.foreach(t => scheduleOnce(selectNameFromFileId(t.fileId), t.startDateAndTime))
  }

  def main(args: Array[String]): Unit = {

    retrieveDataFromDB
  }

}