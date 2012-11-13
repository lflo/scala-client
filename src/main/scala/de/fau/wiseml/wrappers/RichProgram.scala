package de.fau.wiseml.wrappers

import eu.wisebed.api.wsn.Program
import eu.wisebed.api.wsn.ProgramMetaData
import java.io.File
import java.io.FileInputStream
import java.io.BufferedInputStream
import java.io.DataInputStream

class RichProgram extends Program {

}

object RichProgram{
  	def apply( pathName:String,  name:String = "", other:String ="",   platform:String ="",  version:String="1.0"):RichProgram = {

		val programMetaData = new ProgramMetaData
		programMetaData.setName(name)
		programMetaData.setOther(other)
		programMetaData.setPlatform(platform)
		programMetaData.setVersion(version);

		val rv = new RichProgram
		val programFile = new File(pathName);

		val fis = new FileInputStream(programFile);
		val bis = new BufferedInputStream(fis);
		val dis = new DataInputStream(bis);

		val length = programFile.length();
		val binaryData = new Array[Byte](length.toInt)
		dis.readFully(binaryData);
		

		rv.setProgram(binaryData);
		rv.setMetaData(programMetaData);
		rv
		
	}
  	
}

