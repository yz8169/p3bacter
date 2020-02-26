package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by yz on 2018/5/9
  */
class AdminDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def selectById1: Future[AdminRow] = db.run(Admin.filter(_.id === 1).result.head)

  def update(row: AdminRow): Future[Unit] = {
    db.run(Admin.filter(_.id === row.id).update(row)).map(_ => ())
  }

}
