package hmda.api.protocol.processing

import hmda.api.model.{ InstitutionSummary, Institutions }
import hmda.model.fi.{ Active, Inactive, Institution, InstitutionStatus }
import spray.json.{ DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat }

trait InstitutionProtocol extends DefaultJsonProtocol {
  implicit object InstitutionStatusJsonFormat extends RootJsonFormat[InstitutionStatus] {
    override def write(status: InstitutionStatus): JsValue = {
      status match {
        case Active => JsString("active")
        case Inactive => JsString("inactive")
      }
    }

    override def read(json: JsValue): InstitutionStatus = {
      json match {
        case JsString(s) => s match {
          case "active" => Active
          case "inactive" => Inactive
        }
        case _ => throw new DeserializationException("Institution Status expected")
      }
    }
  }

  implicit val institutionFormat = jsonFormat3(Institution.apply)
  implicit val institutionsFormat = jsonFormat1(Institutions.apply)

}
