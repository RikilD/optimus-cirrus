/*
 * Morgan Stanley makes this available to you under the Apache License, Version 2.0 (the "License").
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package optimus.examples.testing

import optimus.platform.OptimusTask
import optimus.platform.dal.config.DalAppId
import optimus.platform.dal.config.DalEnv
import optimus.platform.dal.config.DalLocation
import org.junit.runner.notification.RunNotifier
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.model.Statement

/**
 * A JUnit runner that starts an Optimus runtime around the test class, so that tests can evaluate `@node` code, create
 * `@entity` instances and apply tweaks in `given` blocks. Without it, anything touching the graph fails with an
 * uninitialised `EvaluationContext`.
 *
 * Use it as:
 * {{{
 *   @RunWith(classOf[OptimusTestRunner])
 *   class MyTest {
 *     @Test @entersGraph def myTest(): Unit = ...
 *   }
 * }}}
 *
 * Test methods that reach the graph need `@entersGraph`, exactly like `OptimusApp.run`.
 *
 * The runtime is created once per test class and torn down afterwards, and it is configured with `DalEnv("none")` -- no
 * DAL. Tests that need stored entities would need a broker, which this workspace has no way to start.
 *
 * Note that `EvaluationContext` initialisation is per thread, and sbt runs test classes in a shared forked JVM: once
 * one class has run under this runner, a class that forgot `@RunWith` may still pass by inheriting that thread's
 * context. Run such a class on its own (`testOnly`) to see it fail as it should.
 */
class OptimusTestRunner(clazz: Class[_]) extends BlockJUnit4ClassRunner(clazz) {
  override protected def classBlock(notifier: RunNotifier): Statement = {
    val tests = super.classBlock(notifier)
    new Statement {
      override def evaluate(): Unit = OptimusTestRunner.task.withOptimus(() => tests.evaluate())
    }
  }
}

object OptimusTestRunner {

  /**
   * The app id matches the default `optimus.testsuite.override.appId` in `optimus.graph.Settings`, so that graph
   * diagnostics attribute anything these tests produce to the test runner.
   */
  private val task: OptimusTask = new OptimusTask {
    override protected def appId: DalAppId = DalAppId("OptimusTestRunner")
    override protected def dalLocation: DalLocation = DalEnv("none")
  }
}
