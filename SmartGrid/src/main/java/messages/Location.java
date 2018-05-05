package messages;

import akka.actor.ActorRef;

public class Location {
    private ActorRef sender;
    private double x, y;

    public Location(ActorRef sender, double x, double y){
        this.sender = sender;
        this.x = x;
        this.y = y;
    }

    public ActorRef getSender() { return sender; }

    public void setSender(ActorRef sender) { this.sender = sender; }

    public double getX() { return x; }

    public void setX(double x) { this.x = x; }

    public double getY() { return y; }

    public void setY(double y) { this.y = y; }
}
