object BuildDefaults {

  def buildScalaVersion = sys.props.getOrElse("scala.version", "2.11.8")
  def buildVersion = "0.1.4"
  def buildOrganization = "com.appministry"

}