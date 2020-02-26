package dao

import javax.inject.Inject
import models.Tables
import models.Tables._
import play.api.Play
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.db.NamedDatabase
import slick.ast.TypedType
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by yz on 2018/4/27
  */
class MissionDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def insert(row: MissionRow): Future[Unit] = db.run(Mission += row).map(_ => ())

  def selectByMissionName(userId: Int, missionName: String): Future[MissionRow] = db.run(Mission.
    filter(_.userid === userId).filter(_.missionname === missionName).result.head)

  def selectOptionByMissionName(userId: Int, missionName: String): Future[Option[MissionRow]] = db.run(Mission.
    filter(_.userid === userId).filter(_.missionname === missionName).result.headOption)

  def selectOptionByMissionAttr(userId: Int, missionName: String,kind:String): Future[Option[MissionRow]] = db.run(Mission.
    filter(_.userid === userId).filter(_.kind===kind).filter(_.missionname === missionName).result.headOption)

  def selectByMissionId(userId: Int, missionId: Int): Future[MissionRow] = db.run(Mission.
    filter(_.userid === userId).filter(_.id === missionId).result.head)

  def update(row: MissionRow): Future[Unit] = db.run(Mission.filter(_.id === row.id).update(row)).map(_ => ())

  def selectAll(userId: Int): Future[Seq[MissionRow]] = db.run(Mission.filter(_.userid === userId).sortBy(_.id.desc).result)

  def selectAll(userId: Int, kind: String): Future[Seq[MissionRow]] = db.run(Mission.filter(_.userid === userId).
    filter(_.kind === kind).sortBy(_.id.desc).result)

  def deleteById(id: Int): Future[Unit] = db.run(Mission.filter(_.id === id).delete).map(_ => ())

  def deleteByUserId(userId: Int): Future[Unit] = db.run(Mission.filter(_.userid === userId).delete).map(_ => ())

  def test = {
    val action = Mission.result
    val action1 = Mission.result
    db.run(action.transactionally)
  }


}
