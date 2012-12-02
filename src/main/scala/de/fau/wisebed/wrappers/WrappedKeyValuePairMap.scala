package de.fau.wisebed.wrappers

import scala.collection._
import eu.wisebed.api.common.KeyValuePair
import scala.collection.JavaConversions._

class WrappedKeyValuePairMap(_ls:java.util.List[KeyValuePair]) extends mutable.Map[String, String] {
    def ls = asScalaBuffer(_ls)
	
	def get(key: String): Option[String] = ls.find(_.getKey == key) map (_.getValue)
	def iterator: Iterator[(String, String)] = ls.map(x => {x.getKey -> x.getValue}).iterator
	
	def += (kv: (String, String)) = { 
		remove(kv._1)
		val kvp = new KeyValuePair 
		kvp.setKey( kv._1)
		kvp.setValue(kv._2)
		ls.append(kvp)
		WrappedKeyValuePairMap.this 
	}
	
	def -= (key: String) = { remove(key); WrappedKeyValuePairMap.this }

	private def remkey(key:String)  {
		ls.filter(_.getKey == key).foreach(ls -= _)
	}
}
