/**
 * 
 */
package org.tqdev.visarray.model;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.tqdev.visarray.opengl.VertexBufferObject;

import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.file.TFileInputStream;
import de.schlichtherle.truezip.io.Streams;

/**
 * @author Omegaice
 *
 */
public class Cloud{
	public float PointScale;
    public FloatBuffer Transformation = BufferUtils.createFloatBuffer(16);
    
    public int Verticies;
    public String Vertex;
    
    private ByteBuffer DataBuffer = null;
    private VertexBufferObject DataVBO = null;
    
    public void SetVertex( String Path ){
        Vertex = Path;
        
        try{
        	ByteArrayOutputStream data = new ByteArrayOutputStream();
        	Streams.copy( new TFileInputStream(new TFile(Vertex)), data);
            DataBuffer = BufferUtils.createByteBuffer(data.size());
            DataBuffer.put(data.toByteArray());
            DataBuffer.rewind();
        }catch( Exception e ){

        }
    }
    
    public void Draw(){
        if( DataVBO == null && DataBuffer != null ){
            DataVBO = new VertexBufferObject();
            DataVBO.Create();
            DataVBO.Bind(GL_ARRAY_BUFFER);
            {
                DataVBO.Upload(GL_ARRAY_BUFFER, DataBuffer);
            }
            DataVBO.Unbind(GL_ARRAY_BUFFER);   
        }
        
        DataVBO.Bind(GL_ARRAY_BUFFER);
        
        glPointSize(PointScale);
        
        glPushMatrix();
        {
            glMultMatrix( Transformation );

            glEnableClientState(GL_VERTEX_ARRAY);
            glEnableClientState(GL_COLOR_ARRAY);

            glVertexPointer(3, GL_FLOAT, 28, 0);
            glColorPointer(3, GL_FLOAT, 28, 12);

            glDrawArrays(GL_POINTS, 0, Verticies);

            glDisableClientState(GL_VERTEX_ARRAY);
            glDisableClientState(GL_COLOR_ARRAY);
        }
        glPopMatrix();
        
        DataVBO.Unbind(GL_ARRAY_BUFFER);
    }
}