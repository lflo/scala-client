package de.fau.wisebed.test

import org.slf4j.LoggerFactory
import de.fau.wisebed.messages.MessageWaiter
import de.uniluebeck.itm.tr.util.Logging
import org.apache.log4j.Level



object Flash {
	

	
		
	def main(args: Array[String]) {
		val log = LoggerFactory.getLogger(this.getClass)
		
		
		TH.init(30)
		
		
		TH.flash("sky-shell.ihex")
		
		
		val bw = new MessageWaiter(TH.activemotes,  "Contiki>")
		TH.exp.addMessageInput(bw)
		

		
		log.debug("Waiting for bootup")
		if(! bw.waitTimeout(10*1000)){
			log.error("Boot failed");
			sys.exit(1)
		}
		
		
		TH.finish
		
		
		log.debug("DONE")
		sys.exit(0)
	}
}
