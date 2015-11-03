package cs425.mp3;

import java.io.Serializable;

/**
 * Created by parijatmazumdar on 02/11/15.
 */
public class XYZ implements Serializable{
    private final String x;
    private final String y;
    private final String z;

    public XYZ(String x,String y, String z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void print() {
        System.out.println(x+":"+y+":"+z);
    }
}
