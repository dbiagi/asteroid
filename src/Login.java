import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class Login extends JDialog implements ActionListener{
    private JTextField txt_nome;
    private JTextField txt_serv;
    private JButton bt_ok;
    private JButton bt_cancel;
    private String ip;
    private String name;
    
    public Login(JFrame frame){
        super(frame, true);
        JPanel messagePanel = new JPanel();
        getContentPane().add(messagePanel);
        txt_nome = new JTextField(null, 10);
        txt_serv = new JTextField("localhost", 10);
        bt_ok = new JButton("Iniciar");
        bt_cancel = new JButton("Sair");
        bt_ok.addActionListener(this);
        bt_cancel.addActionListener(this);
        messagePanel.setLayout(new GridLayout(0,2));
        messagePanel.add(new JLabel("Jogador: "));
        messagePanel.add(txt_nome);
        messagePanel.add(new JLabel("IP Servidor: "));
        messagePanel.add(txt_serv);
        messagePanel.add(bt_ok);
        messagePanel.add(bt_cancel);
        pack();
        setLocationRelativeTo(frame);
        setVisible(true);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == bt_ok){
            name = txt_nome.getText();
            ip = txt_serv.getText();
            dispose();
        }
        
        if(e.getSource() == bt_cancel){
            System.exit(1);
        }
    }
    
    public String getPlayerName(){
        return name;
    }
    
    public String getServerIpAdress(){
        return ip;
    }
}
