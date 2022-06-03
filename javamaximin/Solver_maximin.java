/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_maximin;

import java.io.IOException;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import lpsolve.*;
import static p2p_maximin.Graph.array;
/**
 *
 * @author imahori
 */
public class Solver_maximin {
    public static double [] milp(MyNode[] pu, MyNode[] pro, MyEdge[] edges, 
            double total_sw, double total_disp, double total_std, double total_per, 
            double total_disp_per, double total_std_per, int it, int N, double counter)throws IOException {
        // return
        double[] array = new double[8];
        try{
            //System.out.println("start solver -----------------------------------");
            //System.out.println("solver.java ------------------------------------");
            // parameter
            double avg_uti = 0;
            double std_uti = 0;
            double disp_uti = 0;
            double avg_per = 0;
            double std_per = 0;
            double disp_per = 0;
            double time_per = 0;
            double sw_counter = 0;
            // array for constraints　(already initialized)
            double[] arr_1 = new double[edges.length + 2]; // 各消費者の利得はz（消費者の利得の最小値）以上
            double[] arr_2 = new double[edges.length + 2]; // sellerは全余剰電力を他者に販売する
            double[] arr_3 = new double[edges.length + 2]; // buyerは全不足電力を他者から買取する
            double[] arr_4 = new double[edges.length + 2]; // 取引量の制限あり
            
            // the # of constraints, edges + minimum consumers' benefit
            LpSolve solver = LpSolve.makeLp(0, (edges.length + 1)); 
            // the length of list edges + 2 (cuz it ignores the first element of objective function)
            // aleready initilized coefficients of objective function
            double[] obj_fn = new double[edges.length + 2];
            //System.out.println("obj_fn : " + obj_fn.length);
            obj_fn[edges.length + 1] = 1.0;
            solver.setObjFn(obj_fn);
            // set bound 
            solver.setBounds((edges.length + 1), 0, Double.POSITIVE_INFINITY);
            // set objective function
            // set coefficient of objective function
            // Constraint 1： each prosumer's benefit must be more than z
            for(int s = 0; s < N; s++){
                // initialize array_1
                for(int r = 0; r < arr_1.length; r++){
                    arr_1[r] = 0.0;
                }
                // if prosumer is seller
                if(pro[s].sb == 0){
                    for(MyEdge edge: edges){
                        if(edge.ini == pro[s]){
                            if(edge.ter != pu[1]){
                                arr_1[edge.label+1] = (pro[s].off - pu[1].cha);
                            }
                        }
                    }
                }
                // if prosumer is buyer
                else if(pro[s].sb == 1){
                    for(MyEdge edge: edges){
                        if(edge.ter == pro[s]){
                            if(edge.ini != pu[0]){
                                arr_1[edge.label+1] = (pu[0].off - pro[s].off);
                            }
                        }
                    }
                }
                arr_1[arr_1.length-1] = -1;
                // System.out.println("array_1 : ");
                solver.addConstraint(arr_1, LpSolve.GE, 0); 
            }
  
            // Constaraint 2 ：seller must sell own surplus energy
            for(int i = 0; i < N; i++){ // prosumerのnodeを回す
                for(int r = 0; r < arr_2.length; r++){
                    arr_2[r] = 0.0;
                }
                MyNode target = pro[i]; // target nodeに選択したprosumer nodeを代入
                if(target.sb == 0){ // target nodeがsellerの場合
                    for(int j = 0; j < edges.length; j++){ // edgeを回す
                        if(edges[j].ini == target){ // edgeの始点がtarget nodeだったら
                            arr_2[j+1] = 1.0; // arr_2の係数を1.0にする
                        }
                    } // edge全部見た  
                    solver.addConstraint(arr_2, LpSolve.EQ, pro[i].cap); // 制約条件を追加
                }
                for(int o = 0; o < edges.length + 2; o++){ // arr_1を初期化する
                    arr_2[o] = 0.0;
                }
            }

            // Constaraint 3 ：buyer must purchase own shortage electricity from seller
            for(int i = 0; i < N; i++){ // prosumerのnodeを回す
                for(int r = 0; r < arr_3.length; r++){
                    arr_3[r] = 0.0;
                }
                MyNode target = pro[i]; // target nodeに選択したprosumer nodeを代入
                if(target.sb == 1){ // target nodeがbuyerの場合
                    for(int j = 0; j < edges.length; j++){ // edgeを回す
                        if(edges[j].ter == target){ // edgeの終点がtarget nodeだったら
                            arr_3[j+1] = 1.0; // arr_2の係数を1.0にする
                        }
                    } // edge全部見た
                    solver.addConstraint(arr_3, LpSolve.EQ, pro[i].dem); // 制約条件を追加
                }
                for(int o = 0; o < edges.length + 2; o++){ // arr_2を初期化する
                    arr_3[o] = 0.0;
                }
            }

            // Conastaraint 4 :each trading must be less than min(initial capacity, terminal demand)
            double strict = 0;
            for(int r = 0; r < arr_4.length; r++){
                arr_4[r] = 0.0;
            }
            for(int i = 0; i < edges.length; i++){
                arr_4[i+1] = 1.0;
                strict = Math.min(edges[i].ini.cap, edges[i].ter.dem);
                solver.addConstraint(arr_4, LpSolve.LE, strict);
                // initialize arr_4
                arr_4[i+1] = 0.0;
            }

        //System.out.println("get solution");
        // 最大化
        solver.setMaxim();
        // Solve problem
        solver.solve();
        // get objective function
        double min;
        min = solver.getObjective();
        //System.out.println("objective function :" + min);
        // print solution
        double[] var = solver.getPtrVariables();
        for(int q = 0; q < edges.length; q++){
            //System.out.println("Value of var[" + q + "] = " + var[q]);
            edges[q].x = var[q];
        }

        // charge.bsgraph(graph, pu, pro, edges, N, var);
        // total prosumers' utility
        double sw = 0;

        // caluculate prosumers' benefit　ここ？
        for(int q = 0; q < edges.length; q++){
            if(edges[q].ini != pu[0]){ // seller
                if(edges[q].ter != pu[1]){
                    edges[q].ini.uti += (edges[q].ini.off - pu[1].cha) * edges[q].x;
                    edges[q].ini.total_benefit += (edges[q].ini.off - pu[1].cha) * edges[q].x;
                    edges[q].ter.uti += (pu[0].off - edges[q].ini.off) * edges[q].x;
                    edges[q].ter.total_benefit += (pu[0].off - edges[q].ini.off) * edges[q].x;
                    sw += (edges[q].ini.off - pu[1].cha) * edges[q].x + (pu[0].off - edges[q].ini.off) * edges[q].x;
                }
            }   
        }
        //System.out.println(">> >> >> >> >> >> >> >> sw : " + sw);
        
        if(sw == 0){
            //System.out.println("sw = 0 ::  :::: ::: ::");
            array[0] = total_sw;
            array[1] = total_disp;
            array[2] = total_std;
            array[3] = total_per;
            array[4] = total_disp_per;
            array[5] = total_std_per;
            array[6] = counter;
            array[7] = 1;
            CSV.makeCSV(var, pu, pro, edges, N, sw, avg_uti, std_uti, disp_uti, avg_per, std_per, disp_per, it);
            return array;
        }
        
        // delete the problem and free memory
        solver.deleteLp();
        
        // average of prosumers' benefit
        //System.out.println("SW : " + sw);
        total_sw += sw;
        avg_uti = sw/N;
        //System.out.println("avg_uti : " + avg_uti);
        // dispersion of prosumers' benefit
        for(int v = 0; v < N; v++){
            disp_uti += Math.pow((pro[v].uti - avg_uti) , 2);
        }
        disp_uti = disp_uti/N;
        //System.out.println("disp_uti" + disp_uti);
        // standard deviation of prosumers' benefit
        std_uti = Math.sqrt(disp_uti);
        
        // total of each parameter for one day
        
        total_disp += disp_uti;
        total_std += std_uti;
        
        // % 
        for(int h = 0; h < N; h++){
            if(pro[h].max_uti == 0){
                pro[h].per = 0;
            }
            else{
                pro[h].per = pro[h].uti/pro[h].max_uti;
            }
            //System.out.println("uti : " + pro[h].uti);
            //System.out.println("max : " + pro[h].max_uti);
            //System.out.println("pro percent : " + pro[h].per);
            total_per += pro[h].per;
            time_per += pro[h].per;
            pro[h].total_per += pro[h].per;
            
        }
        //System.out.println("total_per : " + total_per);
        //for(int h = 0; h < N; h++){
            
            //System.out.println("pro[" + h + "].total_per : " + pro[h].total_per);
        ///}
        // average of %
        avg_per = time_per/N;

        // dispersion of %      
        for(int v = 0; v < N; v++){
            disp_per += Math.pow((pro[v].per - avg_per) , 2);
        }
        disp_per = disp_per/N;
        
        // standard deviation of %
        std_per = Math.sqrt(disp_per);
        
        // total of each parameter for one day
        total_disp_per += disp_per;
        total_std_per += std_per;
        counter = counter + 1.0;
        // return
        array[0] = total_sw;
        array[1] = total_disp;
        array[2] = total_std;
        array[3] = total_per;
        array[4] = total_disp_per;
        array[5] = total_std_per;
        array[6] = counter;
        array[7] = sw_counter;
        
        
            // calculator.calc(pu, pro, edges, sw, total_sw, total_disp, total_std, total_per, total_disp_per, total_std_per, d, it, N);
            //CSV.makeCSV(var, pu, pro, edges, N, sw, avg_uti, std_uti, disp_uti, avg_per, std_per, disp_per, it);
        
        }
        catch(LpSolveException e){
            e.printStackTrace();
        }
        
        //System.out.println("finish solver ----------------------------------");
        return array;
    }
}
