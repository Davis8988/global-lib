import org.myorg.pipeline.Student

class StudentTest extends GroovyTestCase {
   void testDisplay() {
      def stud = new Student(name : 'Joe', ID : '1')
      def expected = 'Joe11'
	  print "Testing expected output"
      assertToString(stud.Display(), expected)
   }
}