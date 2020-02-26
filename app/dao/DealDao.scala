package dao

import javax.inject.Inject

import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class DealDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def insertOrUpdate(row: DealRow): Future[Unit] = db.run(Deal.insertOrUpdate(row)).map(_ => ())

  def selectByProjectName(projectName:String): Future[Option[DealRow]] = db.run(Deal.
    filter(_.projectname===projectName).result.headOption)

  def deleteByProjectName(projectName: String): Future[Unit] = db.run(Deal.filter(_.projectname === projectName).
    delete).map(_ => ())

}
