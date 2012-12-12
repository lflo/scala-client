package de.fau.wisebed.messages

import de.fau.wisebed._
import scala.parallel.Future
import eu.wisebed.api._
import scala.collection.mutable
import eu.wisebed.api.common.Message
import de.fau.wisebed.wrappers.WrappedMessage._
import scala.collection.mutable
import org.slf4j.LoggerFactory
import java.util.Date

/**
 * @todo Make weak to remove when no one listens
 */

/**
 * @param nodes The nodes to monitor
 * @param needle The string to wait for. This may be multiline 
 *
 */
class MessageWaiter(nodes:List[Node], needle:String) extends MessageInput with Future[Boolean]{
	val log = LoggerFactory.getLogger(this.getClass) 

	private var stop = false
	
	private var mbuf = nodes.map(_ -> "").toMap
	private var ready = nodes.map(_ -> false).toMap
	
	def isDone:Boolean = stop
	
	def isOK:Boolean = ready.forall(_._2 == true)
	
	override def isWeak = true
	
	def apply():Boolean = synchronized{
		while(!isDone) wait()
		isOK
	}
	
	def waitTimeout(timeout:Int):Boolean = synchronized{
		def date = (new Date).getTime
		var ctime:Long = date
		val time:Long = ctime + timeout
		while( {ctime = date; ctime} < time && !isDone) wait(time - ctime)
		isOK
	}
	
	def unregister() = synchronized {
		stop = true
		notify()
		stopInput()
	}
	
	override def handleMsg(msg:Message) {
		val stro = mbuf.get(msg.node)
		if(stro.isEmpty) return		
		val str = stro.get + msg.dataString		
		if(str.contains(needle)) {
			ready += (msg.node -> true)
			mbuf -= msg.node
			if(isOK) unregister()
		} else {
			// Save the rest of the String, that might contain the needle. More optimization is unreasonable. 
			mbuf += (msg.node -> str.takeRight(needle.length) ) 
		}		
	}
}
