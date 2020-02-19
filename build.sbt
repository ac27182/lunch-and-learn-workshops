scalaVersion := "2.13.0"

val cats    = "2.1.0"
val refined = "0.9.12"
val fs2     = "2.2.2"
val http4s  = "0.21.1"
val circe   = "0.13.0"

libraryDependencies := Seq(
  "org.typelevel" %% "cats-effect"         % cats,
  "org.typelevel" %% "cats-core"           % cats,
  "eu.timepit"    %% "refined"             % refined,
  "co.fs2"        %% "fs2-core"            % fs2,
  "org.http4s"    %% "http4s-circe"        % http4s,
  "org.http4s"    %% "http4s-dsl"          % http4s,
  "org.http4s"    %% "http4s-blaze-client" % http4s,
  "org.http4s"    %% "http4s-blaze-server" % http4s,
  "io.circe"      %% "circe-core"          % circe,
  "io.circe"      %% "circe-parser"        % circe,
  "io.circe"      %% "circe-generic"       % circe
)
addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3")
addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")
