package com.firebase.sbtb

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.channel.ChannelHandler.Sharable
import io.netty.util.CharsetUtil
import io.netty.buffer.ByteBufUtil
import java.nio.CharBuffer

@Sharable
object SocialLocalWeatherApp extends SimpleChannelInboundHandler[FullHttpRequest] {

  var isFoggy = false

  def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
    val response = if (msg.getUri == "/somaIsFoggy") {
      val method = msg.getMethod
      if (method == HttpMethod.POST) {
        // Update the state
        isFoggy = msg.content().toString(CharsetUtil.UTF_8).toBoolean
        getBooleanResponse(isFoggy, ctx)
      } else if (method == HttpMethod.GET) {
        getBooleanResponse(isFoggy, ctx)
      } else {
        get405Response
      }
    } else {
      get404Response
    }
    ctx.writeAndFlush(response)
  }

  def getBooleanResponse(bool: Boolean, ctx: ChannelHandlerContext): HttpResponse = {
    val contentString = bool.toString
    val buf = ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(contentString), CharsetUtil.UTF_8)
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
