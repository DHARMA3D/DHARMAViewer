/**
 * 
 */
package org.tqdev.visarray.model;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

/**
 * @author James Sweet
 *
 */
public class Model{
    public String Title;
    public String	Path;
	public float Radius;
	public Vector3f Center;
	public Vector4f View;
    
	public List<Mesh> Meshes = new ArrayList<Mesh>();
	public List<Cloud> Clouds = new ArrayList<Cloud>();
}