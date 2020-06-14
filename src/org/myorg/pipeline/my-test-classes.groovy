class Example {
   static void main(String[] args) {
      Student mst = new Student();
      mst.name = "Joe";
      mst.ID = 1;
      println(mst.Display())
   } 
} 
 
public class Student {
   String name;
   int ID;
	
   String Display() {
      return name +ID;
   }  
}