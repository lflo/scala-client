package de.fau.wisebed.wrappers

import eu.wisebed.api.wsn.ChannelHandlerConfiguration
import de.fau.wisebed.WisebedApiConversions._
import scala.collection.JavaConversions._

class WrappedChannelHandlerConfiguration(chc:ChannelHandlerConfiguration = new ChannelHandlerConfiguration) {
	val wconf = new WrappedKeyValuePairMap(chc.getConfiguration)
	
	def this (name:String)  {
		this()
		chc.setName(name)
	}
	
	def channelHandlerConfiguration = chc
	
	def name:String = chc.getName
	def name_=(n:String) {chc.setName(n)}
	
	def conf(k:String):Option[String] = wconf.get(k)
	def conf_=(k:String, v:String)  = wconf += k -> v
	
	def getConfMap = wconf	
}

object WrappedChannelHandlerConfiguration {
	implicit def chc2wchc(chc:ChannelHandlerConfiguration) = new WrappedChannelHandlerConfiguration(chc)
	implicit def wchc2chc(wchc:WrappedChannelHandlerConfiguration) = wchc.channelHandlerConfiguration
}