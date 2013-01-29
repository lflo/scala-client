package de.fau.wisebed.test

import org.slf4j.LoggerFactory
import de.fau.wisebed.messages.MessageWaiter
import de.uniluebeck.itm.tr.util.Logging
import org.apache.log4j.Level

object ListNodes {
	val log = LoggerFactory.getLogger(getClass)
		
	def main(args: Array[String]) {
		TH.init(2)

		val inact = TH.motes.filter(!TH.activemotes.contains(_))
		log.info("Aktive Nodes  : " +  TH.activemotes.mkString(", "))
		log.info("Inaktive Nodes: " +  inact.mkString(", "))
		
		TH.finish
	}
}
