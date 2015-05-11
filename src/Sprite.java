
import java.awt.Dimension;
import java.awt.Point;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Diego
 */
class Sprite {
    private Point position;
    private Dimension size;
    private Point speed;
    
    public void setPosition(Point position){
        this.position = position;
    }
    
    public void setSize(Dimension size){
        this.size = size;
    }
    
    public void setSpeed(Point speed){
        this.speed = speed;
    }
    
    public Point getPosition(){
        return position;
    }
    
    public Dimension getSize(){
        return size;
    }
    
    public Point getSpeed(){
        return speed;
    }
}
