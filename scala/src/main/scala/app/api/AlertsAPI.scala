package app.api

import _root_.play.api.libs.json.Json
import app.models.httpapi.alerts._
import cats.effect.Concurrent
import org.http4s._
import org.http4s.client._
import org.http4s.play.PlayEntityCodec._
import org.http4s.play._

object AlertsAPI {
  /** Base uri for the [[AlertsAPI]]. */
  case class BaseUri(uri: Uri)

  /** GET `/alerts` endpoint. */
  def queryAlerts[F[_] : Concurrent](baseUri: BaseUri)(implicit client: Client[F]): F[QueryAlerts] = {
    val request = Request[F](Method.GET, baseUri.uri / "alerts")
    client.expect[QueryAlerts](request)
  }

  /** GET `/query` endpoint. */
  def query[F[_] : Concurrent](
    baseUri: BaseUri, queryName: QueryName
  )(implicit client: Client[F]): F[QueryResponse] = {
    val request = Request[F](Method.GET, (baseUri.uri / "query").withQueryParam("target", queryName))
    client.expect[QueryResponse](request)
  }

  /** POST to `/notify` endpoint. */
  def notify[F[_] : Concurrent](
    baseUri: BaseUri, alertName: AlertName, message: AlertMessage
  )(implicit client: Client[F]): F[BooleanResponse] = {
    val entity = jsonEncoder.toEntity(Json.obj("alertName" -> alertName, "message" -> message))
    val request = Request[F](Method.POST, baseUri.uri / "notify", body = entity.body)
    client.expect[BooleanResponse](request)
  }

  /** POST to `/resolve` endpoint. */
  def resolve[F[_] : Concurrent](
    baseUri: BaseUri, alertName: AlertName
  )(implicit client: Client[F]): F[BooleanResponse] = {
    val entity = jsonEncoder.toEntity(Json.obj("alertName" -> alertName))
    val request = Request[F](Method.POST, baseUri.uri / "resolve", body = entity.body)
    client.expect[BooleanResponse](request)
  }
}
