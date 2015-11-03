package cs425.mp3;

import cs425.mp3.FileServer.Message;
import cs425.mp3.FileServer.MessageType;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

/**
 * Created by parijatmazumdar on 02/11/15.
 */
public class Main {
    public static void main(String [] args) {
        XYZ t=new XYZ("a","b","cd");
        String ser=t.toString();
        System.out.println(MessageType.DEL.toString());
        // deserialize the object
        try {
            byte b[] = ser.getBytes();
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            XYZ obj = (XYZ) si.readObject();
            obj.print();
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
