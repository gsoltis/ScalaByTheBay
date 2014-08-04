package com.firebase.sbtb

import scala.collection.mutable

case class MarketingStats(reads: Int, writes: Int)

trait FoggyStateReceiver {
  def sendIsFoggy(isFoggy: Boolean)
}

object SocialLocalWeatherApp {

  var isFoggy = false
  var readCount = 0
  var writeCount = 0

  private val receivers = new mutable.HashSet[FoggyStateReceiver] with mutable.SynchronizedSet[FoggyStateReceiver]

  def setFoggy(foggy: Boolean): Boolean = {
    synchronized {
      val oldFoggyState = isFoggy
      isFoggy = foggy
      logRead()
      logWrite()
      if (foggy != oldFoggyState) {
        // Only broadcast on changes
        val broadcastCount = (receivers map { case receiver: FoggyStateReceiver =>
          receiver.sendIsFoggy(isFoggy)
          1
        }).sum
        // Log the number of sends we did. Do it as we send so we avoid accounting errors with receivers being added
        // or removed at the same time.
        readCount += broadcastCount
      }
      isFoggy
    }
  }

  def getFoggy: Boolean = {
    synchronized {
      logRead()
      isFoggy
    }
  }

  def getMarketingStats: MarketingStats = {
    synchronized {
      MarketingStats(readCount, writeCount)
    }
  }

  def registerBroadcastReceiver(receiver: FoggyStateReceiver) {
    receivers += receiver
    receiver.sendIsFoggy(getFoggy)
  }

  def deregisterBroadcastReceiver(receiver: FoggyStateReceiver) {
    receivers -= receiver
  }

  private def logWrite() {
    writeCount += 1
  }

  private def logRead() {
    readCount += 1
  }

}
