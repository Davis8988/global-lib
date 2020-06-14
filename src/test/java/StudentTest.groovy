import org.junit.Test
import org.myorg.pipeline.Student
import static org.junit.Assert.assertEquals;



import static groovy.test.GroovyAssert.shouldFail


class StudentTest extends GroovyTestCase {
   void testStringComp() {
		print "Testing testStringComp()"
		assertToString("hello", "hello")
   }
   
   void testDisplay() {
		print "Testing display()"
		def stud = new Student(name : 'Joe', ID : '1')
		def expected = 'SomethingElse'
		print "Testing expected output"
		assertToString(stud.Display(), expected)
   }
   
   void testIndexOutOfBoundsAccess() {
	   print "Testing indexOutOfBoundsAccess()"
       def numbers = [1,2,3,4]
       shouldFail {
           numbers.get(4)
       }
   }
   
   void testRandomTest() {
		def sieve = (0..10).toList()
		GSieve.filter(sieve); // [1,2,3,5,7]
		assertEquals("Count of primes in 1..10 not correct", 5, (sieve.findAll {it -> it != 0}).size());
   }
   
}