package dao

import javax.inject.Inject

import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GroupDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def selectAll(projectName: String): Future[Seq[GroupRow]] = db.run(Group.filter(_.projectname === projectName).result)

  def selectByGroupName(projectName: String, name: String): Future[Option[GroupRow]] = db.run(Group.
    filter(_.projectname === projectName).filter(_.groupname === name).result.headOption)

  def selectById(id: Int): Future[GroupRow] = db.run(Group.
    filter(_.id === id).result.head)

  def insert(row: GroupRow): Future[Unit] = db.run(Group += row).map(_ => ())

  def deleteById(id: Int): Future[Unit] = db.run(Group.filter(_.id === id).delete).map(_ => ())

  def deleteByProjectName(projectName: String): Future[Unit] = db.run(Group.filter(_.projectname === projectName).delete).map(_ => ())

  def update(id: Int, relation: Option[String]): Future[Unit] = db.run(Group.filter(_.id === id).map(_.relation).update(relation)).map(_ => ())


}
