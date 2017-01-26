object BuildDefaults {

  def buildScalaVersion = sys.props.getOrElse("scala.version", "2.11.8")
  def buildVersion = "0.2.0"
  def buildOrganization = "uk.co.appministry"

}