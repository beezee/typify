package typify

import scala.reflect.ClassTag
import scalaz.std.option._
import scalaz.std.vector._
import scalaz.syntax.validation._

trait CatchAllInstance extends ValidatedHelper {
  protected def parseMap(x: Any): Option[Map[Any, Any]] =
    Some(x).collect { case m: Map[_, _] => m.asInstanceOf[Map[Any, Any]] }

  implicit def cpt[T](implicit ct: ClassTag[T]) = new CanParse[T, Any] {
    def as(x: Any): Validated[T] =
      Validated(ops => x match {
        case t: T => Op.typedValue(t)
        case _ => Op.typedValueError[T](ops, none[T])
      })

    def parse(k: String, x: Any): Validated[T] =
      for {
        m <- Validated(ops => parseMap(x).fold(Op.typedValueError[Map[Any, Any]](ops, none[Map[Any, Any]]))(Op.typedValue(_)))
        v <- Validated(ops => m.get(k).fold(Op.downFieldError[Any](ops, k))(Op.downField(_, k)))
        t <- as(v)
      } yield t
  }
}

trait CatchOptionInstance extends CatchAllInstance {
  implicit def cpot[T: ClassTag] = new CanParse[Option[T], Any] {
    def as(x: Any): Validated[Option[T]] =
      Validated(_ => Op.typedValue(x match {
        case o@Some(_) => o.collect { case t: T => t }
        case t: T => Option(t)
        case _ => none[T]
      }))

    def parse(k: String, x: Any): Validated[Option[T]] =
      for {
        m <- Validated(_ => Op.typedValue(parseMap(x).getOrElse(Map())))
        vo <- Validated(_ => Op.downField(m.get(k), k))
        t <- vo.fold(Validated(_ => Op.typedValue(none[T])))(as(_))
      } yield t
  }
}

object parsedany extends CatchOptionInstance {
  lazy implicit val cpoany = new CanParse[Option[Any], Any] {
    def as(x: Any): Validated[Option[Any]] =
      Validated(_ => Op.typedValue(x match {
        case o: Option[Any] => o
        case y => Option(y)
      }))

    def parse(k: String, x: Any): Validated[Option[Any]] =
      Validated(ops => ParseError(ops, Op.DownField("foo"), "").failureNel[(Vector[Op], Option[Any])])
      // parseMap(x).flatMap(_.get(k)).fold(Op.typedValue(none[Any]).successNel[ParseError])(y =>
      //   as(y).bimap(_.map(_.copy(key = k)), t => (t._1, t._2.append(NonEmptyList(Op.DownField(k))))))
  }
}