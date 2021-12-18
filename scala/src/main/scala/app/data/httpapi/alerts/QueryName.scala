package app.data.httpapi.alerts

import app.utils.TypeWrapperCompanion
import org.http4s.QueryParamEncoder


/** Query name that the alert is monitoring. */
case class QueryName(name: String) extends AnyVal
object QueryName extends TypeWrapperCompanion with TypeWrapperCompanion.WithFormat {
  override type Wrapper = QueryName
  override type Underlying = String

  override def getValue(a: QueryName) = a.name
  override def underlyingFormat = implicitly

  implicit val queryParamEncoder: QueryParamEncoder[QueryName] =
    implicitly[QueryParamEncoder[String]].contramap(_.name)
}