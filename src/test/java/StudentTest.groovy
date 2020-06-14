@Library('david')
import org.junit.Test
import org.myorg.pipeline.Student

import static groovy.test.GroovyAssert.shouldFail


class StudentTest extends GroovyTestCase {
   @Test
   void testDisplay() {
		print "Testing display()"
		def stud = new Student(name : 'Joe', ID : '1')
		def expected = 'SomethingElse'
		print "Testing expected output"
		assertToString(stud.Display(), expected)
   }
   
   @Test
   void indexOutOfBoundsAccess() {
	   print "Testing indexOutOfBoundsAccess()"
       def numbers = [1,2,3,4]
       shouldFail {
           numbers.get(4)
       }
   }
   
}