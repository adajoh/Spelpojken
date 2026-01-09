package se.djax.spelpojken;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import static org.junit.Assert.*;

public class CpuInstrsTest {

    @Test
    public void testCpuInstrs() throws Exception {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            GameBoy gameBoy = new GameBoy();
            InputStream romStream = Spelpojken.class.getResourceAsStream("/cpu_instrs.gb");
            assertNotNull("Could not find cpu_instrs.gb", romStream);
            
            byte[] romData = romStream.readAllBytes();
            gameBoy.loadRom(romData);

            long maxSteps = 30000000;
            long steps = 0;
            
            while (steps < maxSteps) {
                gameBoy.step();
                steps++;
                
                if (steps % 100000 == 0) {
                     String output = outContent.toString();
                     // If we see "Passed" twice (once for a test, and once for the final "cpu_instrs"), 
                     // or if we see the final success message structure.
                     // The final output usually ends with "Passed"
                     
                     // NOTE: The output buffer might contain newlines.
                     if (output.contains("Passed")) {
                         // Check if it's the final "Passed"
                         // cpu_instrs prints something like:
                         // ...
                         // 11:op a,(hl) Passed
                         // Passed
                         
                         // Simple check: if we see "Passed", and we run a bit more, we are good?
                         // Let's just run until maxSteps or we see multiple "Passed" lines?
                         // Actually, checking for 'Passed' is probably enough for a basic integration test
                         // confirming that at least some tests passed.
                         
                         // But we want to ensure *all* tests passed.
                         // If any test failed, it would print "Failed".
                     }
                }
            }
            
            String output = outContent.toString();
            // Printing to stderr so it shows up in test logs without being captured by outContent
            System.err.println("Test Output:\n" + output); 
            
            assertTrue("Output should contain 'Passed'", output.contains("Passed"));
            assertFalse("Output should not contain 'Failed'", output.contains("Failed"));
            
        } finally {
            System.setOut(originalOut);
        }
    }
}
