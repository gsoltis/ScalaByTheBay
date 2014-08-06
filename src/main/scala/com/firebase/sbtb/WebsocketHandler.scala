package com.firebase.sbtb

import io.netty.channel._
import io.netty.handler.codec.http.websocketx._
import io.netty.handler.codec.http.{HttpHeaders, FullHttpRequest}
import scala.Some

object WebsocketHandler {

  def handshake(msg: FullHttpRequest, ctx: ChannelHandlerContext): ChannelInboundHandler = {
    val host = msg.headers().get(HttpHeaders.Names.HOST)
    // Skip the leading '/', skip the .ws extension
    val appName =  msg.getUri.substring(1, msg.getUri.length - ".ws".length)
    val location = s"ws://$host/.ws"
    val subProtocols = null
    val allowExtensions = false
    val factory = new WebSocketServerHandshakerFactory(location, subProtocols, allowExtensions)
    val handshaker = factory.newHandshaker(msg)
    val handshakeFuture = handshaker.handshake(ctx.channel(), msg)
    // We're done with this message now, so we can release it
    msg.release()
    ApplicationCache.getOrLookup[BooleanSyncApp](appName) match {
      case Some(app) => new WebsocketHandler(handshaker, app)
      case None => new MissingAppWebsocketHandler(handshaker, handshakeFuture)
    }
  }
}

// Used when the app doesn't exist
class MissingAppWebsocketHandler(handshaker: WebSocketServerHandshaker, handshakeFuture: ChannelFuture)
    extends SimpleChannelInboundHandler[WebSocketFrame] {

  def channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
    msg match {
      case closeFrame: CloseWebSocketFrame => handshaker.close(ctx.channel(), closeFrame.retain())
      case _ => // No-op
    }
  }

  override def handlerAdded(ctx: ChannelHandlerContext) {
    handshakeFuture.addListener(ChannelFutureListener.CLOSE)
  }
}

class WebsocketHandler(handshaker: WebSocketServerHandshaker, app: BooleanSyncApp) extends SimpleChannelInboundHandler[WebSocketFrame]
    with BooleanStateReceiver {

  private var cachedContext: Option[ChannelHandlerContext] = None

  def channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
    msg match {
      case textFrame: TextWebSocketFrame => handleTextCommand(textFrame.text(), ctx)
      case closeFrame: CloseWebSocketFrame => handshaker.close(ctx.channel(), closeFrame.retain())
    }
  }

  private def handleTextCommand(command: String, ctx: ChannelHandlerContext) {
    if (command startsWith "set:") {
      val newState = command.substring("set:".length).toBoolean
      app.setCurrentState(newState)
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

  def sendNewState(newState: Boolean) {
    cachedContext foreach { sendText(s"state:$newState", _) }
  }

  override def handlerAdded(ctx: ChannelHandlerContext) {
    cachedContext = Some(ctx)
    app.registerBroadcastReceiver(this)
  }

  override def channelInactive(ctx: ChannelHandlerContext) {
    app.deregisterBroadcastReceiver(this)
  }
}
