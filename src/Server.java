
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JTextArea;

public class Server extends JFrame {
    private ServerSocket server = null;
    private int PORT = 40541;
    private final int MAX_CONNECTED_USERS = 2;
    private JTextArea console;
    private HashMap<Integer, ObjectOutputStream> oosList;
    private Socket client;

    public Server() {
        super("Servidor Asteroids");
        initComponents();
        startServer();
    }

    private void initComponents() {
        setPreferredSize(new Dimension(250, 400));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        console = new JTextArea();
        console.setLineWrap(true);
        console.setEditable(false);
        add(console, BorderLayout.CENTER);

        pack();
        setVisible(true);
    }

    private void startServer() {
        try {
            oosList = new HashMap<>();
            console.append("Iniciando servidor.\n");
            console.append("Aguardando conexões.\n");
            server = new ServerSocket(PORT);
            for (int i = 1; i <= MAX_CONNECTED_USERS; i++) {
                client = server.accept();
                String name = client.getInetAddress().getHostName();
                console.append(name + " acabou de se conectar.\n");
                oosList.put(i, new ObjectOutputStream(client.getOutputStream()));
                //Informa o id para seu cliente, para saber se ele é o player 1 ou 2
                oosList.get(i).writeObject("id:" + i);
                new ServerThread(i).start();
            }
            new GameTasks();
            console.append("Máximo de jogadores conectados atingido\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ServerThread extends Thread {
        private int id;

        public ServerThread(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            Object data = null;
            try {
                ObjectInputStream input = new ObjectInputStream(client.getInputStream());
                while ((data = input.readObject()) != null) {
                    for (int i : oosList.keySet()) {
                        if (i != id && oosList.containsKey(i)) {
                            oosList.get(i).writeObject(data);
                            oosList.get(i).flush();
                        }
                    }
                }
                input.reset();
                input.close();
            } catch (Exception ex) {
                console.append("The client: " + id + " has disconected.\n");
                disconect();
            }
        }

        private void disconect() {
            try {
                oosList.get(id).close();
            } catch (IOException ex) {
                console.append(ex.getMessage() + "\n");
            }
            oosList.remove(id);
        }
    }

    //
    private class GameTasks {

        private Random r = new Random();

        //Delay padrao em milisegundos para o inicio de cada tarefa.
        final private int defaultDelay = 2000;
        //Tempo entre a criação de asteroides
        final private int asteroidInterval = 600;

        private int asteroidTimeCounter = 0;

        private int asteroidSpeedY = 5;

        public GameTasks() {
            Timer asteroid = new Timer("AsteroidTask");
            asteroid.scheduleAtFixedRate(createAsteroid(), asteroidInterval, defaultDelay);
        }

        //Tarefa para criar asteroids em posições x aleatórias
        private TimerTask createAsteroid() {
            return new TimerTask() {
                @Override
                public void run() {
                    asteroidTimeCounter += asteroidInterval;

                    if (asteroidTimeCounter >= 3000) {
                        asteroidSpeedY++;
                        asteroidTimeCounter = 0;
                    }

                    String command = "asteroid:%d,%d,%d,%d,%d";
                    int x = r.nextInt(760) + 20; //Posição x do asteroid
                    int radius = r.nextInt(3);
                    for (int i : oosList.keySet()) {
                        try {
                            oosList.get(i).writeObject(String.format(command, x, 10, 0, asteroidSpeedY, radius));
                            oosList.get(i).flush();
                        } catch (IOException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            };
        }
    }

    public static void main(String args[]) {
        new Server();
    }
}
