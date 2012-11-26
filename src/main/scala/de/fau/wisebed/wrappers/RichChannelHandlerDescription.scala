package de.fau.wisebed.wrappers

import eu.wisebed.api.wsn.ChannelHandlerDescription
import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory


class RichChannelHandlerDescription(chd:ChannelHandlerDescription) {
	val log = LoggerFactory.getLogger(this.getClass)
	def name:String = chd.getName
	def description:String = chd.getDescription
	def configuration:Map[String,String] = {
		val cnf = chd.getConfigurationOptions
		cnf.map(kvp => (kvp.getKey -> kvp.getValue)).toMap
	} 
	def format:String = {
		val rv = new StringBuilder() 
		rv ++= name + ": " + description + "\n"
		for(c <- configuration) {
			rv ++= "\t" + c._1 + "\t" + c._2  + "\n"
		}
		rv.toString
	}
	
	def channelHandlerDescription = chd
	
	
}


object RichChannelHandlerDescription {
	implicit def chd2rchd(chd:ChannelHandlerDescription):RichChannelHandlerDescription = new RichChannelHandlerDescription(chd)
	implicit def rchd2chd(rchd:RichChannelHandlerDescription):ChannelHandlerDescription = rchd.channelHandlerDescription
}