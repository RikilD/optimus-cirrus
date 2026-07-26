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
package optimus.examples.platform.entities

import optimus.examples.testing.OptimusTestRunner
import optimus.platform._
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the tweaking behaviour that `optimus.examples.platform02.graph.BasicTweaks` demonstrates, as a test rather
 * than as an app printing to stdout.
 */
@RunWith(classOf[OptimusTestRunner])
class SimpleCalcsTest {

  @Test @entersGraph def computesFromDefaults(): Unit = {
    val calcs = SimpleCalcs("defaults")
    // a = x * 2 + y, with the constructor defaults x = 7, y = 0
    assertEquals(14, calcs.a)
    assertEquals(21.0, calcs.b(1.5), 0.0)
  }

  @Test @entersGraph def tweakingAnInputRecomputesDependents(): Unit = {
    val calcs = SimpleCalcs("tweaked")

    // Tweaking x re-evaluates a, which depends on it.
    assertEquals(20, given(calcs.x := 10) { calcs.a })

    // Tweaks only apply inside the given block.
    assertEquals(14, calcs.a)
  }

  @Test @entersGraph def tweakingADerivedNodeOverridesItsFormula(): Unit = {
    val calcs = SimpleCalcs("overridden")

    // a is itself tweakable, so it can be replaced outright rather than recomputed from x.
    assertEquals(100, given(calcs.a := 100) { calcs.a })
    assertEquals(150.0, given(calcs.a := 100) { calcs.b(1.5) }, 0.0)
  }

  @Test @entersGraph def entitiesWithEqualPropertiesAreTheSameInstance(): Unit = {
    // @entity instances are interned: constructing one twice yields the same object.
    assertEquals(SimpleCalcs("same", 3, 4), SimpleCalcs("same", 3, 4))
  }
}
