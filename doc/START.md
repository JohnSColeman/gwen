Getting Started with Gwen
=========================

Building from Source
--------------------

This project uses sbt as the build tool.

If you would like to build a binary from the source:

1. Download a [Git client](http://git-scm.com/downloads)
2. Clone this Git repository 
3. Download and install the latest [sbt](http://www.scala-sbt.org/) version 
4. Change to the gwen directory
5. Run `sbt test` to compile and run all tests to verify all is OK
6. When successful, run `sbt` to start the sbt console

If you would like to work with the source in the Scala IDE:

- Perform steps 1 to 5 above
- Run `sbt eclipse` to create all eclipse files
- Download and install the [Scala IDE](http://scala-ide.org/) for Scala 2.11.
- Import the project into the IDE 

Quick Start Dev Guide
---------------------

> Please ensure that you have performed steps 1 to 6 (above) before proceeding.  

The aim of this guide is to show you how to:

- Implement an evaluation engine and mix it into the interpreter
- Use the interpreter to evaluate steps and features on your engine

For this dev guide, we will walk through the sample math interpreter
implementation included in the test source of this project. The complete 
source is available here:

- [src/test/scala/gwen/sample/MathInterpreter.scala](../src/test/scala/gwen/sample/MathInterpreter.scala)

This interpreter uses the following math service implementation to perform 
integer addition: 

```
package gwen.sample

class MathService {
  def plus(x: Int, y: Int) = x + y
}
```

A realistic and practical interpreter would obviously target a much more 
sophisticated service or API. But the purpose of this guide is to demonstrate 
a short but complete example of how one service can be integrated. The same 
approach would still apply for any other service. It does not matter if that 
service is simple or complex.

### Defining an environment context

The first thing we need to do is make the above math service both accessible 
and usable from within the evaluation engine that we will develop. We need the 
ability to access the service and invoke it with parameters. And to pass 
parameters we need variables. For exactly these purposes, Gwen provides the 
_EnvContext_ class for us to extend. We need to define an environment context 
to store the following:

- A reference to the math service
- A data scope for binding named variables

We now define a _MathEnvContext_ to store a reference to our service and 
provide us with variables:

```
package gwen.sample

import gwen.eval.EnvContext

class MathEnvContext(val mathService: MathService) 
  extends EnvContext {
  def vars = dataScope("vars")
}
```

### Defining an evaluation engine

We now have a context that encapsulates both our service and our state. Next, 
we need to define the evaluation engine that will map Gherkin steps to 
function calls on our _MathService_. This engine will do all the required math 
and variable binding work. We will define it to support the following step 
expressions:

- x = value
- x = y
- z = x + y
- x == y

Where:

- x and y are single character variable names (from lower case a to z)
- value is a literal integer value
- = performs assignment
- == performs a comparison
- and + performs addition

We now define our evaluation engine by extending the _EvalEngine_ trait over 
the _MathEnvContext_ type we defined above. We implement the two abstract 
methods _init_ and _evaluate_ to initialize the context and evaluate steps 
respectively. Note that we use the inherited logger for logging and regex 
string interpolator for matching step expressions and capturing their 
parameters. Note also that the step expressions are just strings. We do not 
necessarily have to match them using the regex interpolator. In this instance 
we choose to use it for the expressive power it provides.  Also, you will 
notice that this evaluation engine is stateless. It does not store any service 
or state within itself. All of that is stored in the evaluation context (which 
lives only on the stack). This is to support parallel execution.

```
package gwen.sample

import gwen.dsl.Step
import gwen.eval.EvalEngine
import gwen.eval.GwenOptions

trait MathEvalEngine extends EvalEngine[MathEnvContext] {
 
  override def init(options: GwenOptions): MathEnvContext =
    new MathEnvContext(new MathService())
 
  override def evaluate(step: Step, env: MathEnvContext) {
    val vars = env.vars
    step.expression match {
      case r"([a-z])$x = (\d+)$value" =>
        vars.set(x, value)
      case r"([a-z])$x = ([a-z])$y" =>
        vars.set(x, vars.get(y))
      case r"z = ([a-z])$x \+ ([a-z])$y" =>
        val xvalue = vars.get(x).toInt
        val yvalue = vars.get(y).toInt
        logger.info(s"evaluating z = $xvalue + $yvalue")
        val zresult = env.mathService.plus(xvalue, yvalue)
        vars.set("z", zresult.toString)
      case r"([a-z])$x == (\d+)$value" =>
        assert (vars.get(x).toInt == value.toInt)
      case _ =>
        super.evaluate(step, env)
    }
  }
}
```

### Mixing in the evaluation engine

We then mix the above engine into the Gwen interpreter.

```
package gwen.sample

import gwen.eval.GwenInterpreter

class MathInterpreter 
  extends GwenInterpreter[MathEnvContext]
  with MathEvalEngine
```

### Making the interpreter a standalone application

We also make it an application so we can launch it standalone.

```
package gwen.sample

import gwen.eval.GwenApp

object MathInterpreter 
  extends GwenApp(new MathInterpreter)
```
### Launching the REPL console

We are now ready to start using our math interpreter.  We will start by 
launching it in REPL mode. Issue the following scala command:

```
scala gwen.sample.MathInterpreter
```

Or in the sbt console:

```
test:run
```

The REPL will launch and wait for you to start entering steps:

```
   __ ___      _____ _ __     _    
  / _` \ \ /\ / / _ \ '_ \   { \," 
 | (_| |\ V  V /  __/ | | | {_`/   
  \__, | \_/\_/ \___|_| |_|   `    
  |___/                            

Welcome to Gwen!

INFO - Initialising environment context
INFO - MathEnvContext initialised

REPL Console

Enter steps to evaluate or type exit to quit..

gwen>_
```

Once launched, proceed to enter the following steps (one at a time) and 
observe the results. Take note that the evaluation engine we implemented above 
performs an exact match on incoming step expressions. Therefore it will not 
accept 'fuzzy' input. So be sure to preserve the case and spacing of each 
step exactly as listed below.

- Given x = 1
- And y = 2
- When z = x + y
- Then z == 3

```
gwen>Given x = 1

INFO - Evaluating Step: Given x = 1
INFO - Binding 'x = 1' to 'default' vars scope
INFO - [0.0141 secs] Passed Step: Given x = 1

    Given x = 1 # [0.0141 secs] Passed

[Passed]

gwen>And y = 2

INFO - Evaluating Step: And y = 2
INFO - Binding 'y = 2' to 'default' vars scope
INFO - [0.0003 secs] Passed Step: And y = 2

      And y = 2 # [0.0003 secs] Passed

[Passed]

gwen>When z = x + y

INFO - Evaluating Step: When z = x + y
INFO - Found 'x = 1' in 'default' vars scope
INFO - Found 'y = 2' in 'default' vars scope
INFO - evaluating z = 1 + 2
INFO - Binding 'z = 3' to 'default' vars scope
INFO - [0.0547 secs] Passed Step: When z = x + y

     When z = x + y # [0.0547 secs] Passed

[Passed]

gwen>Then z == 3

INFO - Evaluating Step: Then z == 3
INFO - Found 'z = 3' in 'default' vars scope
INFO - [0.0006 secs] Passed Step: Then z == 3

     Then z == 3 # [0.0006 secs] Passed

[Passed]

gwen>_
```

### Showing what is in memory

At any time, you can enter _env_ into the prompt to see a dump of all 
variables currently in memory:

```
gwen>env

{
  "env" : {
    "data" : [ {
      "vars" : [ {
        "scope" : "default",
        "atts" : [ {
          "x" : "1"
        }, {
          "y" : "2"
        }, {
          "z" : "3"
        } ]
      } ]
    } ]
  }
}

gwen>_
``` 

Type _exit_ to quit the REPL.

### Evaluating feature files

We have now verified that our interpreter works and have experimented with it 
a little. We will now write a Gherkin feature file that captures the same 
steps we just entered and evaluated in the REPL. This will give us an 
equivalent behavioral specification in the form of a plain text feature 
file. 

```
 Feature: Integer addition

Scenario: 1 plus 2 should yield 3
    Given x = 1
      And y = 2
     When z = x + y
     Then z == 3
```

This feature file can be located in the project here:

- [src/test/resources/gwen/sample/BasicMath.feature](../src/test/resources/gwen/sample/BasicMath.feature)

Now lets evaluate this feature file using our math interpreter.  We do this 
by invoking the interpreter in batch mode and passing this file in as a 
parameter:

```
scala gwen.sample.MathInterpreter -b src/test/resources/gwen/sample/BasicMath.feature
```

Or in the sbt console:

```
test:run -b src/test/resources/gwen/sample/BasicMath.feature
```

The interpreter will evaluate the feature and exit, and you will see the 
following output: 

```
   __ ___      _____ _ __     _    
  / _` \ \ /\ / / _ \ '_ \   { \," 
 | (_| |\ V  V /  __/ | | | {_`/   
  \__, | \_/\_/ \___|_| |_|   `    
  |___/                            

Welcome to Gwen! 

INFO - Found FeatureUnit(src/test/resources/gwen/sample/BasicMath.feature,List(src/test/resources/gwen/sample/Math.meta))
INFO - Initialising environment context
INFO - MathEnvContext initialised
INFO - Loading meta feature: src/test/resources/gwen/sample/Math.meta
INFO - Interpreting feature file: src/test/resources/gwen/sample/Math.meta
INFO - Evaluating feature: Math functions
INFO - Loading StepDef: ++x
INFO - Loaded FeatureSpec: Math functions
INFO - Feature file interpreted: src/test/resources/gwen/sample/Math.meta

   Feature: Math functions # Loaded

  @StepDef
  Scenario: ++x # Loaded
      Given y = 1 # Loaded
       When z = x + y # Loaded
       Then x = z # Loaded

INFO - Loaded meta feature: Math functions
INFO - Interpreting feature file: src/test/resources/gwen/sample/BasicMath.feature
INFO - Evaluating feature: Integer addition
INFO - Evaluating Scenario: 1 plus 2 should yield 3
INFO - Evaluating Step: Given x = 1
INFO - Binding 'x = 1' to 'default' vars scope
INFO - [0.0173 secs] Passed Step: Given x = 1
INFO - Evaluating Step: And y = 2
INFO - Binding 'y = 2' to 'default' vars scope
INFO - [0.0002 secs] Passed Step: And y = 2
INFO - Evaluating Step: When z = x + y
INFO - Found 'x = 1' in 'default' vars scope
INFO - Found 'y = 2' in 'default' vars scope
INFO - evaluating z = 1 + 2
INFO - Binding 'z = 3' to 'default' vars scope
INFO - [0.0529 secs] Passed Step: When z = x + y
INFO - Evaluating Step: Then z == 3
INFO - Found 'z = 3' in 'default' vars scope
INFO - [0.0004 secs] Passed Step: Then z == 3
INFO - [0.0708 secs] Passed Scenario: 1 plus 2 should yield 3
INFO - [0.0708 secs] Passed FeatureSpec: Integer addition
INFO - Feature file interpreted: src/test/resources/gwen/sample/BasicMath.feature

   Feature: Integer addition # [0.0708 secs] Passed

  Scenario: 1 plus 2 should yield 3 # [0.0708 secs] Passed
      Given x = 1 # [0.0173 secs] Passed
        And y = 2 # [0.0002 secs] Passed
       When z = x + y # [0.0529 secs] Passed
       Then z == 3 # [0.0004 secs] Passed

INFO - Closing environment context

1 feature: Passed 1, Failed 0, Skipped 0, Pending 0, Loaded 0 
1 scenario: Passed 1, Failed 0, Skipped 0, Pending 0, Loaded 0
4 steps: Passed 4, Failed 0, Skipped 0, Pending 0, Loaded 0

[0.0708 secs] Passed

[success] Total time: 1 s, completed May 8, 2014 1:41:14 AM
```

### Composing steps in meta

We will now compose a ++x increment function as a step definition. This 
function will reuse the steps we currently have to increment the value 
contained in the variable named x. We will define this in a meta file so that 
we can load it into the interpreter first before evaluating any features.

```
 Feature: Math functions

@StepDef
Scenario: ++x
    Given y = 1
     When z = x + y
     Then x = z
```

This meta file can be located in the project here:
 
- [src/test/resources/gwen/sample/Math.meta](../src/test/resources/gwen/sample/Math.meta)

### Loading meta

We can now load this function into the math interpreter by passing the meta 
file in as a parameter. Run the following command to load this meta into the 
REPL.

```
scala gwen.sample.MathInterpreter -m src/test/resources/gwen/sample/Math.meta
```

Or in the sbt console:

```
test:run -m src/test/resources/gwen/sample/Math.meta
```

The ++x function will load and become available.

```
   __ ___      _____ _ __     _    
  / _` \ \ /\ / / _ \ '_ \   { \," 
 | (_| |\ V  V /  __/ | | | {_`/   
  \__, | \_/\_/ \___|_| |_|   `    
  |___/                            

Welcome to Gwen! 

INFO - Initialising environment context
INFO - MathEnvContext initialised
INFO - Loading meta feature: src/test/resources/gwen/sample/Math.meta
INFO - Interpreting feature file: src/test/resources/gwen/sample/Math.meta
INFO - Evaluating feature: Math functions
INFO - Loading StepDef: ++x
INFO - Loaded FeatureSpec: Math functions
INFO - Feature file interpreted: src/test/resources/gwen/sample/Math.meta

   Feature: Math functions # Loaded

  @StepDef
  Scenario: ++x # Loaded
      Given y = 1 # Loaded
       When z = x + y # Loaded
       Then x = z # Loaded

INFO - Loaded meta feature: Math functions

REPL Console

Enter steps to evaluate or type exit to quit..

gwen>_
```

Now proceed to enter the following steps to initialise and then increment 
the variable x:

- Given x = 0
- When ++x
- Then x == 1

```
gwen>Given x = 0

INFO - Evaluating Step: Given x = 0
INFO - Binding 'x = 0' to 'default' vars scope
INFO - [0.0122 secs] Passed Step: Given x = 0

    Given x = 0 # [0.0122 secs] Passed

[Passed]

gwen>When ++x

INFO - Evaluating Step: When ++x
INFO - Evaluating StepDef: ++x
INFO - Evaluating Step: Given y = 1
INFO - Binding 'y = 1' to 'default' vars scope
INFO - [0.0003 secs] Passed Step: Given y = 1
INFO - Evaluating Step: When z = x + y
INFO - Found 'x = 0' in 'default' vars scope
INFO - Found 'y = 1' in 'default' vars scope
INFO - evaluating z = 0 + 1
INFO - Binding 'z = 1' to 'default' vars scope
INFO - [0.0552 secs] Passed Step: When z = x + y
INFO - Evaluating Step: Then x = z
INFO - Found 'z = 1' in 'default' vars scope
INFO - Binding 'x = 1' to 'default' vars scope
INFO - [0.0003 secs] Passed Step: Then x = z
INFO - StepDef evaluated: ++x
INFO - [0.0558 secs] Passed Step: When ++x

     When ++x # [0.0558 secs] Passed

[Passed]

gwen>Then x == 1

INFO - Evaluating Step: Then x == 1
INFO - Found 'x = 1' in 'default' vars scope
INFO - [0.0007 secs] Passed Step: Then x == 1

     Then x == 1 # [0.0007 secs] Passed

[Passed]

gwen>_
```
### Evaluating features with meta

Again, like before, we can capture these exact steps in a feature file: 

```
 Feature: Increment integer

Scenario: Incrementing 0 should yield 1
    Given x = 0
     When ++x
     Then x == 1
```

This feature file can be located in the project here:

- [src/test/resources/gwen/sample/MetaMath.feature](../src/test/resources/gwen/sample/MetaMath.feature)

We can now evaluate this feature by passing it to the interpreter. Gwen will 
automatically discover and load the Math.meta file if it finds it in the same 
directory as the feature file (which in this case it will). In fact it will 
load any file with a meta extension that it finds in the same directory. But 
if it finds more than one meta file in the directory then it will error.

We now evaluate our MetaMath.feature with the following command:

```
scala gwen.sample.MathInterpreter -b src/test/resources/gwen/sample/MetaMath.feature
```

Or in the sbt console:

```
test:run -b src/test/resources/gwen/sample/MetaMath.feature
```

The interpreter will discover and load the meta and then evaluate the 
feature:

```
   __ ___      _____ _ __     _    
  / _` \ \ /\ / / _ \ '_ \   { \," 
 | (_| |\ V  V /  __/ | | | {_`/   
  \__, | \_/\_/ \___|_| |_|   `    
  |___/                            

Welcome to Gwen! 

INFO - Found FeatureUnit(src/test/resources/gwen/sample/MetaMath.feature,List(src/test/resources/gwen/sample/Math.meta))
INFO - Initialising environment context
INFO - MathEnvContext initialised
INFO - Loading meta feature: src/test/resources/gwen/sample/Math.meta
INFO - Interpreting feature file: src/test/resources/gwen/sample/Math.meta
INFO - Evaluating feature: Math functions
INFO - Loading StepDef: ++x
INFO - Loaded FeatureSpec: Math functions
INFO - Feature file interpreted: src/test/resources/gwen/sample/Math.meta

   Feature: Math functions # Loaded

  @StepDef
  Scenario: ++x # Loaded
      Given y = 1 # Loaded
       When z = x + y # Loaded
       Then x = z # Loaded

INFO - Loaded meta feature: Math functions
INFO - Interpreting feature file: src/test/resources/gwen/sample/MetaMath.feature
INFO - Evaluating feature: Increment integer
INFO - Evaluating Scenario: Incrementing 1 should yield 2
INFO - Evaluating Step: Given x = 1
INFO - Binding 'x = 1' to 'default' vars scope
INFO - [0.0118 secs] Passed Step: Given x = 1
INFO - Evaluating Step: When ++x
INFO - Evaluating StepDef: ++x
INFO - Evaluating Step: Given y = 1
INFO - Binding 'y = 1' to 'default' vars scope
INFO - [0.0002 secs] Passed Step: Given y = 1
INFO - Evaluating Step: When z = x + y
INFO - Found 'x = 1' in 'default' vars scope
INFO - Found 'y = 1' in 'default' vars scope
INFO - evaluating z = 1 + 1
INFO - Binding 'z = 2' to 'default' vars scope
INFO - [0.0737 secs] Passed Step: When z = x + y
INFO - Evaluating Step: Then x = z
INFO - Found 'z = 2' in 'default' vars scope
INFO - Binding 'x = 2' to 'default' vars scope
INFO - [0.0003 secs] Passed Step: Then x = z
INFO - StepDef evaluated: ++x
INFO - [0.0742 secs] Passed Step: When ++x
INFO - Evaluating Step: Then x == 2
INFO - Found 'x = 2' in 'default' vars scope
INFO - [0.0003 secs] Passed Step: Then x == 2
INFO - [0.0863 secs] Passed Scenario: Incrementing 1 should yield 2
INFO - [0.0863 secs] Passed FeatureSpec: Increment integer
INFO - Feature file interpreted: src/test/resources/gwen/sample/MetaMath.feature

   Feature: Increment integer # [0.0863 secs] Passed

  Scenario: Incrementing 1 should yield 2 # [0.0863 secs] Passed
      Given x = 1 # [0.0118 secs] Passed
       When ++x # [0.0742 secs] Passed
       Then x == 2 # [0.0003 secs] Passed

INFO - Closing environment context

1 feature: Passed 1, Failed 0, Skipped 0, Pending 0, Loaded 0 
1 scenario: Passed 1, Failed 0, Skipped 0, Pending 0, Loaded 0
3 steps: Passed 3, Failed 0, Skipped 0, Pending 0, Loaded 0

[0.0863 secs] Passed

[success] Total time: 0 s, completed May 8, 2014 1:44:29 AM
```

### Testing the interpreter

To complete the development work, we will implement the following test class 
to exercise the evaluation of all the feature files we have written (that 
is: all files in the src/test/resources/gwen/sample directory). In this test 
we will also specify that we want the HTML evaluation reports to be generated 
in the target/report directory.

```
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
```

This test class can be located in the project here:

- [src/test/scala/gwen/sample/MathInterpreterTest.scala](../src/test/scala/gwen/sample/MathInterpreterTest.scala)

The test output follows:

```
INFO - Found FeatureUnit(target/scala-2.11/test-classes/gwen/sample/BasicMath.feature,List(target/scala-2.11/test-classes/gwen/sample/Math.meta))
INFO - Initialising environment context
INFO - MathEnvContext initialised
INFO - Loading meta feature: target/scala-2.11/test-classes/gwen/sample/Math.meta
INFO - Interpreting feature file: target/scala-2.11/test-classes/gwen/sample/Math.meta
INFO - Evaluating feature: Math functions
INFO - Loading StepDef: ++x
INFO - Loaded FeatureSpec: Math functions
INFO - Feature file interpreted: target/scala-2.11/test-classes/gwen/sample/Math.meta

   Feature: Math functions # Loaded

  @StepDef
  Scenario: ++x # Loaded
      Given y = 1 # Loaded
       When z = x + y # Loaded
       Then x = z # Loaded

INFO - Loaded meta feature: Math functions
INFO - Interpreting feature file: target/scala-2.11/test-classes/gwen/sample/BasicMath.feature
INFO - Evaluating feature: Integer addition
INFO - Evaluating Scenario: 1 plus 2 should yield 3
INFO - Evaluating Step: Given x = 1
INFO - Binding 'x = 1' to 'default' vars scope
INFO - [0.0138 secs] Passed Step: Given x = 1
INFO - Evaluating Step: And y = 2
INFO - Binding 'y = 2' to 'default' vars scope
INFO - [0.0002 secs] Passed Step: And y = 2
INFO - Evaluating Step: When z = x + y
INFO - Found 'x = 1' in 'default' vars scope
INFO - Found 'y = 2' in 'default' vars scope
INFO - evaluating z = 1 + 2
INFO - Binding 'z = 3' to 'default' vars scope
INFO - [0.0480 secs] Passed Step: When z = x + y
INFO - Evaluating Step: Then z == 3
INFO - Found 'z = 3' in 'default' vars scope
INFO - [0.0004 secs] Passed Step: Then z == 3
INFO - [0.0624 secs] Passed Scenario: 1 plus 2 should yield 3
INFO - [0.0624 secs] Passed FeatureSpec: Integer addition
INFO - Feature file interpreted: target/scala-2.11/test-classes/gwen/sample/BasicMath.feature

   Feature: Integer addition # [0.0624 secs] Passed

  Scenario: 1 plus 2 should yield 3 # [0.0624 secs] Passed
      Given x = 1 # [0.0138 secs] Passed
        And y = 2 # [0.0002 secs] Passed
       When z = x + y # [0.0480 secs] Passed
       Then z == 3 # [0.0004 secs] Passed

INFO - Generating meta detail report [Math functions]..
INFO - Meta detail report generated: target/report/target-scala-2.11-test-classes-gwen-sample-BasicMath.feature.1.meta.html
INFO - Generating feature detail report [Integer addition]..
INFO - Feature detail report generated: target/report/target-scala-2.11-test-classes-gwen-sample-BasicMath.feature.html
INFO - Closing environment context
INFO - Found FeatureUnit(target/scala-2.11/test-classes/gwen/sample/MetaMath.feature,List(target/scala-2.11/test-classes/gwen/sample/Math.meta))
INFO - Initialising environment context
INFO - MathEnvContext initialised
INFO - Loading meta feature: target/scala-2.11/test-classes/gwen/sample/Math.meta
INFO - Interpreting feature file: target/scala-2.11/test-classes/gwen/sample/Math.meta
INFO - Evaluating feature: Math functions
INFO - Loading StepDef: ++x
INFO - Loaded FeatureSpec: Math functions
INFO - Feature file interpreted: target/scala-2.11/test-classes/gwen/sample/Math.meta

   Feature: Math functions # Loaded

  @StepDef
  Scenario: ++x # Loaded
      Given y = 1 # Loaded
       When z = x + y # Loaded
       Then x = z # Loaded

INFO - Loaded meta feature: Math functions
INFO - Interpreting feature file: target/scala-2.11/test-classes/gwen/sample/MetaMath.feature
INFO - Evaluating feature: Increment integer
INFO - Evaluating Scenario: Incrementing 1 should yield 2
INFO - Evaluating Step: Given x = 1
INFO - Binding 'x = 1' to 'default' vars scope
INFO - [0.0002 secs] Passed Step: Given x = 1
INFO - Evaluating Step: When ++x
INFO - Evaluating StepDef: ++x
INFO - Evaluating Step: Given y = 1
INFO - Binding 'y = 1' to 'default' vars scope
INFO - [0.0001 secs] Passed Step: Given y = 1
INFO - Evaluating Step: When z = x + y
INFO - Found 'x = 1' in 'default' vars scope
INFO - Found 'y = 1' in 'default' vars scope
INFO - evaluating z = 1 + 1
INFO - Binding 'z = 2' to 'default' vars scope
INFO - [0.0005 secs] Passed Step: When z = x + y
INFO - Evaluating Step: Then x = z
INFO - Found 'z = 2' in 'default' vars scope
INFO - Binding 'x = 2' to 'default' vars scope
INFO - [0.0003 secs] Passed Step: Then x = z
INFO - StepDef evaluated: ++x
INFO - [0.0009 secs] Passed Step: When ++x
INFO - Evaluating Step: Then x == 2
INFO - Found 'x = 2' in 'default' vars scope
INFO - [0.0004 secs] Passed Step: Then x == 2
INFO - [0.0014 secs] Passed Scenario: Incrementing 1 should yield 2
INFO - [0.0014 secs] Passed FeatureSpec: Increment integer
INFO - Feature file interpreted: target/scala-2.11/test-classes/gwen/sample/MetaMath.feature

   Feature: Increment integer # [0.0014 secs] Passed

  Scenario: Incrementing 1 should yield 2 # [0.0014 secs] Passed
      Given x = 1 # [0.0002 secs] Passed
       When ++x # [0.0009 secs] Passed
       Then x == 2 # [0.0004 secs] Passed

INFO - Generating meta detail report [Math functions]..
INFO - Meta detail report generated: target/report/target-scala-2.11-test-classes-gwen-sample-MetaMath.feature.1.meta.html
INFO - Generating feature detail report [Increment integer]..
INFO - Feature detail report generated: target/report/target-scala-2.11-test-classes-gwen-sample-MetaMath.feature.html
INFO - Closing environment context
INFO - Generating feature summary report..
INFO - Feature summary report generated: target/report/feature-summary.html

2 features: Passed 2, Failed 0, Skipped 0, Pending 0, Loaded 0 
2 scenarios: Passed 2, Failed 0, Skipped 0, Pending 0, Loaded 0
7 steps: Passed 7, Failed 0, Skipped 0, Pending 0, Loaded 0

[0.0639 secs] Passed
```  

### Serial execution
  
This exact same evaluation performed by the unit test above can also be 
launched directly on the interpreter in serial batch execution mode as 
follows:

```
scala gwen.sample.MathInterpreter -b -r target/report src/test/resources/gwen/sample
```

Or in the sbt console:

```
test:run -b -r target/report src/test/resources/gwen/sample
```

### Parallel execution

To launch the same evaluation again but this time with the two feature files 
running in parallel, invoke the intepreter as follows. This will evaluate the 
two features at the same time on different cores and merge their reports. 

```
scala gwen.sample.MathInterpreter --parallel -b -r target/report src/test/resources/gwen/sample/BasicMath.feature src/test/resources/gwen/sample/MetaMath.feature
```

Or in the sbt console:

```
test:run --parallel -b -r target/report src/test/resources/gwen/sample/BasicMath.feature src/test/resources/gwen/sample/MetaMath.feature
```

### Evaluation reports

Be sure to look at the HTML evaluation report that is generated in the 
target/report directory specified with the -r option. Included in there will 
be a summary report named feature-summary.html with links to both the 
feature and meta detail reports.
  
### Command line options

To see all the available interpreter options, launch the interpreter with the 
--help option as follows:

```
scala gwen.sample.MathInterpreter --help
```

Or in the sbt console:

```
test:run --help
```

All the available options will be printed to the console as shown: 

```
   __ ___      _____ _ __     _    
  / _` \ \ /\ / / _ \ '_ \   { \," 
 | (_| |\ V  V /  __/ | | | {_`/   
  \__, | \_/\_/ \___|_| |_|   `    
  |___/                            

Welcome to Gwen! 

Usage: scala gwen.sample.MathInterpreter [options] [<feature files and/or directories>]

  --version
        Prints the implementation version
  --help
        Prints this usage text
  -b | --batch
        Batch/server mode
  -| | --parallel
        Parallel execution mode
  -p <properties file> | --properties <properties file>
        <properties file> = User properties file
  -r <report directory> | --report <report directory>
        <report directory> Evaluation report output directory
  -t <include/exclude tags> | --tags <include/exclude tags>
        <include/exclude tags> = Comma separated list of tags to @include or ~@exclude
  -m <meta file> | --meta <meta file>
        <meta file> = Path to meta file
  <feature files and/or directories>
        Space separated list of feature files and/or directories      
```