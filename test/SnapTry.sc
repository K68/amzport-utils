val fetchers = List.range(1, 6)
println(fetchers)

val c = ""
c.split("#!#").length

val observersCache = Array[Int](1,2,3,4,5)

observersCache.update(3, 100)

observersCache

val saltValue = "mol-rand-salt"

import java.io.File

import com.roundeights.hasher.Implicits._

"admin666888".salt(saltValue).sha1.hex

"666666".salt(saltValue).sha1.hex

"admin666888".salt(saltValue).crc32.hex + "admin888666".salt(saltValue).crc32.hex

//import kantan.csv.rfc
//import kantan.csv._
//import kantan.csv.ops._
//import kantan.csv.generic._
//import kantan.csv.java8._
//
//val file = new File("D:\\git\\amzport-utils\\importTmp.csv")
//val ifReader = file.asCsvReader[(String, String)](rfc)
//ifReader.foreach(i => println(i))