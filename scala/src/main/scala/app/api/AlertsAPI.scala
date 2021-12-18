package app.api

import app.data.httpapi.alerts._
import cats.effect.Concurrent
import cats.syntax.all._
import org.http4s._
import org.http4s.client._
import org.http4s.play.PlayEntityCodec._

object AlertsAPI {
  /** Base uri for the [[AlertsAPI]]. */
  case class BaseUri(uri: Uri)

  /** GET `/alerts` endpoint. */
  def queryAlerts[F[_] : Concurrent](implicit baseUri: BaseUri, client: Client[F]): F[QueryAlertsResponse] = {
    val request = Request[F](Method.GET, baseUri.uri / "alerts")
    client.expect[QueryAlertsResponse](request)
  }

  /** GET `/query` endpoint. */
  def query[F[_] : Concurrent](
    queryName: QueryName
  )(implicit baseUri: BaseUri, client: Client[F]): F[QueryResponse] = {
    val request = Request[F](Method.GET, (baseUri.uri / "query").withQueryParam("target", queryName))
    client.expect[QueryResponse](request)
  }

  /**
   * POST to `/notify` endpoint.
   *
   * @return [[Client.successful]]
   **/
  def notify[F[_] : Concurrent](
    data: NotifyRequest
  )(implicit baseUri: BaseUri, client: Client[F]): F[Unit] = {
    val entity = playEntityEncoder[F, NotifyRequest].toEntity(data)
    val request = Request[F](Method.POST, baseUri.uri / "notify", body = entity.body)
    okRequest(request)
  }

  /**
   * POST to `/resolve` endpoint.
   *
   * @return [[Client.successful]]
   **/
  def resolve[F[_] : Concurrent](
    data: ResolveRequest
  )(implicit baseUri: BaseUri, client: Client[F]): F[Unit] = {
    val entity = playEntityEncoder[F, ResolveRequest].toEntity(data)
    val request = Request[F](Method.POST, baseUri.uri / "resolve", body = entity.body)
    okRequest(request)
  }

  /** Request that raises an error if the response HTTP status is not successful. */
  private def okRequest[F[_] : Concurrent](request: Request[F])(implicit client: Client[F]): F[Unit] =
    client.status(request).flatMap {
      case status if status.isSuccess => Concurrent[F].unit
      case status => Concurrent[F].raiseError(new Exception(s"request failed: $status"))
    }
}
