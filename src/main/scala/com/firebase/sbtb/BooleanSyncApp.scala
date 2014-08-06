package com.firebase.sbtb

import scala.collection.mutable

case class BooleanSyncAppStats(reads: Int, writes: Int)

trait BooleanSyncApp {

  private var currentState: Boolean = false
  private var readCount = 0
  private var writeCount = 0
  private val receivers = new mutable.HashSet[BooleanStateReceiver] with mutable.SynchronizedSet[BooleanStateReceiver]

  def getCurrentState: Boolean = {
    synchronized {
      logRead()
      currentState
    }
  }

  def setCurrentState(newState: Boolean): Boolean = {
    synchronized {
      logRead()
      logWrite()
      val oldState = currentState
      currentState = newState
      if (currentState != oldState) {
        // Only broadcast on changes
        receivers foreach { case receiver: BooleanStateReceiver =>
          receiver.sendNewState(currentState)
          logRead()
        }
      }
      currentState
    }
  }

  private def logWrite() {
    writeCount += 1
  }

  private def logRead() {
    readCount += 1
  }

  def registerBroadcastReceiver(receiver: BooleanStateReceiver) {
    receivers += receiver
    receiver.sendNewState(getCurrentState)
  }

  def deregisterBroadcastReceiver(receiver: BooleanStateReceiver) {
    receivers -= receiver
  }

  def getAppStats: BooleanSyncAppStats = synchronized { BooleanSyncAppStats(readCount, writeCount) }
}

trait BooleanStateReceiver {
  def sendNewState(newState: Boolean)
}
