@Library('david')
import test.java.StudentTest
import groovy.util.GroovyTestSuite 
import junit.framework.Test 
import junit.textui.TestRunner 

class AllTests { 
   static Test suite() { 
      def allTests = new GroovyTestSuite() 
      allTests.addTestSuite(StudentTest.class) 
      //allTests.addTestSuite(EmployeeTest.class) 
      return allTests 
   } 
} 
print "Executing all tests"
def results = TestRunner.run(AllTests.suite())
print "Results: " + results
print "Results: " + results.wasSuccessful()
print "Error count: " + results.errorCount()
print "Failure count: " + results.failureCount()
print "Erros: "
results.errors().each {err -> print err}
print "Failures: "
results.failures().each {err -> print err}