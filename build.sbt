scalaVersion := "2.13.0"

libraryDependencies := Seq(
  "org.typelevel" %% "cats-effect" % "2.1.0",
  "org.typelevel" %% "cats-core" % "2.1.0",
  "eu.timepit" %% "refined" % "0.9.12",
  "co.fs2" %% "fs2-core" % "2.2.2"
)
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0")
