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
public class DayCSV {
    public static void dayCSV(MyNode[] pu, MyNode[] pro, int N, double sw, double std, double disp,
            double total_per, double total_std_per, double total_disp_per, int it) throws IOException{
        //出力先を作成する
        //System.out.println("CSV");
        // FileWriter fw = new FileWriter("/home/imahori/NetBeansProjects/p2p_maximum/test/p2pmaximum_fin.csv", false);  //※１
        FileWriter fw = new FileWriter("/home/imahori/NetBeansProjects/results/p2pmaximum_fin.csv", false); 
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));

        // Difinision
        double ave_sw = 0, ave_disp = 0, ave_std = 0, ave_per =0, ave_disp_per = 0, ave_std_per = 0;
        
        // write to file
        pw.print("for one day");
        pw.println(" ");
       
        pw.print("pro[ i ]");
        pw.print(",");
        pw.print("obtained benefit");
        pw.print(",");
        pw.print("maximum benefit");
        pw.print(",");
        pw.print("percentage");
        pw.println(" ");
        for(int i = 0; i < N; i++){
            pw.print(pro[i].label);
            pw.print(",");
            pw.print(pro[i].total_benefit);
            pw.print(",");
            pw.print(pro[i].total_max_benefit);
            pw.print(",");
            pw.print(pro[i].total_benefit/pro[i].total_max_benefit);
            pw.print(",");
            pw.println();
        }
        pw.println();
        
        pw.print("time");
        pw.print(",");
        //pw.print(time);
        pw.print(it);
        pw.println();
    
        pw.print("social welfare");
        pw.print(",");
        pw.print(sw);
        pw.println();
        
        pw.print("avg of sw");
        pw.print(",");
        //ave_sw = sw/time;
        ave_sw = sw/it;
        pw.print(ave_sw);
        pw.println();
        
        pw.print("avg of disp of benefit");
        pw.print(",");
        //ave_disp = disp/time;
        ave_disp = disp/it;
        pw.print(ave_disp);
        pw.println();
        
        pw.print("avg of std of benefit");
        pw.print(",");
        //ave_std = std/time;
        ave_std = std/it;
        pw.print(ave_std);
        pw.println();
        
        pw.print("avg of per");
        pw.print(",");
        //ave_per = total_per/time;
        ave_per = total_per/(it*N);
        pw.print(ave_per);
        pw.println();
        
        pw.print("avg of disp of per");
        pw.print(",");
        //ave_disp_per = total_disp_per/time;
        ave_disp_per = total_disp_per/it;
        pw.print(ave_disp_per);
        pw.println();
        
        pw.print("avg of std of per");
        pw.print(",");
        //ave_std_per = total_std_per/time;
        ave_std_per = total_std_per/it;
        pw.print(ave_std_per);
        pw.println();
        
        pw.println();
        
        pw.close();
    }
}
