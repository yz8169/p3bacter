package utils

import java.io.File

import controllers.Tool
import dao.MissionDao
import javax.inject.Inject
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import models.Tables._
import play.api.mvc.RequestHeader

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by yz on 2018/6/13
  */
class MissionExecutor @Inject()(missionDao: MissionDao, tool: Tool, mission: MissionRow)(implicit request: RequestHeader) {
  var workspaceDir: File = _
  var resultDir: File = _
  var dbMissionId: Int = _
  var dbMission: MissionRow = _
  var logFile: File = _

  init

  def init = {
    val missionId = getDbMissionId
    val missionIdDir = tool.getMissionIdDirById(missionId)
    Utils.createDirectoryWhenNoExist(missionIdDir)
    workspaceDir = new File(missionIdDir, "workspace")
    Utils.createDirectoryWhenNoExist(workspaceDir)
    resultDir = new File(missionIdDir, "result")
    Utils.createDirectoryWhenNoExist(resultDir)
    logFile = new File(missionIdDir, "log.txt")
  }


  def exec(shBuffer: ArrayBuffer[String]) = {
    val f = Future {
      val execCommand = Utils.callScript(workspaceDir, shBuffer)
      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(logFile, "Run successed!", "UTF-8")
        dbMission.copy(state = "success", endtime = Some(new DateTime()))
      } else {
        FileUtils.writeStringToFile(logFile, execCommand.getErrStr, "UTF-8")
        dbMission.copy(state = "error", endtime = Some(new DateTime()))
      }
    }
    f.flatMap { updateMission =>
      missionDao.update(updateMission)
    }
  }

  def execLinux(shBuffer: ArrayBuffer[String]) = {
    val f = Future {
      val execCommand = Utils.callLinuxScript(workspaceDir, shBuffer)
      if (execCommand.isSuccess) {
        FileUtils.writeStringToFile(logFile, "Run successed!", "UTF-8")
        dbMission.copy(state = "success", endtime = Some(new DateTime()))
      } else {
        FileUtils.writeStringToFile(logFile, execCommand.getErrStr, "UTF-8")
        dbMission.copy(state = "error", endtime = Some(new DateTime()))
      }
    }
    f.flatMap { updateMission =>
      missionDao.update(updateMission)
    }
  }

  def getDbMissionId = {
    if (dbMissionId == 0) {
      val f = missionDao.insert(mission).flatMap(_ => missionDao.selectByMissionName(mission.userid, mission.missionname))
      dbMission = Utils.execFuture(f)
      dbMissionId = dbMission.id
    }
    dbMissionId
  }

}
