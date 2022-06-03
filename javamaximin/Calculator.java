/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_maximin;

import lpsolve.LpSolveException;

/**
 *
 * @author imahori
 */
public class Calculator {
    public static void calc(MyNode[] pu, MyNode[] pro, MyEdge[] edges, double sw,
            double total_sw, double total_disp, double total_std, double total_per, 
            double total_disp_per, double total_std_per, int d, int it, int N){
        //try{
            // calculate avarage of social welfare
            double avg_uti = 0;
            avg_uti = sw/N;

            // calculate standard deviation and dispersion of benefit
            double std_uti = 0; // 標準偏差
            double disp_uti = 0; // 分散
            for(int v = 0; v < N; v++){
                // pow
                disp_uti += Math.pow((pro[v].uti - avg_uti), 2);
            }
            disp_uti = disp_uti/N;
            // square root
            std_uti = Math.sqrt(disp_uti);

            total_sw += sw;
            total_disp += disp_uti;
            total_std += std_uti;

            for(int e = 0; e < N; e++){
                pro[e].per = pro[e].uti/pro[e].max_uti;
                total_per += pro[e].per;
                pro[e].total_per += pro[e].per;
            }

            // calculate standard deviation and dispersion of percentage
            double avg_per = 0;
            double std_per = 0;
            double disp_per = 0;

            avg_per = total_per/N;

            for(int x = 0; x < N; x++){    
                // pow
                disp_per += Math.pow((pro[x].per - avg_per), 2);
            }
            disp_per = disp_per/N;
            std_per = Math.sqrt(disp_per);

            total_disp_per += disp_per; // time 1 と time 2 の合計 ％の分散
            total_std_per += std_per; // time 1 と time 2 の合計 ％の標準偏差
           // System.out.println("total_sw :  " + total_sw);
        }
    
        /*catch(LpSolveException e){
            e.printStackTrace();
        }*/
    //}
}
