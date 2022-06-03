/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_maximum;

import java.io.IOException;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 *
 * @author imahori
 */
public class Solver_maximize {
    public static double [] milp(MyNode[] pu, MyNode[] pro, MyEdge[] edges, 
            double total_sw, double total_disp, double total_std, double total_per, 
            double total_disp_per, double total_std_per, int it, int N)throws IOException {
        // return
        double[] array = new double[6];
        try{   
            //System.out.println("solver.java ------------------------------------");
            // parameter
            double avg_uti = 0;
            double std_uti = 0;
            double disp_uti = 0;
            double avg_per = 0;
            double std_per = 0;
            double disp_per = 0;
            double time_per = 0;
            
            // 制約条件を設定するための配列
            double[] arr_1 = new double[edges.length + 1]; // sellerは全余剰電力を他者に販売する
            double[] arr_2 = new double[edges.length + 1]; // buyerは全不足電力を他者から買取する
            double[] arr_3 = new double[edges.length + 1]; // 取引量の制限

            LpSolve solver = LpSolve.makeLp(0, (edges.length)); // 変数(枝の長さ)個"取引+目的関数",制約0個
            double[] objectivefunction = new double[edges.length + 1]; // edgeの長さ+1 (objectfunctionの最初の要素は無視されるので)

            for(int i = 0; i < edges.length + 1; i++){ // objectivefunctionの係数を初期化
                objectivefunction[i] = 0.0;
            }

            // objective function の係数設定
            // social welfareの計算
            for(int s = 0; s < edges.length; s++){ // edgeのラベル回す
                //System.out.println("edgesのlabel : " + s);
                MyNode initial = edges[s].ini; // 始点を代入
                MyNode terminal = edges[s].ter; // 終点を代入
                if(terminal == pu[1]){ // buyerが電力会社のとき
                    objectivefunction[s+1] = 0.0; //sellerの効用は0 
                    //System.out.println("a: " + objectivefunction[s+1]);
                }
                else{ // buyerがプロシューマのとき
                    if(initial == pu[0]){ // 始点が電力会社のとき
                        objectivefunction[s+1] = 0.0; // buyerの効用は0
                        //System.out.println("b: " + objectivefunction[s+1]);
                    }
                    else{ // 始点がプロシューマのとき
                        if(terminal == pu[0]){ // 終点が電力会社のとき
                            objectivefunction[s+1] = 0.0; // sellerの効用は0
                            //System.out.println("c: " + objectivefunction[s+1]);
                        }
                        else{ // 終点ｇがプロシューマのとき
                            double s_uti = initial.off - pu[1].cha; // sellerの効用
                            double b_uti = pu[0].off - initial.off; // buyerの効用
                            //System.out.println("initial_off : " + initial.off);
                            //System.out.println("pu_off : " + pu[0].off);
                            //System.out.println("pu_cha : " + pu[1].cha);
                            objectivefunction[s+1] = (s_uti + b_uti); // 上記の総和
                            //System.out.println("d: " + objectivefunction[s+1]);
                        }
                    }
                }
            }

          
            // set objective function
            solver.setObjFn(objectivefunction);

            // 制約条件①：目的関数の取り得る値は0以上に設定
            solver.setBounds((edges.length), 0, Double.POSITIVE_INFINITY);

            // 制約条件②：sellerは全余剰電力を他者に販売する
            //System.out.println("n :" + N); // prosumerのnode数
            for(int i = 0; i < N; i++){ // prosumerのnodeを回す
                //System.out.println("i :" + i); // 今のnode番号
                for(int r = 0; r < arr_1.length; r++){
                    arr_1[r] = 0.0;
                }
                MyNode target = pro[i]; // target nodeに選択したprosumer nodeを代入
                if(target.sb == 0){ // target nodeがsellerの場合
                    for(int j = 0; j < edges.length; j++){ // edgeを回す
                        if(edges[j].ini == target){ // edgeの始点がtarget nodeだったら
                            arr_1[j+1] = 1.0; // arr_1の係数を1.0にする
                            //System.out.println("arr_1[" + (j+1) + "] :" + arr_1[j+1]);
                        }
                    } // edge全部見た
                    solver.addConstraint(arr_1, LpSolve.EQ, pro[i].cap); // 制約条件を追加
                }
                for(int o = 0; o < edges.length + 1; o++){ // arr_1を初期化する
                    arr_1[o] = 0.0;
                }
            }


            // 制約条件③：buyerは全不足電力を他者から買取する
            //System.out.println("n :" + N); 
            for(int i = 0; i < N; i++){ // prosumerのnodeを回す
                for(int r = 0; r < arr_2.length; r++){
                    arr_2[r] = 0.0;
                }
                //System.out.println("i :" + i); // 今のnode番号
                MyNode target = pro[i]; // target nodeに選択したprosumer nodeを代入
                if(target.sb == 1){ // target nodeがbuyerの場合
                    for(int j = 0; j < edges.length; j++){ // edgeを回す
                        if(edges[j].ter == target){ // edgeの終点がtarget nodeだったら
                            arr_2[j+1] = 1.0; // arr_2の係数を1.0にする
                            //System.out.println("arr_2[" + (j+1) + "] :" + arr_2[j+1]);
                        }
                    } // edge全部見た
                    solver.addConstraint(arr_2, LpSolve.EQ, pro[i].dem); // 制約条件を追加
                }
                for(int o = 0; o < edges.length + 1; o++){ // arr_2を初期化する
                    arr_2[o] = 0.0;
                }
            }

            // 制約条件④：取引量の制限 (各取引はprosumerのcap or demandを設定する)
            double strict = 0;
            for(int r = 0; r < arr_3.length; r++){
                    arr_3[r] = 0.0;
            }
            for(int i = 0; i < edges.length; i++){
                arr_3[i+1] = 1.0;
                strict = Math.min(edges[i].ini.cap, edges[i].ter.dem);
                solver.addConstraint(arr_3, LpSolve.LE, strict);
                //System.out.println("strict : " + strict);
                // initialize arr_4
                arr_3[i+1] = 0.0;
            }

            //System.out.println("get solution");

            // 最大化
            solver.setMaxim();
            solver.solve();

            double sw = 0; // 目的関数Get
            sw = solver.getObjective();
            //System.out.println("sw :" + sw);

            // print solution
            double[] var = solver.getPtrVariables();
            for(int q = 0; q < edges.length; q++){
                //System.out.println("Value of var[" + q + "] = " + var[q]);
                edges[q].x = var[q];
            }

        // charge.bsgraph(graph, pu, pro, edges, N, var);
        // total prosumers' utility

        // caluculate prosumers' benefit　ここ？
        for(int q = 0; q < edges.length; q++){
            if(edges[q].ini != pu[0]){ // seller
                if(edges[q].ter != pu[1]){
                    edges[q].ini.uti += (edges[q].ini.off - pu[1].cha) * edges[q].x;
                    edges[q].ini.total_benefit += (edges[q].ini.off - pu[1].cha) * edges[q].x;
                    edges[q].ter.uti += (pu[0].off - edges[q].ini.off) * edges[q].x;
                    edges[q].ter.total_benefit += (pu[0].off - edges[q].ini.off) * edges[q].x;
                    //sw += (edges[q].ini.off - pu[1].cha) * edges[q].x + (pu[0].off - edges[q].ini.off) * edges[q].x;                       // check
                }
            }   
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
       // for(int h = 0; h < N; h++){
        //    
        //    System.out.println("pro[" + h + "].total_per : " + pro[h].total_per);
        //}
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
        
        // return
        array[0] = total_sw;
        array[1] = total_disp;
        array[2] = total_std;
        array[3] = total_per;
        array[4] = total_disp_per;
        array[5] = total_std_per;
        
        // calculator.calc(pu, pro, edges, sw, total_sw, total_disp, total_std, total_per, total_disp_per, total_std_per, d, it, N);
        //CSV.makeCSV(var, pu, pro, edges, N, sw, avg_uti, std_uti, disp_uti, avg_per, std_per, disp_per, it);

        }
        catch(LpSolveException e){
            e.printStackTrace();
        } 
        return array;
    }    
}
