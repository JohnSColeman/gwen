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

package gwen.report.html

import gwen.Predefs.Kestrel
import gwen.report.ReportGenerator
import java.io.File
import scala.reflect.io.Path

/**
 * Generates a HTML evaluation report. The report includes a feature
 * summary and all evaluation features details.
 * 
 * @author Branko Juric
 */
class HtmlReportGenerator(val targetDir: File, val interpreterName: String) 
  extends ReportGenerator(targetDir, interpreterName, "feature-summary", "html") 
  with HtmlReportFormatter {

  // delete any previously generated reports
  if (targetDir.exists()) {
    targetDir.listFiles().filter { file => 
      val name = file.getName
      name.endsWith(".feature.html") || name.endsWith(".meta.html") || name == "feature-summary.html" || name =="resources"
    } foreach { file =>
        if (file.isDirectory) {
          deleteDir(file)
        } else {
          file.delete()
        }
    }
  }
  
  // copy in CSS files (if they don't already exist)
  new File(Path(new File(reportDir, "resources/css")).createDirectory().path) tap { dir =>
    copyClasspathTextResourceToFile("/gwen/report/html/css/gwen.css", dir)
    copyClasspathTextResourceToFile("/gwen/report/html/css/bootstrap.min.css", dir)
  }
  
  // copy in JS files (if they don't already exist)
  new File(Path(new File(reportDir, "resources/js")).createDirectory().path) tap { dir =>
    copyClasspathTextResourceToFile("/gwen/report/html/js/jquery-1.11.0.min.js", dir)
    copyClasspathTextResourceToFile("/gwen/report/html/js/bootstrap.min.js", dir)
  }
  
  // copy in image files (if they don't already exist)
  new File(Path(new File(reportDir, "resources/img")).createDirectory().path) tap { dir =>
    copyClasspathBinaryResourceToFile("/gwen/report/html/img/gwen-logo.png", dir)
  }
  
  private def deleteDir(dir: File) {
    dir.listFiles() foreach { file =>
      if (file.isDirectory()) {
    	  deleteDir(file)
      } else {
        file.delete()
      }
    }
    dir.delete()
  }
  
}