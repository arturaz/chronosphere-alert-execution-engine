name := "alert-execution-engine"
scalaVersion := "2.13.7"

scalacOptions := Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-Werror",
  "-Ywarn-value-discard",
  "-language:implicitConversions"
)

val Http4sVersion = "1.0.0-M30"
val ScalaLoggerVersion = "0.8.4"

libraryDependencies ++= Seq(
  "org.http4s"        %% "http4s-blaze-client" % Http4sVersion,
  "org.http4s"        %% "http4s-play-json"    % Http4sVersion,
  "org.http4s"        %% "http4s-dsl"          % Http4sVersion,
  "ch.qos.logback"    % "logback-classic"      % "1.2.8",
  "com.emarsys"       %% "scala-logger"        % ScalaLoggerVersion,
  "com.emarsys"       %% "scala-logger-ce3"    % ScalaLoggerVersion
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")