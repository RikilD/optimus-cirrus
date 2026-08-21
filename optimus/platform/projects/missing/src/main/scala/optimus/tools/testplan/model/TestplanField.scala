package optimus.tools.testplan.model

/**
 * Compile-time stand-in for optimus.tools.testplan.model.TestplanField.
 *
 * Only TestCases is referenced here, and only for its name, which is matched against a
 * testplan column header.
 */
object TestplanField {
  case object TestCases {
    override def toString: String = "TestCases"
  }
}
