package com.firebase.sbtb

object PDFToWord extends BooleanSyncApp {

  type PDF = Array[Byte]
  type WORD = Array[Byte]

  // Left as an exercise
  def convert(pdf: PDF): WORD = ???
}
