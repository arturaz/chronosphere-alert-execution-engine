name := "alert-execution-engine"
scalaVersion := "2.13.7"

val Http4sVersion = "1.0.0-M30"

scalacOptions := Seq(
  "-deprecation"
)

libraryDependencies ++= Seq(
  "org.http4s"      %% "http4s-ember-client" % Http4sVersion,
  "org.http4s"      %% "http4s-play-json"    % Http4sVersion,
  "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")