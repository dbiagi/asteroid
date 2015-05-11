import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import javax.imageio.ImageIO;

import javax.swing.*;

public class Board extends JPanel implements Runnable {

    private String playerName;
    private Thread t;
    private LinkedList<Asteroid> asteroids;
    private LinkedList<Bullet> bullets;
    private Player localPlayer;
    private Player remotePlayer;
    private int score = 0;
    //variaveis das imagens dos objetos
    private static BufferedImage backgroundImage;
    private static BufferedImage redPlayerImage;
    private static BufferedImage bluePlayerImage;
    private static BufferedImage asteroidImage;
    private static BufferedImage bulletImage;
    private boolean isPaused = true;
    private int asteroidTimer = 0;
    private float FPS;
    //Posiçao x inicial do asteroid
    private int xiAsteroid = 20;
    //Posição x do ultimo asteroid criado
    private int xAsteroid;
    //Posição y inicial do asteroid
    private int yiAsteroid = 10;
    //Variaveis para uso na coneção
    private String serverIpAdress;
    private int PORT = 40541;
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public Board(String serverIpAdress, String playerName) {
        this.serverIpAdress = serverIpAdress;
        this.playerName = playerName;
        asteroids = new LinkedList<>();
        bullets = new LinkedList<>();
        setFocusable(true);
        setVisible(true);
        setPreferredSize(new Dimension(800, 600));
        addKeyboardListener();
        loadResources();
        setUpPlayers();
        FPS = 60;
        connectToServer();
    }

    //Carrega imagens
    private void loadResources() {
        backgroundImage = loadImage("Background.jpg");
        redPlayerImage = loadImage("spaceship_vermelho.png");
        bluePlayerImage = loadImage("spaceship_azul.png");
        asteroidImage = loadImage("asteroid.png");
        bulletImage = loadImage("projetil.png");
    }

    //Inicia variaveis dos jogadores
    private void setUpPlayers() {
        localPlayer = new Player(new Point(),
                new Dimension(bluePlayerImage.getWidth(), bluePlayerImage.getHeight()));
        remotePlayer = new Player(new Point(),
                new Dimension(redPlayerImage.getWidth(), redPlayerImage.getHeight()));
    }

    //Trata os eventos de teclado
    private void addKeyboardListener() {
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F2) {
                    startGame();
                }
                
