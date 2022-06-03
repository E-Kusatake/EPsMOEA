/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_maximum;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
// import edu.uci.ics.jung.graph.Graph;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Random;
import p2p_maximum.MyNode;
import p2p_maximum.MyEdge;

/**
 *
 * @author imahori
 */
public class Graph {
// solver return
    static double [] array = new double[6];
    static int s_count = 0;                 //seller_count
    static int b_count = 0;                 //buyer_count
    static int sb_count = 0;                //seller&buyer_count
    public static void main(String[] args) throws IOException {                 //possible exceptions
        // System.out.println("graph.java -----------------------------------------");
        long startTime = System.currentTimeMillis();
        // create directed Graph -> Time-Varying Graph
        // Graph<MyNode,MyEdge> Graph = new DirectedSparseGraph<>();
        // set # of prosumers
        int N = 10;
        // set # of iteration
        int ite = 1000;
        // # of time
        int time = 1;
        // offered price by public utility
        double pub_off = 0.02905;
        // purchased price by public utility
        double pub_pur = 0.00805;
        // offered price by prosumers
        double pro_off = 0.01855;
        // create nodes for public utility (pu[0] = seller, pu[1] = buyer)
        MyNode[] pu = new MyNode[2];
        // create nodes for prosumers
        MyNode[] pro = new MyNode[N];
        // create prosumers' node
        for(int a = 0; a < N; a++){
            pro[a] = new MyNode(a, 0.0, 0.0, 0.0, 0.0, 0.0, pro_off, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0);
        }
        // create public utility's node
        // label, 発電量 (s) , 消費量 (b) , 供給量の制限 (s) , 需要量 (b) , 留保価格 (s & b) , 販売価格 (s) , 購入価格 (b) , seller or buyer? (s & b)
        pu[0] = new MyNode(N,   0, 0, 0, 0, 0, pub_off,       0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0); // 電力会社(売り手)の生成 (売却価格30円/kWh)
        pu[1] = new MyNode(N+1, 0, 0, 0, 0, 0,       0, pub_pur, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1); // 電力会社(買い手)の生成 (買取価格5円/kWh)
        // Initialize nodes
        // total of social welfare
        double total_sw = 0;
        // total of dispersion of benefit
        double total_disp = 0;
        // total of standard deviation of benefit
        double total_std = 0;
        // total of percentage
        double total_per = 0;
        // total of dispersion of per
        double total_disp_per = 0;
        // total of standard deviation of per
        double total_std_per = 0;
        // suppliable amount of electricty to prosumers
        double total_cap = 0; 
        // acceptable amount of electricity from prosumers
        double total_dem = 0;
        double total_max_benefit = 0.0;
        double ge_val = 0, ge = 0, co_val = 0, co = 0, ca = 0, de =0;
        // binary variable (0 = seller, 1 = buyer, 9 = nothing)
        int sb;
        // initialized total capacity 
        BigDecimal total_cap_bd; // The assigned value is never used
        // initialized total demand 
        BigDecimal total_dem_bd;
        // start iteration
        for(int it = 0; it < ite; it++){
            //System.out.println("=========================================== it : " + it);
            // generate prosumers' prameter 
            int c = 0;
            while(c < N){
                //System.out.println("set parameters");
                // counter for counting the # of sellers and buyers
                s_count = 0;
                b_count = 0;
                sb_count = 0;
                // set prosumers' parameters 
                // initialize generation, consumption, capacity, demand
                ge_val = 0;
                ge = 0;
                co_val = 0;
                co = 0;
                ca = 0;
                de =0;
                total_cap_bd = BigDecimal.ZERO;
                total_dem_bd = BigDecimal.ZERO;
                // Initialize parameters
                for(int h = 0; h < N; h++){
                    pro[h].gen = 0.0;
                    pro[h].con = 0.0;
                    pro[h].cap = 0.0;
                    pro[h].dem = 0.0;
                    pro[h].val = 0.0;
                    pro[h].off = 0.0;
                    pro[h].cha = 0.0;
                    pro[h].per = 0.0;
                    pro[h].ave_per = 0.0;
                    pro[h].max_uti = 0.0;
                    pro[h].uti = 0.0;
                    pro[h].sb = 0;
                }
                //System.out.println("confirm ");
                //for(int g = 0; g < N; g++){
                //    System.out.println("pro[" + g + "] ge: " + pro[g].gen + ", co: " + pro[g].con + ", ca: " + pro[g].cap + ", de: " + pro[g].dem);
                //    System.out.println("pro[" + g + "] max_uti: " + pro[g].max_uti + ", total_max_benefit: " + pro[g].total_max_benefit);
                //}
                for(int b = 0; b < N; b++){
                    // generation
                    // ge = 0.0;
                    //Random rand_ge = new Random();
                    //int rand_geValue = rand_ge.nextInt(10);
                    //ge_val = rand_geValue;
                    ge_val = Math.random() * 503 + 302;
                    BigDecimal ge_bd = new BigDecimal(ge_val);
                    BigDecimal ge_bd2 = ge_bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                    ge = ge_bd2.doubleValue();
                    
                    // consumption
                    // co = 0.0;
                    //Random rand_co = new Random();
                    //int rand_coValue = rand_co.nextInt(10);
                    //co_val = rand_coValue;
                    co_val = Math.random() * 550 +349;
                    BigDecimal co_bd = new BigDecimal(co_val);
                    BigDecimal co_bd2 = co_bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                    co = co_bd2.doubleValue();
                    // nothing in the market
                    if(ge == co){
                        //System.out.println("  ");
                        //System.out.println(">>> set nothing");
                        // capacity
                        ca = 0.0;
                        // demand
                        de = 0.0;
                        // seller or buyer
                        sb = 9;
                        sb_count += 1;
                    }
                    // role as a seller
                    else if(ge > co){
                        //System.out.println("  ");
                        //System.out.println(">>> set seller paramer");
                        // capacity
                        BigDecimal ca_bd = ge_bd2.subtract(co_bd2).setScale(2, BigDecimal.ROUND_HALF_UP);
                        //System.out.println("ca_bd : " + ca_bd);
                        // total of capacity
                        total_cap_bd = total_cap_bd.add(ca_bd);
                        //System.out.println("total_cap_bd : " + total_cap_bd);
                        ca = ca_bd.doubleValue();
                        //System.out.println("ca : " + ca);
                        // demand
                        de = 0.0;
                        // calculate maximum benefit 
                        double coef = pro_off - pub_pur;
                        //System.out.println("coef : " + coef);
                        //System.out.println("pro_off : " + pro_off);
                        //System.out.println("pub_pur : " + pub_pur);
                        
                        BigDecimal bd_coef = new BigDecimal(coef);
                        //System.out.println("bd_coef : " + bd_coef);
                        
                        BigDecimal multiply = bd_coef.multiply(ca_bd);
                        //System.out.println("multiply : " + multiply);
                        
                        pro[b].max_uti = multiply.doubleValue();
                        //System.out.println("pro[" + b + "].max_uti : " + pro[b].max_uti);
                        
                        pro[b].total_max_benefit += pro[b].max_uti;
                        //System.out.println("pro[" + b + "].total_max_benefit : " + pro[b].total_max_benefit);
                        
                        // seller or buyer
                        sb = 0;
                        // count up to sellers
                        s_count += 1;
                    }
                    // role as a buyer
                    else{ // (ge < co)
                        //System.out.println("  ");
                        //System.out.println(">>> set buyer paramer");
                        // demand
                        BigDecimal de_bd = co_bd2.subtract(ge_bd2).setScale(2, BigDecimal.ROUND_HALF_UP);
                        //System.out.println("de_bd : " + de_bd);
                        // total of demand
                        total_dem_bd = total_dem_bd.add(de_bd);
                        //System.out.println("total_dem_bd : " + total_dem_bd);
                        de = de_bd.doubleValue();
                        //System.out.println("de : " + de);
                        // capacity
                        ca = 0.0;
                        // calculate maximum benefit
                        double coef = pub_off - pro_off;
                        //System.out.println("coef : " + coef);
                        //System.out.println("pro_off : " + pro_off);
                        //System.out.println("pub_pur : " + pub_pur);
                        
                        BigDecimal bd_coef = new BigDecimal(coef);
                        //System.out.println("bd_coef : " + bd_coef);
                        
                        BigDecimal multiply = bd_coef.multiply(de_bd);
                        //System.out.println("multiply : " + multiply);
                        
                        pro[b].max_uti = multiply.doubleValue();
                        //System.out.println("pro[" + b + "].max_uti : " + pro[b].max_uti);
                        
                        pro[b].total_max_benefit += pro[b].max_uti;
                        //System.out.println("pro[" + b + "].total_max_benefit : " + pro[b].total_max_benefit);
                        
                        // seller or buyer
                        sb = 1;
                        // count up to buyers
                        b_count += 1;
                    }
                        // set prosumers' parameters
                        pro[b].gen = ge;
                        pro[b].con = co;
                        pro[b].cap = ca;
                        pro[b].dem = de;
                        pro[b].off = pro_off;
                        pro[b].sb = sb;
                        //System.out.println("pro[" + b + "] ge: " + pro[b].gen + ", co: " + pro[b].con + ", ca: " + pro[b].cap + ", de: " + pro[b].dem);
                }
                
                if(s_count == (N-sb_count) || b_count == (N-sb_count)){
                    //System.out.println("sb_count : " + sb_count);
                    // initialize counter
                    s_count = 0;
                    b_count = 0;
                    for(int q = 0; q < N; q++){
                        total_max_benefit = pro[q].total_max_benefit - pro[q].max_uti;
                        pro[q].total_max_benefit = total_max_benefit;
                    }
                    continue;
                }
                else{
                    //type conversion (BigDecimal -> Double)
                    total_cap = total_cap_bd.doubleValue();
                    total_dem = total_dem_bd.doubleValue();
                    c = N;
                }
            }
            //System.out.println(">>>>>>>>>>>>>>>>>>>>> s_count : " + s_count);
            //System.out.println(">>>>>>>>>>>>>>>>>>>>> b_count : " + b_count);
            pu[0].cap = total_dem;
            pu[1].dem = total_cap;
            // creatae edges for initial Graph
            MyEdge[] edges = new MyEdge[(s_count + 1) * (b_count + 1 ) - 1]; // 初期グラフのedgeの本数
            // set edges' parameters
            for(int r = 0; r < edges.length; r++){ // edgeの生成
                // label, initial node, terminal node, trading volume
                edges[r] = new MyEdge(100, null, null, 0);
            }
            int l = 0; // edgeのラベル・初期化
            for(int j = 0; j < N; j++){ // 始点ノードを繰り返す
                if(pro[j].sb == 0){ // sellerのプロシューマがある時
                    for(int k = 0; k < N; k++){ // 終点を繰り返す
                        if(pro[k].sb == 1){ // buyerのプロシューマがある時
                            edges[l].label = l; // ラベル
                            edges[l].ini = pro[j]; // 始点
                            edges[l].ter = pro[k]; // 終点
                            l++;
                        }
                    }
                    // sellerの役割を持つ電力+-会社とは必ずつながっている
                    // 電力会社(買い手)と取引
                    edges[l].label = l;
                    edges[l].ini = pro[j];
                    edges[l].ter = pu[1];
                    l++; 
                }
                else if(pro[j].sb == 1){
                    // 電力会社(売り手)と取引
                    edges[l].label = l;
                    edges[l].ini = pu[0];
                    edges[l].ter = pro[j];
                    l++;
                }
            }
            // ---------------------- Solver --------------------------------------
            array = Solver_maximize.milp(pu, pro, edges, total_sw, total_disp, total_std, total_per, total_disp_per, total_std_per, it, N);
            
            total_sw = array[0];
            total_disp =array[1];
            total_std = array[2];
            total_per = array[3];
            total_disp_per = array[4];
            total_std_per = array[5];
            
        } // last iteration
        //DayCSV.dayCSV(pu, pro, N, total_sw, total_std, total_disp, total_per, total_std_per, total_disp_per, ite);
        long endTime = System.currentTimeMillis();
        System.out.println(" ");
        System.out.println("maxmization");
        System.out.println("開始時刻：" + startTime + " ms");
        System.out.println("終了時刻：" + endTime + " ms");
        System.out.println("処理時間：" + (endTime - startTime) + " ms");
    }
}
/*
Reference

Initialize BigDecimal
http://hensa40.cutegirl.jp/archives/764

*/