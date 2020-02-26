name := "p3bacter"
 
version := "1.0" 
      
lazy val `p3bacter` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
scalaVersion := "2.11.11"

libraryDependencies ++= Seq( jdbc , cache , ws , specs2 % Test,filters )

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

libraryDependencies += "org.biojava" % "biojava-core" % "5.0.0"

libraryDependencies += "com.typesafe.slick" % "slick-codegen_2.11" % "3.2.0"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.25"

libraryDependencies += "com.typesafe.play" % "play-slick_2.11" % "2.1.0"

libraryDependencies += "com.github.tototoshi" % "slick-joda-mapper_2.11" % "2.3.0"

libraryDependencies += "commons-io" % "commons-io" % "2.5"

libraryDependencies += "joda-time" % "joda-time" % "2.9.9"

libraryDependencies += "org.zeroturnaround" % "zt-zip" % "1.11"

libraryDependencies += "org.apache.xmlgraphics" % "batik-codec" % "1.10"






      