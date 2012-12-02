package de.fau.wisebed.wrappers

import eu.wisebed.api.wsn.ChannelHandlerDescription
import scala.collection.JavaConversions._
import org.slf4j.LoggerFactory
import de.fau.wisebed.WisebedApiConversions._

class WrappedChannelHandlerDescription(chd:ChannelHandlerDescription) {
	val log = LoggerFactory.getLogger(WrappedChannelHandlerDescription.this.getClass)
	def name:String = chd.getName
	def description:String = chd.getDescription
	def configuration:Map[String,String] = chd.getConfigurationOptions.toList
	
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

object WrappedChannelHandlerDescription {
	implicit def chd2wchd(chd:ChannelHandlerDescription):WrappedChannelHandlerDescription = new WrappedChannelHandlerDescription(chd)
	implicit def wchd2chd(rchd:WrappedChannelHandlerDescription):ChannelHandlerDescription = rchd.channelHandlerDescription
}
