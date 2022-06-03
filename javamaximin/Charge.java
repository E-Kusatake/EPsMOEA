/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_maximin;

import static EDU.oswego.cs.dl.util.concurrent.DefaultChannelCapacity.set;
// import static com.sun.scenario.Settings.set;
import edu.uci.ics.jung.graph.Graph;
import static java.lang.reflect.Array.set;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author imahori
 */
public class Charge {
    static MyNode ini_node = null, ter_node = null;
    static double price, value, total; // 提供価格, valuation, 収入の総和, 支払の総和
    static double charge;

    static void bsgraph(Graph<MyNode,MyEdge> graph, MyNode[] pu, MyNode[] pro, MyEdge[] edges, int n, double[] var){ // graph, public utility, prosumer, edges, the number of prosumers, var
       // System.out.println("charge計算中");
        // 受け渡しの時に s or b の場合分けをする
        // System.out.println("pro長さ :" + pro.length);

        for(MyEdge select : edges){ // edgeを回す
            ini_node = select.ini; // 始点nodeについて
            ter_node = select.ter; // 終点nodeについて
            if(ini_node != pu[0]){ // 始点nodeが電力会社じゃない場合
                ini_node.val = setValue(pu[1].cha, var[Integer.valueOf(select.label)]); // 始点nodeのvaluationを算出
                if(ter_node == pu[1]){ // 終点nodeが電力会社だったら
                    //ini_node.inc[Integer.valueOf(select.ter.label)] = setSell(pu[1].cha, var[Integer.valueOf(select.label)]);
                }
                else{
                    //ini_node.inc[Integer.valueOf(select.ter.label)] = setSell(ini_node.off, var[Integer.valueOf(select.label)]);
                } // 始点nodeの提供価格を算出
                //System.out.println("始点の収入" + ini_node.inc[Integer.valueOf(select.ter.label)]);
            }
            if(ter_node != pu[1]){
                ter_node.val = setValue(pu[0].off, var[Integer.valueOf(select.label)]); // 終点nodeのvaluationを算出
                //ter_node.pur[Integer.valueOf(select.ini.label)] = ini_node.inc[Integer.valueOf(select.ter.label)]; // 終点nodeの購入価格を算出
            }
        }
        for(MyNode choose : pro){
            total = 0;
            for(int i = 0; i < n + 2; i++){
                if(choose.sb == 0){ // chooseがselleノードの場合
                    //total += choose.inc[i];
                }
                else if(choose.sb == 1){
                    //total += choose.pur[i];
                }
            }
            if(choose.sb == 0){
                choose.uti = total  - choose.val;
                //choose.inc[n+2] = total;
                //System.out.println("income :" + choose.inc[n+2]);
                System.out.println("choose :" + choose.label);
            }
            else if(choose.sb == 1){
                choose.uti = choose.val - total;
                //choose.pur[n+2] = total;
            }
        }
        for(int w = 0; w < n; w++){
            System.out.println("pro効用" + w + pro[w].uti);
        }
    }

   
    private static double setValue(double pu_set, double amount){ // valuation(電力会社の販売価格単価 or 購入価格単価), 電力取引量
        
        BigDecimal b_val, b_set, b_amount; // BigDecimalのvaluation, 販売価格単価 or 購入価格単価, capacity or demand

        b_amount = BigDecimal.valueOf(amount); // このnodeのcapacity or demand
        b_set = BigDecimal.valueOf(pu_set); // 電力会社の電力料金単価　提供 or 支払い

        b_val = b_amount.multiply(b_set); // BigDecimalでprice算出

        double value = b_val.doubleValue(); // valuationをDoubleに直して代入
        return value; // valuationを返す
    }
    
    private static double setSell(double off, double amount){ // 販売価格(or 買取価格)を算出する関数
        
        BigDecimal s_price, s_off, s_tori; // BigDecimal 提供価格・電力料金単価・電力取引量

        s_tori = BigDecimal.valueOf(amount); // 取引量 capacity
        s_off = BigDecimal.valueOf(off); // 隣接nodeの提供価格 Charge
        

        s_price = s_tori.multiply(s_off); // BigDecimalでprice算出 
        
        price = s_price.doubleValue(); // 購入価格をDoubleに直して代入
                        
        System.out.println("price" + s_price);
        System.out.println("offer" + s_off);
        System.out.println("amount" + s_tori);
        return price; // 購入価格を返す
        
    }
}
