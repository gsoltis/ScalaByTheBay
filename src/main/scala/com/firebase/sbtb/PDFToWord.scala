package com.firebase.sbtb

sealed trait PDFToWordMessage

class PDFToWord extends BooleanSyncApp {

  type PDF = Array[Byte]
  type WORD = Array[Byte]

  override protected def handleUnknownMessage(msg: Any) {
    msg match {
      case pdfToWordMessage: PDFToWordMessage => ???
      case other => super.handleUnknownMessage(other)
    }
  }

  // Left as an exercise
  def convert(pdf: PDF): WORD = ???
}
