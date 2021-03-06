package org.duffqiu.rest.common

import scala.collection.immutable.Traversable

import dispatch.{ :/ => :/ }
import dispatch.Http
import dispatch.Req
import dispatch.url

object RestClient {
  private[common] final val DEFAULT_NAME = "RestTestClient"
  private[common] final val DEFAULT_PORT = 8080
}

class RestClient(val name: String = RestClient.DEFAULT_NAME, val hostName: RestHost = LOCAL_HOST, val port: Int = RestClient.DEFAULT_PORT,
                 client: Http = Http()) {
  val host = :/(hostName.name, port)

  def ->(hostName: RestHost) = {
    new RestClient(name, hostName, port, client)
  }

  def buildHttpRequest(resource: RestResource, operation: RestOperation, request: RestRequest) = {

    val fullPath = request.pathPara().foldLeft(resource.path) {
      (input: String, t: (String, String)) => input.replaceAllLiterally(t._1, t._2)
    }

    val reqWithUrl = url(host.url + fullPath)

    val reqWithMethod = resource.style match {
      case REST_STYLE => reqWithUrl.setMethod(operation())
      case _ => reqWithUrl.setMethod("POST")
    }

    val reqWithHeader = request.headerPara().foldLeft(reqWithMethod) {
      (req: Req, t: (String, String)) => req <:< Traversable((t._1, t._2))
    }

    val reqWithBody = request.body match {
      case EmptyBody => reqWithHeader
      case restBody: RestBody => reqWithHeader.setBody(request.bodyJson)
      case x: Any => reqWithHeader.setBody(x.toString)
    }

    val reqWithAll = request.queryPara().foldLeft(reqWithBody) {
      (req: Req, t: (String, String)) => req <<? Traversable((t._1, t._2))
    }

    reqWithAll
  }

  def apply(): Http = client

  def stop: Unit = client.shutdown

}

