package com.firebase.sbtb

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import scala.util.{Failure, Success, Try}
import java.util.concurrent.{Future => JavaFuture}
import java.net.InetAddress
import io.netty.channel.socket.nio.NioServerSocketChannel

object App {

  def main(args: Array[String]) {
    setupNetty()
  }

  private def setupNetty() {

    val bossThreadGroupSize = 1
    val bossThreadGroup = new NioEventLoopGroup(bossThreadGroupSize)

    val workerThreadGroupSize = 16
    val workerThreadGroup = new NioEventLoopGroup(workerThreadGroupSize)
    val server = new ServerBootstrap
    server.group(bossThreadGroup, workerThreadGroup)
    server.channel(classOf[NioServerSocketChannel])
    // This object will initialize the pipeline for each new channel
    server.childHandler(ServerChannelInitializer)

    val t = Try {
      val localhost = InetAddress.getLocalHost
      val port = 9002
      val serverChannelFuture = server.bind(localhost, port).sync()
      println("Running!")
      val serverChannel = serverChannelFuture.channel()
      // Blocks until this channel is closed. Since we aren't closing it, it runs forever
      serverChannel.closeFuture().sync()
    }

    t match {
      case Success(_) => println("Listen socket closed")
      case Failure(throwable) => throwable.printStackTrace()
    }

    val javaFuture: JavaFuture[_] = bossThreadGroup.shutdownGracefully()
    javaFuture.get()
  }
}
