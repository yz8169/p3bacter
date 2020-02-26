package dao

import javax.inject.Inject

import controllers._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.Future
import models.Tables._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by yz on 2017/6/29.
  */
class ClassifyDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def selectAll: Future[Seq[ClassifyRow]] = db.run(Classify.result)

  def selectGetAllPhylumns: Future[Seq[String]] = db.run(Classify.map(_.phylum).distinct.result)

  def selectGetAllOutlines: Future[Seq[String]] = db.run(Classify.map(_.outline).distinct.result)

  def selectGetAllOrders: Future[Seq[String]] = db.run(Classify.map(_.order).distinct.result)

  def selectGetAllFamilys: Future[Seq[String]] = db.run(Classify.map(_.family).distinct.result)

  def selectGetAllGenus: Future[Seq[String]] = db.run(Classify.map(_.genus).distinct.result)

  def selectGetAllSpecies: Future[Seq[String]] = db.run(Classify.map(_.species).distinct.result)

  def insertAll(rows: Seq[ClassifyRow]): Future[Unit] = {
    db.run(Classify ++= rows).map(_ => ())
  }

  def updateAll(rows: Seq[ClassifyRow]): Future[Unit] = {
    val action = {
      val sampleNames = rows.map(_.id)
      val deleteAll = Classify.filter(_.id.inSetBind(sampleNames)).delete
      val insertAll = Classify ++= rows
      deleteAll.flatMap(_ => insertAll)
    }.transactionally
    db.run(action).map(_ => ())
  }

  def selectById(id: String): Future[Option[ClassifyRow]] = db.run(Classify.filter(_.id === id).result.headOption)

  def selectByPhylum(phylum: String): Future[Seq[ClassifyRow]] = db.run(Classify.filter(_.phylum === phylum).result)

    def selectGetOrderInfo(data: ClassifyData): Future[Seq[ClassifyRow]] = db.run(Classify.filter(_.phylum === data.phylum).
      filter(_.outline === data.outline).result)

    def selectGetFamilyInfo(data: ClassifyData): Future[Seq[ClassifyRow]] = db.run(Classify.filter(_.phylum === data.phylum).
      filter(_.outline === data.outline).filter(_.order === data.order).result)

    def selectGetGenusInfo(data: ClassifyData): Future[Seq[ClassifyRow]] = db.run(Classify.filter(_.phylum === data.phylum).
      filter(_.outline === data.outline).filter(_.order === data.order).filter(_.family === data.family).result)

    def selectGetSpeciesInfo(data: ClassifyData): Future[Seq[ClassifyRow]] = db.run(Classify.filter(_.phylum === data.phylum).
      filter(_.outline === data.outline).filter(_.order === data.order).filter(_.family === data.family).filter(_.genus === data.genus).
      result)

//    def selectGetAllIdByData(data: SearchClassifyData): Future[Seq[String]] = db.run(Classify.
//      filter { x =>
//        data.phylum match {
//          case None => x.phylum === x.phylum
//          case Some(y) => x.phylum === y.head
//        }
//      }.filter { x =>
//      data.outline match {
//        case None => x.phylum === x.phylum
//        case Some(y) => x.outline === y.head
//      }
//    }.filter { x =>
//      data.order match {
//        case None => x.phylum === x.phylum
//        case Some(y) => x.order === y.head
//      }
//    }.filter { x =>
//      data.family match {
//        case None => x.phylum === x.phylum
//        case Some(y) => x.family === y.head
//      }
//    }.filter { x =>
//      data.genus match {
//        case None => x.phylum === x.phylum
//        case Some(y) => x.genus === y.head
//      }
//    }.filter { x =>
//      data.species match {
//        case None => x.phylum === x.phylum
//        case Some(y) => x.species === y.head
//      }
//    }.map(_.id).result)

    def selectGetIdInfo(data: ClassifyData): Future[Seq[ClassifyRow]] = db.run(Classify.filter(_.phylum === data.phylum).
      filter(_.outline === data.outline).filter(_.order === data.order).filter(_.family === data.family).
      filter(_.genus === data.genus).filter(_.species === data.species).result)

  def insert(row: ClassifyRow): Future[Unit] = {
    db.run(Classify += row).map(_ => ())
  }

  def deleteByIds(ids: Seq[String]): Future[Unit] = {
    db.run(Classify.filter(_.id.inSetBind(ids)).delete).map(_ => ())
  }

  def update(row: ClassifyRow): Future[Unit] = {
    db.run(Classify.filter(_.id === row.id).update(row)).map(_ => ())
  }

  def deleteById(id: String): Future[Unit] = {
    db.run(Classify.filter(_.id === id).delete).map(_ => ())
  }


}
