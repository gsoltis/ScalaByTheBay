package com.firebase.sbtb

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.FullHttpRequest

class LongPollHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

  def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest) {
    // Left as an exercise to the reader
    ???
  }
}
