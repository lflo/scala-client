package de.fau.wisebed

import java.util.Calendar
import java.util.GregorianCalendar
import scala.collection.JavaConversions.asScalaBuffer
import org.apache.log4j.Level
import org.slf4j.LoggerFactory
import de.uniluebeck.itm.tr.util.Logging
import eu.wisebed.api.common._
import Reservation.reservation2CRD
import jobs.MoteAliveState._
import wrappers._
import wrappers.WrappedChannelHandlerConfiguration._
import wrappers.WrappedMessage._
import messages.MsgLiner
import messages.MessageLogger
import de.fau.wisebed.messages.MessageWaiter
import scala.xml.XML
import java.io.File

object TestClient {
	val log = LoggerFactory.getLogger(this.getClass)

	
		
	def main(args: Array[String]) {
		Logging.setLoggingDefaults(Level.DEBUG) // new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"))

		val conffile = new File("config.xml")
		if(!conffile.exists){
			log.error("Could not find \"config.xml\"");
			sys.exit(1)
		}
		
		
		val conf = XML.loadFile(conffile)
		val smEndpointURL = (conf \ "smEndpointURL").text.trim
		val snaaEndpointURL = (conf \ "snaaEndpointURL").text.trim
		val rsEndpointURL = (conf \ "rsEndpointURL").text.trim
		
		val prefix = (conf \ "prefix").text.trim
		val login = (conf \ "login").text.trim
		val pass = (conf \ "pass").text.trim
		
		
		//Get Motes
		log.debug("Starting Testbed")
		val tb = new Testbed(smEndpointURL, snaaEndpointURL, rsEndpointURL)
		log.debug("Requesting Motes")
		val motes = tb.getnodes()
		log.debug("Motes: " + motes.mkString(", "))
		
		/* FIXME: This does not work!
		log.debug("Requesting Motesate")
		val statusj = tb.areNodesAlive(motes)
		val status = statusj.status
		status.foreach(m => log.info(m._1 + ": " +  m._2)) 
		*/
		
		log.debug("Logging in")
		tb.addCredencials(prefix, login, pass)
		
		
		log.debug("Requesting reservations")
		var res = tb.getReservations(20)
		
		for(r <- res) {
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.getNodeURNs.mkString(", ")) 
		}
		
		if(!res.exists(_.now)) {
			log.debug("No Reservations or in the Past- Requesting")
			val from = new GregorianCalendar
			val to = new GregorianCalendar
			to.add(Calendar.MINUTE, 120)
			val r = tb.makeReservation(from, to, motes, "morty")
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.getNodeURNs.mkString(", ")) 
			res ::= r
		}
		
		val exp = new Experiment(res.toList, tb)
		
		
		exp.addMessageInput(  new MessageLogger(mi => {
			import wrappers.WrappedMessage._
			log.info("Got message from " + mi.node + ": " + mi.dataString)
		}) with MsgLiner)
		
		log.debug("Requesting Motestate")
		val statusj = exp.areNodesAlive(motes)
		val status = statusj.status
		for((m, s) <- status) log.info(m +": " + s)
		
		val activemotes = (for((m, s) <- status; if(s == Alive)) yield m).toList
		
		log.debug("Requesting Supported Channel Handlers")
		val handls = exp.supportedChannelHandlers
		for(h <- handls){
			println(h.format)
		}
		
		val setHand = "contiki"
		
		if(handls.find(_.name == setHand) == None){
			log.error("Can not set handler: {}", setHand)
			sys.exit(1)
		} else {
			log.debug("Setting Handler: {}", setHand)
			val chd = exp.setChannelHandler(activemotes, new WrappedChannelHandlerConfiguration("contiki") )
			if(!chd.success){
				log.error("Failed setting Handler")
				sys.exit(1)
			}
		}
		
		
		
		log.debug("Flashing")
		val flashj = exp.flash("sky-shell.ihex", activemotes)
		if(!flashj.success){
			log.error("Failed to flash nodes")
			sys.exit(1)
		}
		
		
		val bw = new MessageWaiter(activemotes,  "Contiki>")
		exp.addMessageInput(bw)
		
		log.debug("Resetting")
		val resj = exp.resetNodes(activemotes)
		if(!resj.success){
			log.error("Failed to reset nodes")
			sys.exit(1)
		}
		
		log.debug("Waiting for bootup")
		if(! bw.waitTimeout(10*1000)){
			log.error("Boot failed");
			sys.exit(1)
		}
		
		log.debug("Sending \\n")
		val snd = exp.send(activemotes, "help\n")
		if(!snd.success){
			log.error("Failed to send information to nodes")
			sys.exit(1)
		}
		log.debug("Waiting for answer")
		Thread.sleep(20 * 1000)
		
		log.debug("Removing Reservation")
		res.foreach(tb.freeReservation(_))
		
		
		log.debug("DONE")
		sys.exit(0)
	}
}
