package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Created by yz on 2018/4/18
  */
class ProteinDealDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def insertOrUpdate(row: ProteindealRow): Future[Unit] = db.run(Proteindeal.insertOrUpdate(row)).map(_ => ())

  def selectByProjectName(projectName:String): Future[Option[ProteindealRow]] = db.run(Proteindeal.
    filter(_.projectname===projectName).result.headOption)



}
