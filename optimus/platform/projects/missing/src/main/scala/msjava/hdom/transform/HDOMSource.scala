package msjava.hdom.transform

import javax.xml.transform.Source
import msjava.hdom.Document
import org.xml.sax.{InputSource, XMLReader}

/** Compile-time stand-in for msjava.hdom.transform.HDOMSource; see the module README. */
class HDOMSource(doc: Document) extends Source {
  override def setSystemId(systemId: String): Unit = ???
  override def getSystemId: String = ???
  def getXMLReader: XMLReader = ???
  def getInputSource: InputSource = ???
}
