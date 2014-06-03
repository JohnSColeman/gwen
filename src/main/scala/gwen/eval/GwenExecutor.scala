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

package gwen.eval

import scala.Option.option2Iterable

import com.typesafe.scalalogging.slf4j.LazyLogging

import gwen.ConsoleWriter
import gwen.Predefs.Kestrel
import gwen.dsl.EvalStatus
import gwen.dsl.Failed
import gwen.dsl.Pending
import gwen.report.FeatureSummary
import gwen.report.ReportGenerator
import gwen.report.html.HtmlReportGenerator

/**
 * Executes user provided options on the given interpreter.
 * 
 * @param interpreter
 * 			the gwen interpreter to execute on
 */
class GwenExecutor[T <: EnvContext](interpreter: GwenInterpreter[T]) extends LazyLogging with ConsoleWriter {
  
  /**
   * Executes the given options.
   * 
   * @param options
   * 			the command line options
   * @param @param optEnv
   * 			optional environment context (None to have Gwen create an env context for each feature unit, 
   *    		Some(env) to reuse an environment context for all, default is None)
   */
  def execute(options: GwenOptions, optEnv: Option[EnvContext] = None): EvalStatus = {
    val start = System.nanoTime
    try {
      FeatureStream.readAll(options.paths) match {
        case featureStream @ _ #:: _ =>
          val reportGenerator = options.reportDir map { reportDir =>
            new HtmlReportGenerator(reportDir, interpreter.getClass().getSimpleName())
          }
          val summary = {
            if (options.parallel) {
              featureStream.par flatMap (executeFeatureUnits(options, _, reportGenerator, optEnv))
            } else {
              executeFeatureUnits(options, featureStream.flatten, reportGenerator, optEnv)
            }
          }.toList.foldLeft(FeatureSummary()) (_+_)
          reportGenerator foreach { _.reportSummary(summary) }
          EvalStatus(summary.featureResults.map(_.evalStatus)) tap { status =>
            println
            println(summary.toString)
            println
            println(status)
            println
          }
        case _ =>
          options.metaFile foreach { metaFile =>
            optEnv foreach { env =>
              interpreter.loadMeta(List(metaFile), Nil, env) 
            }
          }
          if (!options.paths.isEmpty) {
            logger.info("No features found in specified files and/or directories!")
         }
          Pending
      }
    } catch {
      case e: Throwable =>
        if (options.batch) {
          logger.error(e.getMessage, e)
          Failed(System.nanoTime - start, e)
        } else {
          throw e
        }
    }
  }
  
  /**
   * Executes all feature units in the given stream.
   * 
   * @param options
   * 		the command line options
   * @param featureStream
   * 		the feature stream to execute
   * @param envOpt
   * 		optional environment context (reused across all feature units if provided, 
   *    	otherwise a new context is created for each unit)
   */
  private def executeFeatureUnits(options: GwenOptions, featureStream: Stream[FeatureUnit], reportGenerator: Option[ReportGenerator], envOpt: Option[EnvContext]): Stream[FeatureSummary] = 
    featureStream.flatMap { unit =>
      val env = envOpt.getOrElse(interpreter.initialise(options))
      try {
        if (envOpt.isDefined) { interpreter.reset(env) }
        val metaFiles = unit.metaFiles ++ options.metaFile.toList
        interpreter.interpretFeature(unit.featureFile, metaFiles, options.tags, env).map { spec =>
          FeatureSummary(spec, reportGenerator map { _.reportDetail(spec) })
        }
      } finally {
        if (!envOpt.isDefined) { interpreter.close(env) }
      }
    }
}