package com.firebase.sbtb

sealed trait BooleanSyncAppMessage
case object GetState extends BooleanSyncAppMessage
case class SetState(newState: Boolean) extends BooleanSyncAppMessage
case object GetStats extends BooleanSyncAppMessage
case class AddListener(listener: BooleanStateReceiver) extends BooleanSyncAppMessage
case class RemoveListener(listener: BooleanStateReceiver) extends BooleanSyncAppMessage
