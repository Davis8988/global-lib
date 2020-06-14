
import org.myorg.pipeline.Student
import test.java.StudentTest
import groovy.util.GroovyTestSuite 
import junit.framework.Test 
import junit.textui.TestRunner 

class AllTests { 
	
   static Test testSuite() { 
      def allTestsArr = new GroovyTestSuite() 
      allTestsArr.addTestSuite(StudentTest.class) 
      //allTestsArr.addTestSuite(EmployeeTest.class) 
      return allTestsArr 
   } 
} 