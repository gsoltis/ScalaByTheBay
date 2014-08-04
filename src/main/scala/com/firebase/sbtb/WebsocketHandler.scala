package com.firebase.sbtb

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.websocketx._
import io.netty.handler.codec.http.{HttpHeaders, FullHttpRequest}

object WebsocketHandler {

  def handshake(msg: FullHttpRequest, ctx: ChannelHandlerContext): WebSocketServerHandshaker = {
    val host = msg.headers().get(HttpHeaders.Names.HOST)
    val location = s"ws://$host/.ws"
    val subProtocols = null
    val allowExtensions = false
    val factory = new WebSocketServerHandshakerFactory(location, subProtocols, allowExtensions)
    val handshaker = factory.newHandshaker(msg)
    handshaker.handshake(ctx.channel(), msg)
    // We're done with this message now, so we can release it
    msg.release()
    handshaker
  }
}

class WebsocketHandler(handshaker: WebSocketServerHandshaker) extends SimpleChannelInboundHandler[WebSocketFrame]
    with FoggyStateReceiver {

  var cachedContext: Option[ChannelHandlerContext] = None

  def channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
    msg match {
      case textFrame: TextWebSocketFrame => handleTextCommand(textFrame.text(), ctx)
      case closeFrame: CloseWebSocketFrame => handshaker.close(ctx.channel(), closeFrame.retain())
    }
  }

  private def handleTextCommand(command: String, ctx: ChannelHandlerContext) {
    if (command startsWith "setFoggy:") {
      val newFoggyState = command.substring("setFoggy:".length).toBoolean
      SocialLocalWeatherApp.setFoggy(newFoggyState)
    } else {
      sendText("invalid command", ctx)
    }
  }

  private def sendText(text: String, ctx: ChannelHandlerContext) {
    // Only send if the channel is still active, otherwise it's wasted effort
    if (ctx.channel().isActive) {
      val frame = new TextWebSocketFrame(text)
      ctx.writeAndFlush(frame)
    }
  }

  def sendIsFoggy(isFoggy: Boolean) {
    cachedContext foreach { sendText(s"foggy:$isFoggy", _) }
  }

  override def handlerAdded(ctx: ChannelHandlerContext) {
    cachedContext = Some(ctx)
    SocialLocalWeatherApp.registerBroadcastReceiver(this)
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    SocialLocalWeatherApp.deregisterBroadcastReceiver(this)
  }
}
