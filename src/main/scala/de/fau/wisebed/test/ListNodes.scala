package de.fau.wisebed.test

import org.slf4j.LoggerFactory
import de.fau.wisebed.messages.MessageWaiter
import de.uniluebeck.itm.tr.util.Logging
import org.apache.log4j.Level



object ListNodes {
	

	
		
	def main(args: Array[String]) {
		val log = LoggerFactory.getLogger(getClass)
		Logging.setLoggingDefaults(Level.DEBUG) // new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"))
	
		TH.init(2)
		val inact = TH.motes -- TH.activemotes
		log.info("Aktive Nodes  : " +  TH.activemotes.mkString(", "))
		log.info("Inaktive Nodes: " +  inact.mkString(", "))
		
	}
}
