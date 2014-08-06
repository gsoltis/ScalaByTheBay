package com.firebase.sbtb

import scala.collection.mutable.{Map => MutableMap}
import akka.actor.{ActorSystem, Props, ActorRef}
import akka.util.Timeout
import java.util.concurrent.TimeUnit

object ApplicationCache {

  // In your application, you probably want this to be some sort of external data store
  private val KNOWN_APPS: Map[String, Props] = Map(
    "somaIsFoggy" -> Props[BooleanSyncApp],
    "NYCUberSurge" -> Props[BooleanSyncApp],
    "PDFToWordConverter" -> Props[PDFToWord]
  )

  private val cache: MutableMap[String, ActorRef] = MutableMap()
  private val system = ActorSystem("TheActorSystem")
  val timeout = Timeout(10, TimeUnit.SECONDS)

  def getOrLookup(name: String): Option[ActorRef] = {
    synchronized {
      cache.get(name) orElse {
        // In your application, you would do your query to your external store here
        KNOWN_APPS.get(name) map {
          case appProps => {
            val actorRef = system.actorOf(appProps)
            cache += name -> actorRef
            actorRef
          }
        }
      }
    }
  }
}
