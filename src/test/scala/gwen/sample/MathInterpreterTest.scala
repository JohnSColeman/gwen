/*
 * Copyright 2014 Branko Juric, Brady Wood
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gwen.sample

import gwen.dsl.Failed
import gwen.dsl.Passed
import gwen.eval.GwenOptions
import java.io.File
import org.scalatest.FlatSpec

class MathInterpreterTest extends FlatSpec {
  
  "math features" should "evaluate" in {
    
    val dir = new File(getClass().getResource("/gwen/sample/BasicMath.feature").getFile()).getParentFile()
    val relativeDir = new File(new File(".").toURI().relativize(dir.toURI()).getPath());
    
    val options = GwenOptions(
      batch = true,
      reportDir = Some(new File("target/report")), 
      paths = List(relativeDir))
      
    val intepreter = new MathInterpreter()
    intepreter.execute(options, None) match {
      case Passed(_) => // excellent :)
      case Failed(_, error) => error.printStackTrace(); fail(error.getMessage())
      case _ => fail("evaluation expected but got noop")
    }
  }
}