package msjava.base.slr.internal

class ServiceEnvironment(val name: String)

object ServiceEnvironment {
  val dev = new ServiceEnvironment("dev")
  val qa = new ServiceEnvironment("qa")
  val uat = new ServiceEnvironment("uat")
  val prod = new ServiceEnvironment("prod")
}