                if (!isPaused && localPlayer.isAlive()) {
                    //Move o jogador para a esquerda, e envia mensagem para o jogador remoto informando sua nova posição
                    if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        localPlayer.getPosition().x -= 10;
                        sendMessage("xy:" + localPlayer.getPosition().x + "," + 
                                localPlayer.getPosition().y);
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) { //Move o jogador para a direita, e envia mensagem para o jogador remoto informando sua nova posição
                        localPlayer.getPosition().x += 10;
                        sendMessage("xy:" + localPlayer.getPosition().x + "," + 
                                localPlayer.getPosition().y);
                    } else if (e.getKeyCode() == KeyEvent.VK_SPACE) { //Cria um projetil e envia mensagem para o jogador removo informando sua posição
                        Point bulletPoint = new Point(localPlayer.getPosition().x + 
                                localPlayer.getSize().width / 2,
                                localPlayer.getPosition().y);
                        fire(bulletPoint);
                        sendMessage("fire:" + bulletPoint.x + "," + bulletPoint.y);
                    }
                }
            }
        });
    }

    private void startGame() {
        if (localPlayer.getId() == 1) {
            localPlayer.setPosition(new Point(100,
                    (int)this.getPreferredSize().getHeight() - localPlayer.getSize().height - 20));
            remotePlayer.setPosition(new Point((int)this.getPreferredSize().getWidth() - 100,
                    (int)this.getPreferredSize().getHeight() - remotePlayer.getSize().height - 20));
        } else {
            localPlayer.setPosition(new Point((int)this.getPreferredSize().getWidth() - 100,
                   (int)this.getPreferredSize().getHeight() - localPlayer.getSize().height - 20));
            remotePlayer.setPosition(new Point(100,
                    (int)this.getPreferredSize().getHeight() - remotePlayer.getSize().height - 20));
        }

        isPaused = false;

        if (t == null) {
            t = new Thread(this);
        }
        if (!t.isAlive()) {
            t.start();
        }
    }

    private void createAsteroid(Point pos) {
        asteroids.addFirst(new Asteroid(pos, new Dimension(asteroidImage.getWidth(), asteroidImage.getHeight())));
    }

    private void fire(Point pos) {
        bullets.add(new Bullet(pos, new Dimension(bulletImage.getWidth(), bulletImage.getHeight())));
    }

    //Conecta no servidor do jogo
    private void connectToServer() {
        try {
            socket = new Socket(serverIpAdress, PORT);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            startListeningServer();
            
            //Envia um ok para o outro jogador
            sendMessage("ok:" + localPlayer.getId());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erro ao conectar no servidor: "
                    + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    //Inicia thread para recebimento asíncrono de mensagem
    private void startListeningServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Object data;
                try {
                    while ((data = ois.readObject()) != null) {
                        messageReceived(data.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //Trata mensagens recebidas
    private void messageReceived(String message) {
        System.out.println(message);
        String[] parts = message.split(":");
        switch (parts[0]) {
            //Recebe o seu id no servidor
            case "id":
                localPlayer.setId(Integer.parseInt(parts[1]));
                break;
            //Recebe o ok do outro jogador
            case "ok":
                remotePlayer.setId(Integer.parseInt(parts[1]));
                if(isPaused){
                    startGame();
                    sendMessage("ok:" + localPlayer.getId());                
                }
                break;
            //Recebe a nova coordenada xy do jogador remoto
            case "xy":
                //"xy: 100,200"
                String[] pCoords = parts[1].split(",");
                remotePlayer.setPosition(new Point(Integer.parseInt(pCoords[0]), 
                        Integer.parseInt(pCoords[1])));
                break;
            case "fire":
                String[] aCoords = parts[1].split(",");
                fire(new Point(Integer.parseInt(aCoords[0]), 
                        Integer.parseInt(aCoords[1])));
                break;
        }
    }

    //Envia mensagem para o servidor
    private void sendMessage(String message) {
        try {
            oos.writeObject(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.drawImage(backgroundImage, null, 0, 0);

        if (isPaused) {
            g2d.setColor(Color.blue);
            g2d.setFont(new Font("Segoe UI Light", Font.BOLD, 36));
            g2d.drawString("Esperando o 2º jogador.", this.getSize().width / 2 - 200, this.getSize().height / 2);
        } else {
            
            for (Asteroid a : asteroids) {
                g2d.drawImage(asteroidImage, a.getPosition().x, a.getPosition().y, null);
            }

            for (Bullet b : bullets) {
                g2d.drawImage(bulletImage, b.getPosition().x, b.getPosition().y, null);
            }

            if (localPlayer.isAlive()) {
                g2d.drawImage(bluePlayerImage, localPlayer.getPosition().x,
                        localPlayer.getPosition().y, null);
            }

            if (remotePlayer.isAlive()) {
                g2d.drawImage(redPlayerImage, remotePlayer.getPosition().x,
                        remotePlayer.getPosition().y, null);
            }
            
            g2d.setColor(new Color(128, 0, 255));
            g2d.setFont(new Font("Segoe UI Light", Font.BOLD, 36));
            g2d.drawString("Score:" + score, this.getWidth() - 200, 50);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep((int) FPS);
                asteroidTimer += FPS;

                //Cria 1 asteroid a cada 1s
                if (asteroidTimer >= 1000) {
                    if (xAsteroid + 100 < this.getWidth() - asteroidImage.getWidth()) {
                        xAsteroid += 100;
                    } else {
                        xAsteroid = xiAsteroid;
                    }
                    createAsteroid(new Point(xAsteroid, yiAsteroid));
                    asteroidTimer = 0;
                }

                LinkedList<Asteroid> auxAsteroid = (LinkedList<Asteroid>) asteroids.clone();
                LinkedList<Bullet> auxBullet = (LinkedList<Bullet>) bullets.clone();

                for (Asteroid a : asteroids) {
                    //Decresce o valor  y dos asteroides
                    a.getPosition().y += a.getSpeed().y;

                    //Verifica se algum asteroid atingiu algum jogador
                    if (localPlayer.isAlive()) {
                        if (collisionDetection(a, localPlayer)) {
                            localPlayer.setAlive(false);
                        }
                    }
                    if (remotePlayer.isAlive()) {
                        if (collisionDetection(a, remotePlayer)) {
                            remotePlayer.setAlive(false);
                        }
                    }

                    //Verifica se algum projetil atingiu algum asteroid
                    for (Bullet b : bullets) {
                        if (collisionDetection(a, b)) {
                            auxAsteroid.remove(a);
                            auxBullet.remove(b);
                            score += 10;
                        }
                    }

                    if (a.getPosition().y > this.getHeight()) {
                        auxAsteroid.remove(a);
                    }
                }
                asteroids = auxAsteroid;


                for (Bullet b : bullets) {
                    b.getPosition().y -= b.getSpeed().y;
                    if (b.getPosition().y < 0) {
                        auxBullet.remove(b);
                    }
                }
                bullets = auxBullet;

                repaint();
            } catch (Exception e) {
            }

        }
    }

    //Carrega imagens na pasta img
    public BufferedImage loadImage(String fileName) {
        try {
            return ImageIO.read(getClass().getResource("/img/" + fileName));
        } catch (IOException e) {
            System.out.println("Content could not be read");
            e.printStackTrace();
            return null;
        }
    }

    //Detecta colisão entre dois sprites
    private boolean collisionDetection(Sprite sprite1, Sprite sprite2) {
        Point pos1 = sprite1.getPosition();
        Point pos2 = sprite2.getPosition();
        int w1 = sprite1.getSize().width;
        int h1 = sprite1.getSize().height;
        int w2 = sprite2.getSize().width;
        int h2 = sprite2.getSize().height;
        if (((pos1.x > pos2.x && pos1.x < pos2.x + w2)
                && (pos1.y > pos2.y && pos1.y < pos2.y + h2))
                || ((pos2.x > pos1.x && pos2.x < pos1.x + w1)
                && (pos2.y > pos1.y && pos2.y < pos1.y + h1))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void update(Graphics g) {
        super.paint(g);
        Image offScreen = null;
        Graphics offGraphics = null;
        Graphics2D g2d = (Graphics2D) g.create();
        Dimension dimension = getSize();
        if (offScreen == null) {
            offScreen = createImage(dimension.width, dimension.height);
            if (offGraphics != null) {
                offGraphics.dispose();
            }
            offGraphics = offScreen.getGraphics();
        }
        offGraphics.setColor(getBackground());
        offGraphics.fillRect(0, 0, dimension.width, dimension.height);
        offGraphics.setColor(Color.black);
        paint(offGraphics);
        g2d.drawImage(offScreen, 0, 0, null);
        g2d.dispose();
    }

    private class Asteroid extends Sprite {
        public Asteroid(Point position, Dimension size) {
            setPosition(position);
            setSize(size);
            setSpeed(new Point(0, 5));
        }
    }

    private class Bullet extends Sprite {
        public Bullet(Point position, Dimension size) {
            setPosition(position);
            setSize(size);
            setSpeed(new Point(0, 10));
        }
    }

    private class Player extends Sprite {

        private int id;
        private boolean isAlive;

        public Player(Point position, Dimension size) {
            setPosition(position);
            setSize(size);
            isAlive = true;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public boolean isAlive() {
            return isAlive;
        }

        public void setAlive(boolean isAlive) {
            this.isAlive = isAlive;
        }
    }
}
