package dao

import controllers.UserData
import javax.inject.Inject
import models.Tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by yz on 2018/4/8
  */
class UserDao @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends
  HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  def insert(row: UserRow): Future[Unit] = db.run(User += row).map(_ => ())

  def selectByAccount(account: String): Future[Option[UserRow]] =
    db.run(User.filter(_.account === account).result.headOption)

  def selectByUserData(data: UserData): Future[Option[UserRow]] =
    db.run(User.filter(_.account === data.account).filter(_.password === data.password).result.headOption)

  def selectAll: Future[Seq[UserRow]] = db.run(User.result)

  def selectById(id: Int): Future[UserRow] =
    db.run(User.filter(_.id === id).result.head)

  def update(row: UserRow): Future[Unit] = db.run(User.filter(_.id === row.id).update(row).map(_ => ()))

  def deleteById(id: Int): Future[Unit] = {
    db.run(User.filter(_.id === id).delete).map(_ => ())
  }

  def updatePassword(id: Int, password: String): Future[Unit] = db.run(User.filter(_.id === id).map(_.password).
    update(password).map(_ => ()))

}
