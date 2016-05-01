/**
  * Copyright 2016 Rad Gruchalski (radek@gruchalski.com)
  * Licensed under the Apache License, Version 2.0.
  */
package com.appministry.scathon.client.callbackServer

import java.net.ServerSocket

import com.appministry.scathon.models.v2.{ApiPostEventParser, EventParser, EventTypes}
import com.codahale.metrics.MetricRegistry
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.{Http, ListeningServer, Service}
import com.twitter.logging.Logger
import com.twitter.util.{Future, Time}
import play.api.libs.json.Json

class CallbackServer extends EventParser
  with ApiPostEventParser {

  val callbackUri = "/test/callback"

  private var _server: Option[ListeningServer] = None
  private var _port: Option[Int] = None
  private val registry = new MetricRegistry

  private val apiRequests = registry.counter("apiRequests")
  def apiRequestCount = apiRequests.getCount

  private def getFreePort(): Int = {
    val ss = new ServerSocket(0)
    val port = ss.getLocalPort
    ss.close()
    port
  }

  def port: Option[Int] = {
    _port
  }

  def start: Unit = {
    _server match {
      case Some(server) => Logger.get().error("Server already started.")
      case None =>
        _port = Some(getFreePort())
        val service = new Service[Request, Response] {
          def apply(request: Request): Future[Response] = Future.value(
            request.method match {
              case Method.Post =>
                request.path match {
                  case `callbackUri` =>
                    eventFormat.reads(Json.parse(request.contentString)).asOpt match {
                      case Some(event) =>
                        event.eventType match {
                          case EventTypes.api_post_event =>
                            apiPostEventFormat.reads(Json.parse(request.contentString)).asOpt match {
                              case Some(apiPostEvent) =>
                                apiRequests.inc()
                                Response(Status.Ok)
                              case None => Response(Status.BadRequest)
                            }
                          case _ => Response(Status.Ok)
                        }
                      case None => Response(Status.BadRequest)
                    }
                  case _ => Response(Status.NotFound)
                }
              case _ => Response(Status.MethodNotAllowed)
            }
          )
        }
        _server = Some(Http.serve(s":${_port.get}", service))
    }
  }

  def stop: Unit = {
    _server.map { s => s.close(Time.Zero) }
  }

}
