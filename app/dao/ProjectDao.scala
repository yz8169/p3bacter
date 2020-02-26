package dao

import javax.inject.Inject

//import controllers.UserData
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ProjectDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def selectAll: Future[Seq[ProjectRow]] = db.run(Project.result)

  def selectAllProtein: Future[Seq[ProjectRow]] = db.run(Project.filter(_.kind==="蛋白组").result)

  def selectAllMetabolite: Future[Seq[ProjectRow]] = db.run(Project.filter(_.kind==="代谢组").result)

  def selectByProjectName(projectName: String): Future[Option[ProjectRow]] = db.run(Project.
    filter(_.projectname === projectName).result.headOption)

  def insert(row: ProjectRow): Future[Unit] = db.run(Project += row).map(_ => ())

  def deleteById(id: Int): Future[Unit] = db.run(Project.filter(_.id === id).delete).map(_ => ())

//  def selectByUserData(data: UserData): Future[Option[ProjectRow]] = db.run(Project.
//    filter(_.projectname === data.projectName).filter(_.password === data.password).result.headOption)

  def selectById(id: Int): Future[ProjectRow] = db.run(Project.filter(_.id === id).result.head)

}
