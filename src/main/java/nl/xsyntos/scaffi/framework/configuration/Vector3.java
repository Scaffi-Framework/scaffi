package nl.xsyntos.scaffi.framework.configuration;


import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author KOWI2003
 */
@Data
@AllArgsConstructor
public class Vector3 {
    
    public static Vector3 ZERO = new Vector3();

    private double x;
    private double y;
    private double z;

    public Vector3() {
        this(0, 0, 0);
    }

    public Vector3(Location location) {
        this(location.getX(), location.getY(), location.getZ());
    }

    /**
     * @param vector the value to add
     * @return a copy of this vector with the parameter values added to it
     */
    public Vector3 add(Vector3 vector) {
        return add(vector.x, vector.y, vector.z);
    }

    /**
     * @param x the x value to add
     * @param y the y value to add
     * @param z the z value to add
     * @return a copy of this vector with the parameter values added to it
     */
    public Vector3 add(double x, double y, double z) {
        return new Vector3(this.x + x, this.y + y, this.z + z);
    }

    /**
     * @param vector the value to subtract
     * @return a copy of this vector with the parameter values subtracted from it
     */
    public Vector3 subtract(Vector3 vector) {
        return subtract(vector.x, vector.y, vector.z);
    }
    
    /**
     * @param x the x value to subtract
     * @param y the y value to subtract
     * @param z the z value to subtract
     * @return a copy of this vector with the parameter values subtracted from it
     */
    public Vector3 subtract(double x, double y, double z) {
        return new Vector3(this.x - x, this.y - y, this.z - z);
    }

    /**
     * @param scalar the value to multiply the vector with
     * @return a copy of this vector with the parameter values subtracted from it
     */
    public Vector3 multiply(double scalar) {
        return new Vector3(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    /**
     * normalizes the vector, which means the vector will get a length of one
     */
    public void normalize() {
        double total = this.x + this.y + this.z;
        this.x /= total;
        this.y /= total;
        this.z /= total;
    }

    /**
     * gets an copy of the vector that is normalized, which means that the length of the returned vector is exactly one
     * @return the normalized vector
     */
    public Vector3 normalized() {
        Vector3 copy = copy();
        copy.normalize();
        return copy;
    }

    /**
     * gets the squared length of the vector
     * @return the squared length of the vector
     */
    public double lengthSqr() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }
    
    /**
     * gets the length of the vector
     * @return the length of the vector
     */
    public double length() {
        return Math.sqrt(lengthSqr());
    }

    /**
     * gets the distance from this vector to the vector given as parameter
     * @param vector the vector to get the distance to from
     * @return the distance between the two vectors
     */
    public double distanceTo(Vector3 vector) {
        return subtract(vector).length();
    }

    /**
     * gets the squared distance from this vector to the vector given as parameter (faster than normal distance to)
     * @param vector the vector to get the distance to from
     * @return the distance between the two vectors
     */
    public double sqrDistanceTo(Vector3 vector) {
        return subtract(vector).lengthSqr();
    }

    /**
     * gets the distance from this vector to the coordinates given as parameter
     * @param x the x position to get the distance to
     * @param y the y position to get the distance to
     * @param z the z position to get the distance to
     * @return the distance between the this vector and the coorinates
     */
    public double distanceTo(double x, double y, double z) {
        return distanceTo(new Vector3(x, y, z));
    }

    public double dot(Vector3 vector) {
        return this.x * vector.x + this.y * vector.y + this.z * vector.z;
    }

    /**
     * returns an inverted copy of this vector
     * @return the inverted copy
     */
    public Vector3 negated() {
        return new Vector3(-this.x, -this.y, -this.z);
    }

    /**
     * creates an copy of the vector
     * @return the copy
     */
    public Vector3 copy() {
        return new Vector3(this.x, this.y, this.z);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Vector3) {
            Vector3 vector = (Vector3)obj;
            return this.x == vector.x && this.y == vector.y && this.z == vector.z; 
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.y + ", " + this.z + "]";
    }

    /**
     * gets an location based on the vector's position, where the world is the overworld
     * @return the location from this vector
     */
    public Location toLocation() {
        return toLocation(Bukkit.getWorlds().get(0));
    }

    /**
     * gets an location based on the vector's position, where the world is the world specified as the parameter
     * @param world the world to make the location of
     * @return the location from this vector with the world from the parameter
     */
    public Location toLocation(World world) {
        return new Location(world, x, y, z);
    }
}