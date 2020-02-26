package models
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = slick.jdbc.MySQLProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  import com.github.tototoshi.slick.MySQLJodaSupport._
  import org.joda.time.DateTime
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(Admin.schema, Classify.schema, Deal.schema, Geneinfo.schema, Group.schema, Mission.schema, Mode.schema, Project.schema, Proteindeal.schema, Sample.schema, User.schema, VirusGeneInfo.schema, VirusSample.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Admin
   *  @param id Database column id SqlType(INT), PrimaryKey
   *  @param account Database column account SqlType(VARCHAR), Length(255,true)
   *  @param password Database column password SqlType(VARCHAR), Length(255,true) */
  final case class AdminRow(id: Int, account: String, password: String)
  /** GetResult implicit for fetching AdminRow objects using plain SQL queries */
  implicit def GetResultAdminRow(implicit e0: GR[Int], e1: GR[String]): GR[AdminRow] = GR{
    prs => import prs._
    AdminRow.tupled((<<[Int], <<[String], <<[String]))
  }
  /** Table description of table admin. Objects of this class serve as prototypes for rows in queries. */
  class Admin(_tableTag: Tag) extends profile.api.Table[AdminRow](_tableTag, Some("p3"), "admin") {
    def * = (id, account, password) <> (AdminRow.tupled, AdminRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(account), Rep.Some(password)).shaped.<>({r=>import r._; _1.map(_=> AdminRow.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column account SqlType(VARCHAR), Length(255,true) */
    val account: Rep[String] = column[String]("account", O.Length(255,varying=true))
    /** Database column password SqlType(VARCHAR), Length(255,true) */
    val password: Rep[String] = column[String]("password", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table Admin */
  lazy val Admin = new TableQuery(tag => new Admin(tag))

  /** Entity class storing rows of table Classify
   *  @param id Database column id SqlType(VARCHAR), PrimaryKey, Length(255,true)
   *  @param phylum Database column phylum SqlType(VARCHAR), Length(255,true)
   *  @param outline Database column outline SqlType(VARCHAR), Length(255,true)
   *  @param order Database column order SqlType(VARCHAR), Length(255,true)
   *  @param family Database column family SqlType(VARCHAR), Length(255,true)
   *  @param genus Database column genus SqlType(VARCHAR), Length(255,true)
   *  @param species Database column species SqlType(VARCHAR), Length(255,true) */
  final case class ClassifyRow(id: String, phylum: String, outline: String, order: String, family: String, genus: String, species: String)
  /** GetResult implicit for fetching ClassifyRow objects using plain SQL queries */
  implicit def GetResultClassifyRow(implicit e0: GR[String]): GR[ClassifyRow] = GR{
    prs => import prs._
    ClassifyRow.tupled((<<[String], <<[String], <<[String], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table classify. Objects of this class serve as prototypes for rows in queries. */
  class Classify(_tableTag: Tag) extends profile.api.Table[ClassifyRow](_tableTag, Some("p3"), "classify") {
    def * = (id, phylum, outline, order, family, genus, species) <> (ClassifyRow.tupled, ClassifyRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(phylum), Rep.Some(outline), Rep.Some(order), Rep.Some(family), Rep.Some(genus), Rep.Some(species)).shaped.<>({r=>import r._; _1.map(_=> ClassifyRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(VARCHAR), PrimaryKey, Length(255,true) */
    val id: Rep[String] = column[String]("id", O.PrimaryKey, O.Length(255,varying=true))
    /** Database column phylum SqlType(VARCHAR), Length(255,true) */
    val phylum: Rep[String] = column[String]("phylum", O.Length(255,varying=true))
    /** Database column outline SqlType(VARCHAR), Length(255,true) */
    val outline: Rep[String] = column[String]("outline", O.Length(255,varying=true))
    /** Database column order SqlType(VARCHAR), Length(255,true) */
    val order: Rep[String] = column[String]("order", O.Length(255,varying=true))
    /** Database column family SqlType(VARCHAR), Length(255,true) */
    val family: Rep[String] = column[String]("family", O.Length(255,varying=true))
    /** Database column genus SqlType(VARCHAR), Length(255,true) */
    val genus: Rep[String] = column[String]("genus", O.Length(255,varying=true))
    /** Database column species SqlType(VARCHAR), Length(255,true) */
    val species: Rep[String] = column[String]("species", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table Classify */
  lazy val Classify = new TableQuery(tag => new Classify(tag))

  /** Entity class storing rows of table Deal
   *  @param projectname Database column projectName SqlType(VARCHAR), PrimaryKey, Length(255,true)
   *  @param delete Database column delete SqlType(VARCHAR), Length(255,true), Default(None)
   *  @param iqr Database column iqr SqlType(DOUBLE)
   *  @param replace Database column replace SqlType(VARCHAR), Length(255,true), Default(None)
   *  @param replacemethod Database column replaceMethod SqlType(VARCHAR), Length(255,true)
   *  @param rate Database column rate SqlType(DOUBLE)
   *  @param assignvalue Database column assignValue SqlType(VARCHAR), Length(255,true)
   *  @param kvalue Database column kValue SqlType(INT)
   *  @param normal Database column normal SqlType(VARCHAR), Length(255,true), Default(None)
   *  @param normalmethod Database column normalMethod SqlType(VARCHAR), Length(255,true)
   *  @param rowname Database column rowName SqlType(VARCHAR), Length(255,true)
   *  @param colname Database column colName SqlType(VARCHAR), Length(255,true)
   *  @param coefficient Database column coefficient SqlType(DOUBLE) */
  final case class DealRow(projectname: String, delete: Option[String] = None, iqr: Double, replace: Option[String] = None, replacemethod: String, rate: Double, assignvalue: String, kvalue: Int, normal: Option[String] = None, normalmethod: String, rowname: String, colname: String, coefficient: Double)
  /** GetResult implicit for fetching DealRow objects using plain SQL queries */
  implicit def GetResultDealRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[Double], e3: GR[Int]): GR[DealRow] = GR{
    prs => import prs._
    DealRow.tupled((<<[String], <<?[String], <<[Double], <<?[String], <<[String], <<[Double], <<[String], <<[Int], <<?[String], <<[String], <<[String], <<[String], <<[Double]))
  }
  /** Table description of table deal. Objects of this class serve as prototypes for rows in queries. */
  class Deal(_tableTag: Tag) extends profile.api.Table[DealRow](_tableTag, Some("p3"), "deal") {
    def * = (projectname, delete, iqr, replace, replacemethod, rate, assignvalue, kvalue, normal, normalmethod, rowname, colname, coefficient) <> (DealRow.tupled, DealRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(projectname), delete, Rep.Some(iqr), replace, Rep.Some(replacemethod), Rep.Some(rate), Rep.Some(assignvalue), Rep.Some(kvalue), normal, Rep.Some(normalmethod), Rep.Some(rowname), Rep.Some(colname), Rep.Some(coefficient)).shaped.<>({r=>import r._; _1.map(_=> DealRow.tupled((_1.get, _2, _3.get, _4, _5.get, _6.get, _7.get, _8.get, _9, _10.get, _11.get, _12.get, _13.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column projectName SqlType(VARCHAR), PrimaryKey, Length(255,true) */
    val projectname: Rep[String] = column[String]("projectName", O.PrimaryKey, O.Length(255,varying=true))
    /** Database column delete SqlType(VARCHAR), Length(255,true), Default(None) */
    val delete: Rep[Option[String]] = column[Option[String]]("delete", O.Length(255,varying=true), O.Default(None))
    /** Database column iqr SqlType(DOUBLE) */
    val iqr: Rep[Double] = column[Double]("iqr")
    /** Database column replace SqlType(VARCHAR), Length(255,true), Default(None) */
    val replace: Rep[Option[String]] = column[Option[String]]("replace", O.Length(255,varying=true), O.Default(None))
    /** Database column replaceMethod SqlType(VARCHAR), Length(255,true) */
    val replacemethod: Rep[String] = column[String]("replaceMethod", O.Length(255,varying=true))
    /** Database column rate SqlType(DOUBLE) */
    val rate: Rep[Double] = column[Double]("rate")
    /** Database column assignValue SqlType(VARCHAR), Length(255,true) */
    val assignvalue: Rep[String] = column[String]("assignValue", O.Length(255,varying=true))
    /** Database column kValue SqlType(INT) */
    val kvalue: Rep[Int] = column[Int]("kValue")
    /** Database column normal SqlType(VARCHAR), Length(255,true), Default(None) */
    val normal: Rep[Option[String]] = column[Option[String]]("normal", O.Length(255,varying=true), O.Default(None))
    /** Database column normalMethod SqlType(VARCHAR), Length(255,true) */
    val normalmethod: Rep[String] = column[String]("normalMethod", O.Length(255,varying=true))
    /** Database column rowName SqlType(VARCHAR), Length(255,true) */
    val rowname: Rep[String] = column[String]("rowName", O.Length(255,varying=true))
    /** Database column colName SqlType(VARCHAR), Length(255,true) */
    val colname: Rep[String] = column[String]("colName", O.Length(255,varying=true))
    /** Database column coefficient SqlType(DOUBLE) */
    val coefficient: Rep[Double] = column[Double]("coefficient")
  }
  /** Collection-like TableQuery object for table Deal */
  lazy val Deal = new TableQuery(tag => new Deal(tag))

  /** Entity class storing rows of table Geneinfo
   *  @param samplename Database column sampleName SqlType(VARCHAR), Length(255,true)
   *  @param proteinid Database column proteinId SqlType(VARCHAR), Length(255,true)
   *  @param seqname Database column seqName SqlType(VARCHAR), Length(255,true)
   *  @param start Database column start SqlType(INT)
   *  @param end Database column end SqlType(INT)
   *  @param strand Database column strand SqlType(VARCHAR), Length(255,true)
   *  @param product Database column product SqlType(LONGTEXT), Length(2147483647,true)
   *  @param gene Database column gene SqlType(LONGTEXT), Length(2147483647,true)
   *  @param pep Database column pep SqlType(LONGTEXT), Length(2147483647,true) */
  final case class GeneinfoRow(samplename: String, proteinid: String, seqname: String, start: Int, end: Int, strand: String, product: String, gene: String, pep: String)
  /** GetResult implicit for fetching GeneinfoRow objects using plain SQL queries */
  implicit def GetResultGeneinfoRow(implicit e0: GR[String], e1: GR[Int]): GR[GeneinfoRow] = GR{
    prs => import prs._
    GeneinfoRow.tupled((<<[String], <<[String], <<[String], <<[Int], <<[Int], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table geneinfo. Objects of this class serve as prototypes for rows in queries. */
  class Geneinfo(_tableTag: Tag) extends profile.api.Table[GeneinfoRow](_tableTag, Some("p3"), "geneinfo") {
    def * = (samplename, proteinid, seqname, start, end, strand, product, gene, pep) <> (GeneinfoRow.tupled, GeneinfoRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(samplename), Rep.Some(proteinid), Rep.Some(seqname), Rep.Some(start), Rep.Some(end), Rep.Some(strand), Rep.Some(product), Rep.Some(gene), Rep.Some(pep)).shaped.<>({r=>import r._; _1.map(_=> GeneinfoRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column sampleName SqlType(VARCHAR), Length(255,true) */
    val samplename: Rep[String] = column[String]("sampleName", O.Length(255,varying=true))
    /** Database column proteinId SqlType(VARCHAR), Length(255,true) */
    val proteinid: Rep[String] = column[String]("proteinId", O.Length(255,varying=true))
    /** Database column seqName SqlType(VARCHAR), Length(255,true) */
    val seqname: Rep[String] = column[String]("seqName", O.Length(255,varying=true))
    /** Database column start SqlType(INT) */
    val start: Rep[Int] = column[Int]("start")
    /** Database column end SqlType(INT) */
    val end: Rep[Int] = column[Int]("end")
    /** Database column strand SqlType(VARCHAR), Length(255,true) */
    val strand: Rep[String] = column[String]("strand", O.Length(255,varying=true))
    /** Database column product SqlType(LONGTEXT), Length(2147483647,true) */
    val product: Rep[String] = column[String]("product", O.Length(2147483647,varying=true))
    /** Database column gene SqlType(LONGTEXT), Length(2147483647,true) */
    val gene: Rep[String] = column[String]("gene", O.Length(2147483647,varying=true))
    /** Database column pep SqlType(LONGTEXT), Length(2147483647,true) */
    val pep: Rep[String] = column[String]("pep", O.Length(2147483647,varying=true))

    /** Primary key of Geneinfo (database name geneinfo_PK) */
    val pk = primaryKey("geneinfo_PK", (samplename, proteinid))
  }
  /** Collection-like TableQuery object for table Geneinfo */
  lazy val Geneinfo = new TableQuery(tag => new Geneinfo(tag))

  /** Entity class storing rows of table Group
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param projectname Database column projectName SqlType(VARCHAR), Length(255,true)
   *  @param groupname Database column groupName SqlType(VARCHAR), Length(255,true)
   *  @param content Database column content SqlType(VARCHAR), Length(255,true)
   *  @param relation Database column relation SqlType(VARCHAR), Length(255,true), Default(None) */
  final case class GroupRow(id: Int, projectname: String, groupname: String, content: String, relation: Option[String] = None)
  /** GetResult implicit for fetching GroupRow objects using plain SQL queries */
  implicit def GetResultGroupRow(implicit e0: GR[Int], e1: GR[String], e2: GR[Option[String]]): GR[GroupRow] = GR{
    prs => import prs._
    GroupRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<?[String]))
  }
  /** Table description of table group. Objects of this class serve as prototypes for rows in queries. */
  class Group(_tableTag: Tag) extends profile.api.Table[GroupRow](_tableTag, Some("p3"), "group") {
    def * = (id, projectname, groupname, content, relation) <> (GroupRow.tupled, GroupRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(projectname), Rep.Some(groupname), Rep.Some(content), relation).shaped.<>({r=>import r._; _1.map(_=> GroupRow.tupled((_1.get, _2.get, _3.get, _4.get, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column projectName SqlType(VARCHAR), Length(255,true) */
    val projectname: Rep[String] = column[String]("projectName", O.Length(255,varying=true))
    /** Database column groupName SqlType(VARCHAR), Length(255,true) */
    val groupname: Rep[String] = column[String]("groupName", O.Length(255,varying=true))
    /** Database column content SqlType(VARCHAR), Length(255,true) */
    val content: Rep[String] = column[String]("content", O.Length(255,varying=true))
    /** Database column relation SqlType(VARCHAR), Length(255,true), Default(None) */
    val relation: Rep[Option[String]] = column[Option[String]]("relation", O.Length(255,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table Group */
  lazy val Group = new TableQuery(tag => new Group(tag))

  /** Entity class storing rows of table Mission
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param missionname Database column missionName SqlType(VARCHAR), Length(255,true)
   *  @param userid Database column userId SqlType(INT)
   *  @param kind Database column kind SqlType(VARCHAR), Length(255,true)
   *  @param args Database column args SqlType(TEXT)
   *  @param starttime Database column startTime SqlType(DATETIME)
   *  @param endtime Database column endTime SqlType(DATETIME), Default(None)
   *  @param state Database column state SqlType(VARCHAR), Length(255,true) */
  final case class MissionRow(id: Int, missionname: String, userid: Int, kind: String, args: String, starttime: DateTime, endtime: Option[DateTime] = None, state: String)
  /** GetResult implicit for fetching MissionRow objects using plain SQL queries */
  implicit def GetResultMissionRow(implicit e0: GR[Int], e1: GR[String], e2: GR[DateTime], e3: GR[Option[DateTime]]): GR[MissionRow] = GR{
    prs => import prs._
    MissionRow.tupled((<<[Int], <<[String], <<[Int], <<[String], <<[String], <<[DateTime], <<?[DateTime], <<[String]))
  }
  /** Table description of table mission. Objects of this class serve as prototypes for rows in queries. */
  class Mission(_tableTag: Tag) extends profile.api.Table[MissionRow](_tableTag, Some("p3"), "mission") {
    def * = (id, missionname, userid, kind, args, starttime, endtime, state) <> (MissionRow.tupled, MissionRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(missionname), Rep.Some(userid), Rep.Some(kind), Rep.Some(args), Rep.Some(starttime), endtime, Rep.Some(state)).shaped.<>({r=>import r._; _1.map(_=> MissionRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column missionName SqlType(VARCHAR), Length(255,true) */
    val missionname: Rep[String] = column[String]("missionName", O.Length(255,varying=true))
    /** Database column userId SqlType(INT) */
    val userid: Rep[Int] = column[Int]("userId")
    /** Database column kind SqlType(VARCHAR), Length(255,true) */
    val kind: Rep[String] = column[String]("kind", O.Length(255,varying=true))
    /** Database column args SqlType(TEXT) */
    val args: Rep[String] = column[String]("args")
    /** Database column startTime SqlType(DATETIME) */
    val starttime: Rep[DateTime] = column[DateTime]("startTime")
    /** Database column endTime SqlType(DATETIME), Default(None) */
    val endtime: Rep[Option[DateTime]] = column[Option[DateTime]]("endTime", O.Default(None))
    /** Database column state SqlType(VARCHAR), Length(255,true) */
    val state: Rep[String] = column[String]("state", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table Mission */
  lazy val Mission = new TableQuery(tag => new Mission(tag))

  /** Entity class storing rows of table Mode
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param test Database column test SqlType(VARCHAR), Length(255,true) */
  final case class ModeRow(id: Int, test: String)
  /** GetResult implicit for fetching ModeRow objects using plain SQL queries */
  implicit def GetResultModeRow(implicit e0: GR[Int], e1: GR[String]): GR[ModeRow] = GR{
    prs => import prs._
    ModeRow.tupled((<<[Int], <<[String]))
  }
  /** Table description of table mode. Objects of this class serve as prototypes for rows in queries. */
  class Mode(_tableTag: Tag) extends profile.api.Table[ModeRow](_tableTag, Some("p3"), "mode") {
    def * = (id, test) <> (ModeRow.tupled, ModeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(test)).shaped.<>({r=>import r._; _1.map(_=> ModeRow.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column test SqlType(VARCHAR), Length(255,true) */
    val test: Rep[String] = column[String]("test", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table Mode */
  lazy val Mode = new TableQuery(tag => new Mode(tag))

  /** Entity class storing rows of table Project
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param projectname Database column projectName SqlType(VARCHAR), Length(255,true)
   *  @param describe Database column describe SqlType(VARCHAR), Length(255,true)
   *  @param kind Database column kind SqlType(VARCHAR), Length(255,true)
   *  @param uploadtime Database column uploadTime SqlType(DATETIME) */
  final case class ProjectRow(id: Int, projectname: String, describe: String, kind: String, uploadtime: DateTime)
  /** GetResult implicit for fetching ProjectRow objects using plain SQL queries */
  implicit def GetResultProjectRow(implicit e0: GR[Int], e1: GR[String], e2: GR[DateTime]): GR[ProjectRow] = GR{
    prs => import prs._
    ProjectRow.tupled((<<[Int], <<[String], <<[String], <<[String], <<[DateTime]))
  }
  /** Table description of table project. Objects of this class serve as prototypes for rows in queries. */
  class Project(_tableTag: Tag) extends profile.api.Table[ProjectRow](_tableTag, Some("p3"), "project") {
    def * = (id, projectname, describe, kind, uploadtime) <> (ProjectRow.tupled, ProjectRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(projectname), Rep.Some(describe), Rep.Some(kind), Rep.Some(uploadtime)).shaped.<>({r=>import r._; _1.map(_=> ProjectRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column projectName SqlType(VARCHAR), Length(255,true) */
    val projectname: Rep[String] = column[String]("projectName", O.Length(255,varying=true))
    /** Database column describe SqlType(VARCHAR), Length(255,true) */
    val describe: Rep[String] = column[String]("describe", O.Length(255,varying=true))
    /** Database column kind SqlType(VARCHAR), Length(255,true) */
    val kind: Rep[String] = column[String]("kind", O.Length(255,varying=true))
    /** Database column uploadTime SqlType(DATETIME) */
    val uploadtime: Rep[DateTime] = column[DateTime]("uploadTime")
  }
  /** Collection-like TableQuery object for table Project */
  lazy val Project = new TableQuery(tag => new Project(tag))

  /** Entity class storing rows of table Proteindeal
   *  @param projectname Database column projectName SqlType(VARCHAR), PrimaryKey, Length(255,true)
   *  @param delete Database column delete SqlType(VARCHAR), Length(255,true), Default(None)
   *  @param iqr Database column iqr SqlType(DOUBLE)
   *  @param replace Database column replace SqlType(VARCHAR), Length(255,true), Default(None)
   *  @param replacemethod Database column replaceMethod SqlType(VARCHAR), Length(255,true)
   *  @param rate Database column rate SqlType(DOUBLE)
   *  @param assignvalue Database column assignValue SqlType(VARCHAR), Length(255,true)
   *  @param kvalue Database column kValue SqlType(INT)
   *  @param normal Database column normal SqlType(VARCHAR), Length(255,true), Default(None)
   *  @param normalmethod Database column normalMethod SqlType(INT), Default(None)
   *  @param colname Database column colName SqlType(VARCHAR), Length(255,true)
   *  @param coefficient Database column coefficient SqlType(DOUBLE) */
  final case class ProteindealRow(projectname: String, delete: Option[String] = None, iqr: Double, replace: Option[String] = None, replacemethod: String, rate: Double, assignvalue: String, kvalue: Int, normal: Option[String] = None, normalmethod: Option[Int] = None, colname: String, coefficient: Double)
  /** GetResult implicit for fetching ProteindealRow objects using plain SQL queries */
  implicit def GetResultProteindealRow(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[Double], e3: GR[Int], e4: GR[Option[Int]]): GR[ProteindealRow] = GR{
    prs => import prs._
    ProteindealRow.tupled((<<[String], <<?[String], <<[Double], <<?[String], <<[String], <<[Double], <<[String], <<[Int], <<?[String], <<?[Int], <<[String], <<[Double]))
  }
  /** Table description of table proteindeal. Objects of this class serve as prototypes for rows in queries. */
  class Proteindeal(_tableTag: Tag) extends profile.api.Table[ProteindealRow](_tableTag, Some("p3"), "proteindeal") {
    def * = (projectname, delete, iqr, replace, replacemethod, rate, assignvalue, kvalue, normal, normalmethod, colname, coefficient) <> (ProteindealRow.tupled, ProteindealRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(projectname), delete, Rep.Some(iqr), replace, Rep.Some(replacemethod), Rep.Some(rate), Rep.Some(assignvalue), Rep.Some(kvalue), normal, normalmethod, Rep.Some(colname), Rep.Some(coefficient)).shaped.<>({r=>import r._; _1.map(_=> ProteindealRow.tupled((_1.get, _2, _3.get, _4, _5.get, _6.get, _7.get, _8.get, _9, _10, _11.get, _12.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column projectName SqlType(VARCHAR), PrimaryKey, Length(255,true) */
    val projectname: Rep[String] = column[String]("projectName", O.PrimaryKey, O.Length(255,varying=true))
    /** Database column delete SqlType(VARCHAR), Length(255,true), Default(None) */
    val delete: Rep[Option[String]] = column[Option[String]]("delete", O.Length(255,varying=true), O.Default(None))
    /** Database column iqr SqlType(DOUBLE) */
    val iqr: Rep[Double] = column[Double]("iqr")
    /** Database column replace SqlType(VARCHAR), Length(255,true), Default(None) */
    val replace: Rep[Option[String]] = column[Option[String]]("replace", O.Length(255,varying=true), O.Default(None))
    /** Database column replaceMethod SqlType(VARCHAR), Length(255,true) */
    val replacemethod: Rep[String] = column[String]("replaceMethod", O.Length(255,varying=true))
    /** Database column rate SqlType(DOUBLE) */
    val rate: Rep[Double] = column[Double]("rate")
    /** Database column assignValue SqlType(VARCHAR), Length(255,true) */
    val assignvalue: Rep[String] = column[String]("assignValue", O.Length(255,varying=true))
    /** Database column kValue SqlType(INT) */
    val kvalue: Rep[Int] = column[Int]("kValue")
    /** Database column normal SqlType(VARCHAR), Length(255,true), Default(None) */
    val normal: Rep[Option[String]] = column[Option[String]]("normal", O.Length(255,varying=true), O.Default(None))
    /** Database column normalMethod SqlType(INT), Default(None) */
    val normalmethod: Rep[Option[Int]] = column[Option[Int]]("normalMethod", O.Default(None))
    /** Database column colName SqlType(VARCHAR), Length(255,true) */
    val colname: Rep[String] = column[String]("colName", O.Length(255,varying=true))
    /** Database column coefficient SqlType(DOUBLE) */
    val coefficient: Rep[Double] = column[Double]("coefficient")
  }
  /** Collection-like TableQuery object for table Proteindeal */
  lazy val Proteindeal = new TableQuery(tag => new Proteindeal(tag))

  /** Entity class storing rows of table Sample
   *  @param samplename Database column sampleName SqlType(VARCHAR), PrimaryKey, Length(255,true)
   *  @param length Database column length SqlType(INT)
   *  @param definition Database column definition SqlType(VARCHAR), Length(255,true)
   *  @param organism Database column organism SqlType(VARCHAR), Length(255,true)
   *  @param taxonomy Database column taxonomy SqlType(VARCHAR), Length(255,true) */
  final case class SampleRow(samplename: String, length: Int, definition: String, organism: String, taxonomy: String)
  /** GetResult implicit for fetching SampleRow objects using plain SQL queries */
  implicit def GetResultSampleRow(implicit e0: GR[String], e1: GR[Int]): GR[SampleRow] = GR{
    prs => import prs._
    SampleRow.tupled((<<[String], <<[Int], <<[String], <<[String], <<[String]))
  }
  /** Table description of table sample. Objects of this class serve as prototypes for rows in queries. */
  class Sample(_tableTag: Tag) extends profile.api.Table[SampleRow](_tableTag, Some("p3"), "sample") {
    def * = (samplename, length, definition, organism, taxonomy) <> (SampleRow.tupled, SampleRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(samplename), Rep.Some(length), Rep.Some(definition), Rep.Some(organism), Rep.Some(taxonomy)).shaped.<>({r=>import r._; _1.map(_=> SampleRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column sampleName SqlType(VARCHAR), PrimaryKey, Length(255,true) */
    val samplename: Rep[String] = column[String]("sampleName", O.PrimaryKey, O.Length(255,varying=true))
    /** Database column length SqlType(INT) */
    val length: Rep[Int] = column[Int]("length")
    /** Database column definition SqlType(VARCHAR), Length(255,true) */
    val definition: Rep[String] = column[String]("definition", O.Length(255,varying=true))
    /** Database column organism SqlType(VARCHAR), Length(255,true) */
    val organism: Rep[String] = column[String]("organism", O.Length(255,varying=true))
    /** Database column taxonomy SqlType(VARCHAR), Length(255,true) */
    val taxonomy: Rep[String] = column[String]("taxonomy", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table Sample */
  lazy val Sample = new TableQuery(tag => new Sample(tag))

  /** Entity class storing rows of table User
   *  @param id Database column id SqlType(INT), AutoInc, PrimaryKey
   *  @param account Database column account SqlType(VARCHAR), Length(255,true)
   *  @param password Database column password SqlType(VARCHAR), Length(255,true)
   *  @param registertime Database column registerTime SqlType(DATETIME) */
  final case class UserRow(id: Int, account: String, password: String, registertime: DateTime)
  /** GetResult implicit for fetching UserRow objects using plain SQL queries */
  implicit def GetResultUserRow(implicit e0: GR[Int], e1: GR[String], e2: GR[DateTime]): GR[UserRow] = GR{
    prs => import prs._
    UserRow.tupled((<<[Int], <<[String], <<[String], <<[DateTime]))
  }
  /** Table description of table user. Objects of this class serve as prototypes for rows in queries. */
  class User(_tableTag: Tag) extends profile.api.Table[UserRow](_tableTag, Some("p3"), "user") {
    def * = (id, account, password, registertime) <> (UserRow.tupled, UserRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(account), Rep.Some(password), Rep.Some(registertime)).shaped.<>({r=>import r._; _1.map(_=> UserRow.tupled((_1.get, _2.get, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(INT), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column account SqlType(VARCHAR), Length(255,true) */
    val account: Rep[String] = column[String]("account", O.Length(255,varying=true))
    /** Database column password SqlType(VARCHAR), Length(255,true) */
    val password: Rep[String] = column[String]("password", O.Length(255,varying=true))
    /** Database column registerTime SqlType(DATETIME) */
    val registertime: Rep[DateTime] = column[DateTime]("registerTime")
  }
  /** Collection-like TableQuery object for table User */
  lazy val User = new TableQuery(tag => new User(tag))

  /** Entity class storing rows of table VirusGeneInfo
   *  @param samplename Database column sampleName SqlType(VARCHAR), Length(255,true)
   *  @param proteinid Database column proteinId SqlType(VARCHAR), Length(255,true)
   *  @param locusname Database column locusName SqlType(VARCHAR), Length(255,true)
   *  @param genename Database column geneName SqlType(VARCHAR), Length(255,true)
   *  @param seqname Database column seqName SqlType(VARCHAR), Length(255,true)
   *  @param start Database column start SqlType(INT)
   *  @param end Database column end SqlType(INT)
   *  @param strand Database column strand SqlType(VARCHAR), Length(255,true)
   *  @param product Database column product SqlType(LONGTEXT), Length(2147483647,true)
   *  @param gene Database column gene SqlType(LONGTEXT), Length(2147483647,true)
   *  @param pep Database column pep SqlType(LONGTEXT), Length(2147483647,true) */
  final case class VirusGeneInfoRow(samplename: String, proteinid: String, locusname: String, genename: String, seqname: String, start: Int, end: Int, strand: String, product: String, gene: String, pep: String)
  /** GetResult implicit for fetching VirusGeneInfoRow objects using plain SQL queries */
  implicit def GetResultVirusGeneInfoRow(implicit e0: GR[String], e1: GR[Int]): GR[VirusGeneInfoRow] = GR{
    prs => import prs._
    VirusGeneInfoRow.tupled((<<[String], <<[String], <<[String], <<[String], <<[String], <<[Int], <<[Int], <<[String], <<[String], <<[String], <<[String]))
  }
  /** Table description of table virus_gene_info. Objects of this class serve as prototypes for rows in queries. */
  class VirusGeneInfo(_tableTag: Tag) extends profile.api.Table[VirusGeneInfoRow](_tableTag, Some("p3"), "virus_gene_info") {
    def * = (samplename, proteinid, locusname, genename, seqname, start, end, strand, product, gene, pep) <> (VirusGeneInfoRow.tupled, VirusGeneInfoRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(samplename), Rep.Some(proteinid), Rep.Some(locusname), Rep.Some(genename), Rep.Some(seqname), Rep.Some(start), Rep.Some(end), Rep.Some(strand), Rep.Some(product), Rep.Some(gene), Rep.Some(pep)).shaped.<>({r=>import r._; _1.map(_=> VirusGeneInfoRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8.get, _9.get, _10.get, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column sampleName SqlType(VARCHAR), Length(255,true) */
    val samplename: Rep[String] = column[String]("sampleName", O.Length(255,varying=true))
    /** Database column proteinId SqlType(VARCHAR), Length(255,true) */
    val proteinid: Rep[String] = column[String]("proteinId", O.Length(255,varying=true))
    /** Database column locusName SqlType(VARCHAR), Length(255,true) */
    val locusname: Rep[String] = column[String]("locusName", O.Length(255,varying=true))
    /** Database column geneName SqlType(VARCHAR), Length(255,true) */
    val genename: Rep[String] = column[String]("geneName", O.Length(255,varying=true))
    /** Database column seqName SqlType(VARCHAR), Length(255,true) */
    val seqname: Rep[String] = column[String]("seqName", O.Length(255,varying=true))
    /** Database column start SqlType(INT) */
    val start: Rep[Int] = column[Int]("start")
    /** Database column end SqlType(INT) */
    val end: Rep[Int] = column[Int]("end")
    /** Database column strand SqlType(VARCHAR), Length(255,true) */
    val strand: Rep[String] = column[String]("strand", O.Length(255,varying=true))
    /** Database column product SqlType(LONGTEXT), Length(2147483647,true) */
    val product: Rep[String] = column[String]("product", O.Length(2147483647,varying=true))
    /** Database column gene SqlType(LONGTEXT), Length(2147483647,true) */
    val gene: Rep[String] = column[String]("gene", O.Length(2147483647,varying=true))
    /** Database column pep SqlType(LONGTEXT), Length(2147483647,true) */
    val pep: Rep[String] = column[String]("pep", O.Length(2147483647,varying=true))

    /** Primary key of VirusGeneInfo (database name virus_gene_info_PK) */
    val pk = primaryKey("virus_gene_info_PK", (samplename, proteinid))
  }
  /** Collection-like TableQuery object for table VirusGeneInfo */
  lazy val VirusGeneInfo = new TableQuery(tag => new VirusGeneInfo(tag))

  /** Entity class storing rows of table VirusSample
   *  @param samplename Database column sampleName SqlType(VARCHAR), PrimaryKey, Length(255,true)
   *  @param length Database column length SqlType(INT)
   *  @param definition Database column definition SqlType(VARCHAR), Length(255,true)
   *  @param organism Database column organism SqlType(VARCHAR), Length(255,true)
   *  @param taxonomy Database column taxonomy SqlType(VARCHAR), Length(255,true) */
  final case class VirusSampleRow(samplename: String, length: Int, definition: String, organism: String, taxonomy: String)
  /** GetResult implicit for fetching VirusSampleRow objects using plain SQL queries */
  implicit def GetResultVirusSampleRow(implicit e0: GR[String], e1: GR[Int]): GR[VirusSampleRow] = GR{
    prs => import prs._
    VirusSampleRow.tupled((<<[String], <<[Int], <<[String], <<[String], <<[String]))
  }
  /** Table description of table virus_sample. Objects of this class serve as prototypes for rows in queries. */
  class VirusSample(_tableTag: Tag) extends profile.api.Table[VirusSampleRow](_tableTag, Some("p3"), "virus_sample") {
    def * = (samplename, length, definition, organism, taxonomy) <> (VirusSampleRow.tupled, VirusSampleRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(samplename), Rep.Some(length), Rep.Some(definition), Rep.Some(organism), Rep.Some(taxonomy)).shaped.<>({r=>import r._; _1.map(_=> VirusSampleRow.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column sampleName SqlType(VARCHAR), PrimaryKey, Length(255,true) */
    val samplename: Rep[String] = column[String]("sampleName", O.PrimaryKey, O.Length(255,varying=true))
    /** Database column length SqlType(INT) */
    val length: Rep[Int] = column[Int]("length")
    /** Database column definition SqlType(VARCHAR), Length(255,true) */
    val definition: Rep[String] = column[String]("definition", O.Length(255,varying=true))
    /** Database column organism SqlType(VARCHAR), Length(255,true) */
    val organism: Rep[String] = column[String]("organism", O.Length(255,varying=true))
    /** Database column taxonomy SqlType(VARCHAR), Length(255,true) */
    val taxonomy: Rep[String] = column[String]("taxonomy", O.Length(255,varying=true))
  }
  /** Collection-like TableQuery object for table VirusSample */
  lazy val VirusSample = new TableQuery(tag => new VirusSample(tag))
}
