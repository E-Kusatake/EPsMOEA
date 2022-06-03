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
public class CSV {
    public static void makeCSV(double[] var, MyNode[] pu, MyNode[] pro, MyEdge[] edges, int N, double sw,
            double avg_uti, double std_uti, double disp_uti, double avg_per, double std_per, double disp_per, int it) throws IOException{
        //出力先を作成する
        //System.out.println("CSV");
        //FileWriter fw = new FileWriter("/home/imahori/NetBeansProjects/p2p_maximum/test/p2pmaximum_time" + (it) + "_.csv", false);  //※１
        FileWriter fw = new FileWriter("/home/imahori/NetBeansProjects/results/p2pmaximum_time" + (it) + "_.csv", false);
        PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
        
        pw.print("# of Prosumers");
        pw.print(",");
        pw.print(N);
        pw.println();

        pw.print("time");
        pw.print(",");
        pw.print(it);
        pw.println();
    
        pw.print("social welfare");
        pw.print(",");
        pw.print(sw);
        pw.println();
        
        pw.print("avg of sw");
        pw.print(",");
        pw.print(avg_uti);
        pw.println();
        
        pw.print("disp of benefit");
        pw.print(",");
        pw.print(disp_uti);
        pw.println();
        
        pw.print("std of benefit");
        pw.print(",");
        pw.print(std_uti);
        pw.println();
        
        pw.print("avg og per");
        pw.print(",");
        pw.print(avg_per);
        pw.println();
        
        pw.print("disp of per");
        pw.print(",");
        pw.print(disp_per);
        pw.println();
        
        pw.print("std of per");
        pw.print(",");
        pw.print(std_per);
        pw.println();
        
        pw.println();
           
        pw.print("prosumers");
        pw.print(",");
        pw.print("generation");
        pw.print(",");
        pw.print("consumption");
        pw.print(",");
        pw.print("capacity");
        pw.print(",");
        pw.print("demand");
        pw.print(",");
        pw.print("utility");
        pw.println();

        for(int t = 0; t < N; t++){
            pw.print((t));
            pw.print(",");
            pw.print((pro[t].gen));
            pw.print(",");
            pw.print(pro[t].con);
            pw.print(",");
            pw.print((pro[t].cap));
            pw.print(",");
            pw.print((pro[t].dem));
            pw.print(",");
            pw.print((pro[t].uti));
            pw.print(",");
            pw.println();
        }
        
        pw.println();
        
        pw.print("initial");
        pw.print(",");
        pw.print("terminal");
        pw.print(",");
        pw.print("volume");
        pw.println();
        
        for(int u = 0; u < edges.length; u++){
            pw.print(edges[u].ini.label);
            pw.print(",");
            pw.print(edges[u].ter.label);
            pw.print(",");
            pw.print(var[u]);
            pw.println();
        }
          pw.close();
    }
}
