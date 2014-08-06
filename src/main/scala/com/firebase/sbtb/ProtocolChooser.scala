package com.firebase.sbtb

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.channel.ChannelHandler.Sharable

@Sharable
object ProtocolChooser extends SimpleChannelInboundHandler[FullHttpRequest] {

  def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
    // We're going to be firing this message upstream, so make sure we hang on to the content
    msg.retain()
    if (msg.getUri endsWith ".ws") {
      // It's a websocket connection. Do the handshake and add our websocket handler
      val wsHandler = WebsocketHandler.handshake(msg, ctx)
      ctx.pipeline().addLast("websocket-app", wsHandler)
    } else if (msg.getUri endsWith ".lp") {
      // It's a long poll connection
      // Add the handler for long poll requests
      ctx.pipeline().addLast("long-poll-app", new LongPollHandler)
      // In this case, we can't remove this handler, we may get other http requests
      // on the same channel
      ctx.fireChannelRead(msg)
    } else {
      // It's a regular http request. Add our http handling code
      ctx.pipeline().addLast("http-app", HttpHandler)
      // In this case, we can't remove this handler, we may get other http requests
      // on the same channel
      ctx.fireChannelRead(msg)
    }
  }
}
