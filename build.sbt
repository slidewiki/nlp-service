import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseKeys
import NativePackagerHelper._

EclipseKeys.projectFlavor := EclipseProjectFlavor.Java           // Java project. Don't expect Scala IDE
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources)  // Use .class files instead of generated .scala files for views and routes


name := """nlp-services"""

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.8"
//scalaVersion := "2.12.0" // not compatible with play 2.5.x

libraryDependencies ++= Seq(
	javaWs,
  "org.apache.commons" % "commons-compress" % "1.13",
  "commons-io" % "commons-io" % "2.5",
  "org.apache.opennlp" % "opennlp-tools" % "1.7.1",
  "io.swagger" %% "swagger-play2" % "1.5.3",
  "org.webjars" % "swagger-ui" % "2.2.10",
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1-m03",
  "com.optimaize.languagedetector" % "language-detector" % "0.6",
  "org.jsoup" % "jsoup" % "1.10.2",
  "org.glassfish.jersey.core" % "jersey-client" % "2.26-b02"
 )


mappings in Universal ++= directory("resources")


fork in run := true