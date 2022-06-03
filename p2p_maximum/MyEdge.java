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
public class MyEdge {
    int label;
    double x;
    MyNode ini, ter;
    
    public MyEdge(int label, MyNode ini, MyNode ter, double x){ // label, flow, initial node, terminal node
        this.label = label;
        this.x = x;
        this.ini = ini;
        this.ter = ter;
    }
    
    /*
    public String toSting(){
        return label;
    }
    */
}
