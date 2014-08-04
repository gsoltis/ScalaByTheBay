package com.firebase.sbtb

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpRequestDecoder, HttpResponseEncoder}

object ServerChannelInitializer extends ChannelInitializer[SocketChannel] {

  val MAX_HTTP_CHUNK_SIZE = 256 * 1024 * 1024 // Max 256MB http chunk

  def initChannel(ch: SocketChannel) {
    val pipeline = ch.pipeline()

    // Time to configure our pipeline

    // Inbound
    pipeline.addLast("http-decoder", new HttpRequestDecoder())
    pipeline.addLast("http-chunk-aggregator", new HttpObjectAggregator(MAX_HTTP_CHUNK_SIZE))

    // Outbound
    // Note that the websocket handshaker will remove this
    pipeline.addLast("http-encoder", new HttpResponseEncoder)

    // Our handlers
    pipeline.addLast("protocol-chooser", ProtocolChooser)
  }
}
