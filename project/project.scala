object project extends ProjectSettings {
  def scalaVersion = "2.10.4"
  def version = "0.10.0"
  def name = "core"
  def description = "The Rapture Core project provides a common foundation upon which other Rapture projects are based, however it provides utilities which may be useful in any project."
  def dependencies = Nil
  def thirdPartyDependencies = Nil
  def imports = Seq("rapture.core._")
}
