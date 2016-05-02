package typify

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.typify.parsedinstances._
import scalaz.std.option._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.syntax.traverse._
import scalaz.syntax.validation._
import scalaz.ValidationNel
import shapeless.LabelledGeneric
import shapeless.tag
import shapeless.tag.@@

@JSExport
object jsDynamicExample {

  trait Email {}
  trait Age {}
  trait SessId {}

  sealed trait Gender
  case object Male extends Gender
  case object Female extends Gender

  case class Person(email: String @@ Email, age: Int @@ Age, gender: Gender, session: Option[Int @@ SessId])
  case class UnsafePerson(email: String, age: Int)

  val typify = new Typify[String, js.Dynamic]
  import typify.parsers._

  implicit lazy val e2s = (pd: Parsed[js.Dynamic], p: ParseError) => s"${p.key}: ${p.error}"

  implicit lazy val vEmail = typify.validate[String, String @@ Email]((e: String) =>
    e.contains("@").option(tag[Email](e)).toSuccessNel("invalid email"))
  implicit lazy val vGender = typify.validate[String, Gender]((e: String) => e match {
    case "m" => Male.successNel[String]
    case "f" => Female.successNel[String]
    case x => s"Invalid gender $x".failureNel[Gender]
  })
  implicit lazy val vAge = typify.validate[Int, Int @@ Age]((k: String, a: Int) =>
    (a > 18).option(tag[Age](a)).toSuccessNel(s"${k} too young"))
  implicit lazy val sid = typify.validate[Option[Int], Option[Int @@ SessId]]((i: Option[Int]) =>
    i match {
      case Some(id) => (id > 10000).option(Some(tag[SessId](id))).toSuccessNel(s"invalid session $id")
      case None => None.successNel[String]
    })

  implicit lazy val oPerson = typify.validate[Option[js.Dynamic], Option[Person]](
    (k: String, jsd: Option[js.Dynamic], p: Parsed[js.Dynamic]) =>
      jsd.map(op => typify[Person](op)).sequenceU)

  @JSExport
  def optionalPerson(jsd: String): ValidationNel[String, Option[Person]] =
    typify[Option[Person]](js.JSON.parse(jsd))

  @JSExport
  def validatePerson(jsd: String): ValidationNel[String, Person] = {
    typify[Person](js.JSON.parse(jsd))
  }

  case class Optional[A](a: Option[A])

  @JSExport
  def opPerson(jsd: String): ValidationNel[String, Optional[Person]] =
    typify[Optional[Person]](js.JSON.parse(jsd), Seq("b"))

  @JSExport
  def partialValidatePerson(jsd: String, root: Seq[String] = Seq()):
  ValidationNel[String, (String, Int) => Person] =
    typify[(String @@ Email, Int @@ Age) => Person](js.JSON.parse(jsd), root)
      .map(fn => (s: String, i: Int) => fn(tag[Email](s), tag[Age](i)))
}

