package app.api

import app.data.httpapi.alerts._
import cats.effect.Concurrent
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

  /** POST to `/notify` endpoint. */
  def notify[F[_] : Concurrent](
    data: NotifyRequest
  )(implicit baseUri: BaseUri, client: Client[F]): F[BooleanResponse] = {
    val entity = playEntityEncoder[F, NotifyRequest].toEntity(data)
    val request = Request[F](Method.POST, baseUri.uri / "notify", body = entity.body)
    client.expect[BooleanResponse](request)
  }

  /** POST to `/resolve` endpoint. */
  def resolve[F[_] : Concurrent](
    data: ResolveRequest
  )(implicit baseUri: BaseUri, client: Client[F]): F[BooleanResponse] = {
    val entity = playEntityEncoder[F, ResolveRequest].toEntity(data)
    val request = Request[F](Method.POST, baseUri.uri / "resolve", body = entity.body)
    client.expect[BooleanResponse](request)
  }

  /** Sends [[notify]] or [[resolve]] respectively. */
  def sendAlertState[F[_] : Concurrent](
    alertName: AlertName, alertState: AlertState
  )(implicit baseUri: BaseUri, client: Client[F]): F[BooleanResponse] = {
    def doNotify(message: AlertMessage) = notify(NotifyRequest(alertName, message))

    alertState match {
      case AlertState.Pass => resolve(ResolveRequest(alertName))
      case AlertState.Warn(message) => doNotify(message)
      case AlertState.Critical(message) => doNotify(message)
    }
  }
}
