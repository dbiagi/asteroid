import java.awt.BorderLayout;
import javax.swing.JFrame;

public class Main {
    public static void main(String args[]){
        JFrame f = new JFrame();
        f.setSize(800, 600);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setResizable(false);
        f.setTitle("Asteroids");
        Login l = new Login(f);
        f.setLayout(new BorderLayout());
        f.add(new Board(l.getServerIpAdress(), l.getPlayerName()), BorderLayout.CENTER);
        //f.add(new Board("localhost", ""), BorderLayout.CENTER);
        f.pack();
        f.setVisible(true);        
    }
}