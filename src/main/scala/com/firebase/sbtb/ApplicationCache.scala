package com.firebase.sbtb

import scala.collection.mutable.{Map => MutableMap}

object ApplicationCache {

  // In your application, you probably want this to be some sort of external data store
  private val KNOWN_APPS: Map[String, BooleanSyncApp] = Map(
    "somaIsFoggy" -> new BooleanSyncApp {},
    "NYCUberSurge" -> new BooleanSyncApp {},
    "PDFToWordConverter" -> PDFToWord
  )

  private val cache: MutableMap[String, BooleanSyncApp] = MutableMap()

  def getOrLookup[A <: BooleanSyncApp: Manifest](name: String): Option[A] = {
    synchronized {
      cache.get(name) orElse {
        // In your application, you would do your query to your external store here
        KNOWN_APPS.get(name) map { case app => cache += name -> app; app }
      } flatMap ensureAppType[A]
    }
  }

  private def ensureAppType[A <: BooleanSyncApp: Manifest](app: BooleanSyncApp): Option[A] = {
    app match {
      case correctlyTypedApp: A => Some(correctlyTypedApp)
      case _ => None
    }
  }
}
