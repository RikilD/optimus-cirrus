package msjava.tools.util

object MSProcess {
  def getPID: Long = ProcessHandle.current().pid()
}
