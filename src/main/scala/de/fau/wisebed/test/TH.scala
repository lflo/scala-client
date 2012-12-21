package de.fau.wisebed.test

import de.uniluebeck.itm.tr.util.Logging
import java.io.File
import org.apache.log4j.Level
import org.slf4j.LoggerFactory
import scala.xml.XML
import de.fau.wisebed.Testbed
import java.util.GregorianCalendar
import java.util.Calendar
import de.fau.wisebed.Reservation.reservation2CRD
import scala.collection.JavaConversions.asScalaBuffer
import de.fau.wisebed.Experiment
import de.fau.wisebed.messages.MessageLogger
import de.fau.wisebed.messages.MsgLiner
import de.fau.wisebed.messages.MessageLogger
import de.fau.wisebed.wrappers
import de.fau.wisebed.jobs.MoteAliveState._
import de.fau.wisebed.wrappers.WrappedChannelHandlerConfiguration
import de.fau.wisebed.Reservation


object TH {
	Logging.setLoggingDefaults(Level.DEBUG) // new PatternLayout("%-11d{HH:mm:ss,SSS} %-5p - %m%n"))

	var activemotes:List[String] = null
	var exp:Experiment = null
	var res:List[Reservation] = null
	
	
	val log = LoggerFactory.getLogger(this.getClass)
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
	
	def init(resTime:Int){
		log.debug("Requesting reservations")
		res = tb.getReservations(resTime)
		
		for(r <- res) {
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.getNodeURNs.mkString(", ")) 
		}
		
		if(!res.exists(_.now)) {
			log.debug("No Reservations or in the Past- Requesting")
			val from = new GregorianCalendar
			from.add(Calendar.MINUTE, -1)
			val to = new GregorianCalendar
			to.add(Calendar.MINUTE, 120)
			val r = tb.makeReservation(from, to, motes, "morty")
			log.debug("Got Reservations: \n" +  r.dateString() + " for " + r.getNodeURNs.mkString(", ")) 
			res ::= r
		}
		
		exp = new Experiment(res.toList, tb)
		
		
		exp.addMessageInput(  new MessageLogger(mi => {
			import wrappers.WrappedMessage._
			log.info("Got message from " + mi.node + ": " + mi.dataString)
		}) with MsgLiner)
		
		log.debug("Requesting Motestate")
		val statusj = exp.areNodesAlive(motes)
		val status = statusj.status
		for((m, s) <- status) log.info(m +": " + s)
		
		activemotes = (for((m, s) <- status; if(s == Alive)) yield m).toList
		
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
	}
	
	def flash(file:String){
		log.debug("Flashing " + file)
		val flashj = exp.flash(file, activemotes)
		if(!flashj.success){
			log.error("Failed to flash nodes")
			sys.exit(1)
		}
	}
	
	def reset{
		log.debug("Resetting")
		val resj = exp.resetNodes(activemotes)
		if(!resj.success){
			log.error("Failed to reset nodes")
			sys.exit(1)
		}
	}
	
	def finish{
		log.debug("Removing Reservation")
		res.foreach(tb.freeReservation(_))
	}
	
	
}