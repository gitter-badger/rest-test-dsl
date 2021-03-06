/**
 * the rest client worker actor
 */
package org.duffqiu.rest.test.actor

import scala.actors.Actor
import scala.actors.Exit
import scala.actors.TIMEOUT
import scala.language.postfixOps

import org.duffqiu.rest.common.RestHost
import org.duffqiu.rest.common.RestOperation
import org.duffqiu.rest.common.RestRequest
import org.duffqiu.rest.common.RestResource
import org.duffqiu.rest.common.RestResponse
import org.duffqiu.rest.common.RestServer
import org.duffqiu.rest.test.dsl.RestClientTestDsl.client2ClientHelper
import org.duffqiu.rest.test.dsl.RestClientTestDsl.string2RestClientHelper
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientOperation
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientRequest
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientResource
import org.duffqiu.rest.test.dsl.RestClientTestDsl.withClientResult

/**
 * @author macbook
 *
 * Jun 7, 2014
 */

object RestClientWorkActor {
    private final val DEFAULT_INTERVAL = 5000
}

case class RestClientWorkActor(val name: String, master: RestClientMasterActor, server: RestServer, host: RestHost, serverPort: Int,
                               testFun: (RestServer, RestResource, RestRequest, RestOperation, RestResponse, RestResponse) => Unit,
                               interval: Int = RestClientWorkActor.DEFAULT_INTERVAL) extends Actor {

    val client = name -> host on serverPort
    var isExit = false

    override def act(): Unit = {
        trapExit = true
        link(master)

        loopWhile(!isExit) {

            receiveWithin(interval) {
                case RestTestTaskMessage(resource, req, operation, resp, expectResult) => {

                    try {
                        client ask_for resource to operation by req should expectResult and_with {
                            resultResp: RestResponse =>
                                testFun(server, resource, req, operation, resp, resultResp)
                        }
                    } catch {

                        case e: Exception =>
                            master ! RestClientExceptionMessage(name, e)

                        case t: Throwable =>
                            println("[" + name + "]got unknow exception: " + t)
                    }

                }
                case CLIENT_BYE =>
                    client.stop
                    isExit = true
                case Exit(linked, reason) =>
                    client.stop
                    isExit = true;
                case TIMEOUT =>

                case _ =>
                    println("[" + name + "]receive unknown message in client worker")
            }
        }
    }
}
