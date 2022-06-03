/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package p2p_maximum;

/**
 *
 * @author imahori
 */
public class MyNode {
    static int nodeNo = 0;
    int label;
    double gen, con, dem, cap , val, off, cha, total_benefit, total_max_benefit,
            per, total_per, ave_per, max_uti, uti;
    int sb; // sb = 0 (seller), sb = 1 (buyer)
    
    /*
    public MyNode(int label){
        this.label = label;
        nodeNo++;
    }
    */
    
    //プロシューマのノード
    MyNode(int label, double gen, double con, double cap, double dem, double val, double off, double cha, 
            double total_benefit, double total_max_benefit, double per, double total_per, double ave_per,
            double max_uti, double uti, int sb){
        // label, 発電量 (s), 消費量 (b), 供給量の制限 (s), 需要量 (b), 留保価格 (s & b), 販売価格 (s), 購入価格 (b),
        // total of benefit (s & b), utility (s & b), seller or buyer? (s & b)
        this.label = label;
        nodeNo++;
        this.gen = gen;
        this.con = con;
        this.cap = cap;
        this.dem = dem;
        this.val = val;
        this.off = off;
        this.cha = cha;
        this.total_benefit = total_benefit;
        this.total_max_benefit = total_max_benefit;
        this.per = per;
        this.total_per = total_per;
        this.max_uti = max_uti;
        this.uti = uti;
        this.sb = sb;
    }
    

    /*@Override
    public String toString(){
        return label;
    }*/
}

