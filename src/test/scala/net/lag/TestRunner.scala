package net.lag

import scala.testing.SUnit._

import net.lag.configgy._
import net.lag.logging._


object TestRunner {
    def sum(x: Seq[Int]) = (0 /: x) { _ + _ }

    def main(args:Array[String]): Unit = {
        val results = new TestResult

        val suite = new TestSuite(ConfiggyExtensionsTests,
                                  AttributesTests,
                                  ConfigParserTests,
                                  ConfigTests,
                                  LoggingTests,
                                  ConfiggyTests)

        val testCount = sum(for (t <- suite.buf) yield t.asInstanceOf[sorg.testing.Tests].tests.length)
        Console.println("Running " + testCount + " unit tests:")
        suite.run(results)

        if (! results.failures.hasNext) {
            Console.println("Success!");
        } else {
            Console.println
            Console println("FAILED TESTS (" + results.failureCount + "):");
            Console.println
            for (val each:TestFailure <- results.failures) {
                Console.println(each.toString + ":")
                for (val line <- each.trace.split("\n")) {
                    Console.println("    " + line)
                }
                Console.println
            }
            System.exit(1)
        }
    }

}
