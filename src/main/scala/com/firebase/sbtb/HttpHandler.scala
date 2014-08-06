package com.firebase.sbtb

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil
import io.netty.buffer.ByteBufUtil
import java.nio.CharBuffer
import io.netty.channel.ChannelHandler.Sharable
import akka.actor.ActorRef
import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.pattern.ask

@Sharable
object HttpHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  private implicit val timeout = ApplicationCache.timeout
  val SOMA_METHODS = Set(HttpMethod.POST, HttpMethod.GET)

  private def runApp(app: ActorRef, ctx: ChannelHandlerContext, msg: FullHttpRequest)
                    (implicit executor: NettyExecutionContext): Future[HttpResponse] = {
    val method = msg.getMethod
    // In a more sophisticated application, you'll likely want to do some better http route and error handling.
    if (SOMA_METHODS contains method) {
      if (msg.getUri endsWith "marketingStats") {
        (app ? GetStats).mapTo[BooleanSyncAppStats] map {
          case stats => getStringResponse(s"reads: ${stats.reads}\nwrites: ${stats.writes}", ctx)
        }
      } else {
        val future = if (method == HttpMethod.POST) {
          val newState = msg.content().toString(CharsetUtil.UTF_8).toBoolean
          app ? SetState(newState)
        } else {
          app ? GetState
        }
        future.mapTo[Boolean] map { getBooleanResponse(_, ctx) }
      }
    } else {
      Future.successful { get405Response }
    }
  }

  def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
    implicit val executor = new NettyExecutionContext(ctx)
    val responseFuture = msg.getUri.split("/").toList match {
      case "" :: appName :: tail => {
        ApplicationCache.getOrLookup(appName) match {
          case Some(app) => runApp(app, ctx, msg)
          case None => Future.successful { get404Response }
        }
      }
      case _ => Future.successful { get404Response }
    }
    responseFuture onComplete {
      case Success(response) => ctx.writeAndFlush(response)
      case Failure(throwable) => ctx.writeAndFlush(get500Response(throwable, ctx))
    }

  }

  def get500Response(throwable: Throwable, ctx: ChannelHandlerContext): HttpResponse = {
    val buf = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap("INTERNAL SERVER ERROR"), CharsetUtil.UTF_8)
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR, buf)
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes())
    response
  }

  def getBooleanResponse(bool: Boolean, ctx: ChannelHandlerContext): HttpResponse = {
    getStringResponse(bool.toString, ctx)
  }

  def getStringResponse(str: String, ctx: ChannelHandlerContext): HttpResponse = {
    val buf = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(str), CharsetUtil.UTF_8)
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf)
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, buf.readableBytes())
    response
  }

  def get404Response: HttpResponse = {
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND)
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0)
    response
  }

  def get405Response: HttpResponse = {
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST)
    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0)
    response
  }

}
