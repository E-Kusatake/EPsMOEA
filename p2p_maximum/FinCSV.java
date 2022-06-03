 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_maximum;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author imahori
 */
public class FinCSV {
    public static void finalCSV(double all_sw, double all_disp, double all_std, double all_disp_per, double all_std_per, double all_per, int ite) throws IOException{
        //出力先を作成する
        //System.out.println("CSV");
        // FileWriter fw = new FileWriter("/home/imahori/NetBeansProjects/p2p_maximum/N_20_maximum/p2pmaximum_fin.csv", false);  //※１
        FileWriter fw = new FileWriter("/mnt/c/Users/towar/java/p2pmaximum_fin.csv", false); 
        
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
        
        pw.print("final file");
        pw.println();
        pw.println();
        
        pw.print("Avg of SW");
        pw.print(",");
        pw.print(all_sw/ite);
        pw.println();
        
        pw.print("Avg of Disp of benefit");
        pw.print(",");
        pw.print(all_disp/ite);
        pw.println();
        
        pw.print("Avg of SD of benefit");
        pw.print(",");
        pw.print(all_std/ite);
        pw.println();
        
        pw.print("Avg of %");
        pw.print(",");
        pw.print(all_per/ite);
        pw.println();
        
        pw.print("Avg of Disp of %");
        pw.print(",");
        pw.print(all_disp_per/ite);
        pw.println();
        
        pw.print("Avg of SD of %");
        pw.print(",");
        pw.print(all_std_per/ite);
        pw.println();
        
        pw.close();
    }
}