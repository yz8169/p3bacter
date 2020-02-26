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
class VirusGeneInfoDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def insertAll(rows: Seq[GeneinfoRow]): Future[Unit] = db.run(Geneinfo ++= rows).map(_ => ())

  def updateAll(rows: Seq[VirusGeneInfoRow]): Future[Unit] = {
    val action = {
      val sampleNames = rows.map(_.samplename)
      val deleteAll = VirusGeneInfo.filter(_.samplename.inSetBind(sampleNames)).delete
      val insertAll = VirusGeneInfo ++= rows
      deleteAll.flatMap(_ => insertAll)
    }.transactionally
    db.run(action).map(_ => ())
  }

  def deleteBySampleName(sampleName: String): Future[Unit] = db.run(Geneinfo.
    filter(_.samplename === sampleName).delete).map(_ => ())

  def selectAll(sampleName: String): Future[Seq[GeneinfoRow]] = db.run(Geneinfo.filter(_.samplename === sampleName).result)


}
