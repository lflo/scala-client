package de.fau.wiseml.wrappers

import eu.wisebed.wiseml.WiseMLHelper
import scala.collection.mutable.Buffer
import scala.collection.JavaConversions._


object RichWiseMLHelper{
  implicit 	def getNodeUrns(serializedWiseML:String, types:Iterable[String] = null):List[String] ={
	  val iter:java.lang.Iterable[String] = asJavaIterable(types)
	  val jlist:java.util.List[java.lang.String] = WiseMLHelper.getNodeUrns(serializedWiseML, iter)
	  val sBuf = scala.collection.JavaConversions.asBuffer(jlist)
	  sBuf.toList
	} 
}
