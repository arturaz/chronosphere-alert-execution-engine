package app.data.httpapi.alerts

import app.utils.TypeWrapperCompanion
import cats.ApplicativeError
import org.http4s.QueryParamEncoder
import play.api.libs.json.Reads


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

/** Success or failure response from the API. */
case class BooleanResponse(success: Boolean) {
  /** Converts this into some [[F]] which is an [[ApplicativeError]]. */
  def asEffect[F[_]](implicit ae: ApplicativeError[F, Throwable]): F[Unit] =
    if (success) ae.pure(())
    else ae.raiseError(new Exception("endpoint failed"))
}
object BooleanResponse {
  implicit val reads: Reads[BooleanResponse] = ???
}