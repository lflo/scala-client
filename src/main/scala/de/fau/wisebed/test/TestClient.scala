package de.fau.wisebed.test

import java.util.Calendar
import java.util.GregorianCalendar
import scala.collection.JavaConversions.asScalaBuffer
import org.apache.log4j.Level
import org.slf4j.LoggerFactory
import de.uniluebeck.itm.tr.util.Logging
import eu.wisebed.api.common._
import de.fau.wisebed.Reservation.reservation2CRD
import de.fau.wisebed.jobs.MoteAliveState._
import de.fau.wisebed.wrappers._
import de.fau.wisebed.wrappers
import wrappers.WrappedChannelHandlerConfiguration._
import wrappers.WrappedMessage._
import de.fau.wisebed.messages
import messages.MsgLiner
import messages.MessageLogger
import de.fau.wisebed.messages.MessageWaiter
import scala.xml.XML
import java.io.File
import de.fau.wisebed.Experiment
import de.fau.wisebed.Testbed

object TestClient {
	val log = LoggerFactory.getLogger(this.getClass)

	
		
	def main(args: Array[String]) {

		
		TH.init(30)
		TH.flash("sky-shell.ihex")
		
		
		


		val bw = new MessageWaiter(TH.activemotes,  "Contiki>")
		TH.exp.addMessageInput(bw)
		
		
		TH.reset
		
		log.debug("Waiting for bootup")
		if(! bw.waitTimeout(10*1000)){
			log.error("Boot failed");
			sys.exit(1)
		}
		
		log.debug("Sending \\n")
		val snd = TH.exp.send(TH.activemotes, "help\n")
		if(!snd.success){
			log.error("Failed to send information to nodes")
			sys.exit(1)
		}
		log.debug("Waiting for answer")
		Thread.sleep(20 * 1000)
		

		
		
		log.debug("DONE")
		sys.exit(0)
	}
}
