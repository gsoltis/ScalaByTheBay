package com.firebase.sbtb

import io.netty.channel.ChannelHandlerContext
import scala.concurrent.ExecutionContext

class NettyExecutionContext(ctx: ChannelHandlerContext) extends ExecutionContext {

  def execute(runnable: Runnable) = ctx.executor().execute(runnable)

  def reportFailure(t: Throwable) = ctx.fireExceptionCaught(t)
}
