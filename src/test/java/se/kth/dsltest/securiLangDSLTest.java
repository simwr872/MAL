package se.kth.dsltest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import se.kth.mal.Master;

public class securiLangDSLTest {
   @Rule
   public TemporaryFolder tmpFolder = new TemporaryFolder();

   @Test
   public void test() {

      try {
         new Master("./src/test/resources/securiLang.slng", tmpFolder.newFolder("java").getPath(), "auto", false, null);
      }
      catch (

      Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
}
