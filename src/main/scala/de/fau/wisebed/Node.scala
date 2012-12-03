package de.fau.wisebed

import scala.collection._
import scala.collection.JavaConversions._

case class Node(val id:String)

object Node {
	implicit def string2Node(s: String):Node = Node(s)
	implicit def stringList2Nodes(ss: List[String]):List[Node] = ss.map(Node(_))
	implicit def nodes2JavaList(ns: List[Node]):java.util.List[String] = ns.map(_.id)
}