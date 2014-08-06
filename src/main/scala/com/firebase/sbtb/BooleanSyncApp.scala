package com.firebase.sbtb

import scala.collection.mutable
import akka.actor.{ActorRef, Actor}

case class BooleanSyncAppStats(reads: Int, writes: Int)

class BooleanSyncApp extends Actor {

  private var currentState: Boolean = false
  private var readCount = 0
  private var writeCount = 0
  private val receivers = new mutable.HashSet[BooleanStateReceiver]

  def receive = {
    case msg: BooleanSyncAppMessage => {
      // We're using a sealed trait, we can prove this is an exhaustive match
      msg match {
        case GetState => sendState(sender())
        case SetState(newState) => {
          setState(newState)
          sendState(sender())
        }
        case GetStats => sendAppStats(sender())
        case AddListener(listener) => registerBroadcastReceiver(listener)
        case RemoveListener(listener) => deregisterBroadcastReceiver(listener)
      }
    }
    case other => handleUnknownMessage(other)
  }

  protected def handleUnknownMessage(msg: Any) {
    // No-op. This could be a hook for customized versions of this app though. Or, they could 'become' a new version of
    // this app.
  }

  private def sendState(recipient: ActorRef) {
    logRead()
    recipient ! currentState
  }

  private def sendState(recipient: BooleanStateReceiver) {
    logRead()
    recipient.sendNewState(currentState)
  }

  private def setState(newState: Boolean) {
    logWrite()
    if (newState != currentState) {
      currentState = newState
      receivers foreach sendState
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
    sendState(receiver)
  }

  def deregisterBroadcastReceiver(receiver: BooleanStateReceiver) {
    receivers -= receiver
  }

  def sendAppStats(recipient: ActorRef) {
    recipient ! BooleanSyncAppStats(readCount, writeCount)
  }
}

trait BooleanStateReceiver {
  def sendNewState(newState: Boolean)
}
