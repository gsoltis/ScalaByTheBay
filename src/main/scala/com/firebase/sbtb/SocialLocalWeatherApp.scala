package com.firebase.sbtb

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.channel.ChannelHandler.Sharable
import io.netty.util.CharsetUtil
import io.netty.buffer.ByteBufUtil
import java.nio.CharBuffer

@Sharable
object SocialLocalWeatherApp extends SimpleChannelInboundHandler[FullHttpRequest] {

  val SOMA_METHODS = Set(HttpMethod.POST, HttpMethod.GET)

  var isFoggy = false
  var readCount = 0
  var writeCount = 0

  private def somaIsFoggy(ctx: ChannelHandlerContext, msg: FullHttpRequest): HttpResponse = {
    val method = msg.getMethod
    if (SOMA_METHODS contains method) {
      val booleanResult = if (method == HttpMethod.POST) {
        val newFoggyState = msg.content().toString(CharsetUtil.UTF_8).toBoolean
        synchronized {
          readCount += 1
          writeCount += 1
          isFoggy = newFoggyState
          isFoggy
        }
      } else {
        synchronized {
          readCount += 1
          isFoggy
        }
      }
      getBooleanResponse(booleanResult, ctx)
    } else {
      get405Response
    }
  }

  private def marketingStats(ctx: ChannelHandlerContext, msg: HttpRequest): HttpResponse = {
    if (msg.getMethod == HttpMethod.GET) {
      val result = synchronized {
        s"reads: $readCount\nwrites: $writeCount"
      }
      getStringResponse(result, ctx)
    } else {
      get405Response
    }
  }

  def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
    val response = if (msg.getUri == "/somaIsFoggy") {
      somaIsFoggy(ctx, msg)
    } else if (msg.getUri == "/marketingStats") {
      marketingStats(ctx, msg)
    } else {
      get404Response
    }
    ctx.writeAndFlush(response)
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
