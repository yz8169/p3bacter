package dao

import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Created by yz on 2018/5/9
  */
class SampleDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def insertAll(rows: Seq[SampleRow]): Future[Unit] = db.run(Sample ++= rows).map(_ => ())

  def updateAll(rows: Seq[SampleRow]): Future[Unit] = {
    val action = {
      val sampleNames = rows.map(_.samplename)
      val deleteAll = Sample.filter(_.samplename.inSetBind(sampleNames)).delete
      val insertAll = Sample ++= rows
      deleteAll.flatMap(_ => insertAll)
    }.transactionally
    db.run(action).map(_ => ())
  }

  def selectAll: Future[Seq[SampleRow]] = db.run(Sample.result)

  def deleteBySampleName(sampleName: String): Future[Unit] = db.run(Sample.
    filter(_.samplename === sampleName).delete).map(_ => ())


}
