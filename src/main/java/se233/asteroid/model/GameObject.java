package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;

public abstract class GameObject {
    protected Point2D position;
    protected Point2D velocity;
    protected double rotation;
    protected Shape shape;
    protected boolean isAlive;

    public GameObject(Point2D position) {
        this.position = position;
        this.velocity = new Point2D(0, 0);
        this.rotation = 0;
        this.isAlive = true;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
        updateShapePosition();
    }

    public Point2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Point2D velocity) {
        this.velocity = velocity;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
        updateShapeRotation();
    }

    public Shape getShape() {
        return shape;
    }

    public boolean isAlive() {
        return isAlive;
    }

    // Abstract methods that must be implemented by subclasses
    public abstract void update();
    public abstract void hit();

    public boolean collidesWith(GameObject other) {
        if (this.shape == null || other.getShape() == null) {
            return false;
        }
        return Shape.intersect(this.shape, other.getShape()).getBoundsInLocal().getWidth() != -1;
    }

    protected void updateShapePosition() {
        if (shape != null) {
            shape.setTranslateX(position.getX());
            shape.setTranslateY(position.getY());
        }
    }

    protected void updateShapeRotation() {
        if (shape != null) {
            shape.setRotate(rotation);
        }
    }
}